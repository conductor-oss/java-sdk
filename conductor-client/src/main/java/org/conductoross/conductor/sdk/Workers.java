/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.conductoross.conductor.sdk;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskContinuation;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorker;

/**
 * Hosts workflow classes and task classes in this process.
 *
 * <p>{@link #start()} registers, from the annotations, a skeleton definition for each workflow and
 * a task definition for each task, then starts polling: one poller per {@code @WorkerTask} method,
 * and one per workflow for its continuations (the workflow's code type, polled like any task
 * type). A continuation arrives as the task the code paused on, carrying its continuation marker;
 * it is processed by re-running the workflow method against the execution's recorded tasks, so any
 * host of the same code can process any continuation. Continuations of one execution are processed
 * one at a time, and completed through the ordinary task update.
 */
public final class Workers {

    private static final Logger LOGGER = LoggerFactory.getLogger(Workers.class);

    private final Conductor conductor;
    private final TaskClient taskClient;
    private final Map<String, WorkflowSpec> specsByCodeType = new LinkedHashMap<>();
    private final List<Object> taskObjects = new ArrayList<>();
    private final List<Object> definedTaskObjects = new ArrayList<>();
    private String workerId;
    private String buildId;
    private boolean devMode;
    private boolean discovery = true;
    private double timeScale = 20;
    private int threads = 8;
    private java.time.Duration continuationTimeout = java.time.Duration.ofSeconds(30);
    private TaskRunnerConfigurer taskRunner;
    private ExecutorService codeExecutor;
    private final List<Thread> pollers = new ArrayList<>();
    private volatile boolean running;
    private volatile boolean registered;

    /**
     * Waits between attempts while the server cannot be reached: 1, 2, 4... seconds, at most a
     * minute, each shortened by up to half at random so that many hosts do not retry in step.
     */
    static final Backoff OUTAGE = Backoff.every(java.time.Duration.ofSeconds(1)).factor(2).max(java.time.Duration.ofMinutes(1));

    Workers(Conductor conductor) {
        this.conductor = conductor;
        this.taskClient = new TaskClient(conductor.client());
        // Identifies this host in poll data and on the tasks it runs. Set CONDUCTOR_WORKER_ID when
        // several hosts share a machine, so that they can be told apart.
        String id = System.getenv("CONDUCTOR_WORKER_ID");
        if (id == null || id.isBlank()) {
            try {
                id = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                id = "native-worker";
            }
        }
        this.workerId = id;
    }

    public Workers workerId(String workerId) {
        this.workerId = workerId;
        return this;
    }

    /** A task domain this host also polls for continuations; see {@code taskToDomain} on a start. */
    public Workers buildId(String buildId) {
        this.buildId = buildId;
        return this;
    }

    /**
     * Development mode: generous continuation timeouts (for breakpoints) and timers divided by the
     * time scale, when the server has {@code conductor.app.nativeWorkflow.waitTimeScaleEnabled}.
     */
    public Workers devMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    public Workers timeScale(double timeScale) {
        this.timeScale = timeScale;
        return this;
    }

    /**
     * How long the server waits for a continuation it handed to a worker before retrying it
     * elsewhere (the continuation lease); default 30 seconds. It bounds how long an
     * execution stalls when a poll response is lost to a network failure or the worker dies while
     * processing. It must exceed the longest replay of an execution. Dev mode uses an hour, so a
     * breakpoint does not trigger a retry.
     */
    public Workers continuationTimeout(java.time.Duration timeout) {
        this.continuationTimeout = timeout;
        return this;
    }

    /**
     * Whether the registered definition carries the graph read from the compiled workflow code.
     * On by default. Reading runs nothing; the untaken cases of a {@code switchOn} are always read.
     */
    public Workers discovery(boolean discovery) {
        this.discovery = discovery;
        return this;
    }

    public Workers threads(int threads) {
        this.threads = threads;
        return this;
    }

    public Workers workflows(Class<?>... types) {
        for (Class<?> t : types) {
            for (WorkflowSpec spec : WorkflowSpec.of(t)) {
                specsByCodeType.put(spec.codeTaskName(), spec);
            }
        }
        return this;
    }

    /** A workflow class whose instances need constructor arguments. */
    @SuppressWarnings("unchecked")
    public <T> Workers workflow(Class<T> type, Supplier<T> factory) {
        for (WorkflowSpec spec : WorkflowSpec.of(type)) {
            spec.factory((Supplier<Object>) factory);
            specsByCodeType.put(spec.codeTaskName(), spec);
        }
        return this;
    }

    /** Objects with {@code @WorkerTask} methods. */
    public Workers tasks(Object... objects) {
        taskObjects.addAll(List.of(objects));
        return this;
    }

    /**
     * Objects with {@code @WorkerTask} methods that another executor polls (Spring's worker
     * auto-configuration, for one): their task definitions are registered on start, and no poller
     * is started for them here.
     */
    public Workers taskDefinitions(Object... objects) {
        definedTaskObjects.addAll(List.of(objects));
        return this;
    }

    public String workerId() {
        return workerId;
    }

    /**
     * Starts hosting and returns at once. The definitions are registered in the background,
     * retried with backoff for as long as the server cannot be reached, and polling starts now:
     * a server that is down, even for hours, never fails this, it only delays the work and logs
     * an error for each failed attempt. A workflow class that cannot be created fails here, since
     * that is a fault in the code that no retry fixes.
     */
    public Workers start() {
        // fail now, not on the first continuation, when a workflow class cannot be created
        specsByCodeType.values().forEach(WorkflowSpec::newInstance);
        for (WorkflowSpec spec : specsByCodeType.values()) {
            for (String site : org.conductoross.conductor.sdk.internal.CodeReader.threadSleeps(spec.method)) {
                LOGGER.warn("Workflow {} calls Thread.sleep at {}: workflow code runs again each time it continues,"
                        + " so the sleep is repeated on every replay and holds a worker thread. Use"
                        + " Conductor.sleep(Duration) for a durable wait.", spec.name, site);
            }
        }
        running = true;
        Thread registrar = Thread.ofVirtual().name("conductor-register").start(this::registerUntilDone);
        pollers.add(registrar);
        List<Worker> workers = new ArrayList<>();
        for (Object o : taskObjects) {
            for (Map.Entry<String, Method> e : Registration.workerMethods(o.getClass()).entrySet()) {
                workers.add(new AnnotatedWorker(e.getKey(), e.getValue(), o));
            }
        }
        if (!workers.isEmpty()) {
            taskRunner =
                    new TaskRunnerConfigurer.Builder(taskClient, workers)
                            .withThreadCount(Math.max(threads, workers.size()))
                            .withTaskPollTimeout(100)
                            .build();
            taskRunner.init();
        }
        codeExecutor = Executors.newVirtualThreadPerTaskExecutor();
        for (String codeType : specsByCodeType.keySet()) {
            // the shared queue, and the build's domain when one is set: an execution started with
            // task domains for it prefers this build and falls back when none of it is polling
            List<String> domains = new ArrayList<>();
            if (buildId != null && !buildId.isBlank()) {
                domains.add(buildId);
            }
            domains.add(null);
            for (String domain : domains) {
                Thread t = Thread.ofVirtual().name("conductor-code-" + codeType + "-" + domain).start(() -> poll(codeType, domain));
                pollers.add(t);
            }
        }
        LOGGER.info("Hosting workflows {} and {} tasks as {}", specsByCodeType.keySet(), workers.size(), workerId);
        return this;
    }

    public void shutdown() {
        running = false;
        pollers.forEach(Thread::interrupt);
        if (taskRunner != null) {
            taskRunner.shutdown();
        }
        if (codeExecutor != null) {
            codeExecutor.shutdownNow();
        }
    }

    private void poll(String codeType, String domain) {
        int failures = 0;
        while (running) {
            try {
                List<Task> tasks = taskClient.batchPollTasksInDomain(codeType, domain, workerId, 10, 500);
                failures = 0;
                if (tasks == null || tasks.isEmpty()) {
                    Thread.sleep(50);
                    continue;
                }
                for (Task task : tasks) {
                    codeExecutor.submit(() -> process(codeType, task));
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                if (running) {
                    failures++;
                    java.time.Duration wait = OUTAGE.delay(failures);
                    LOGGER.error("Polling {} failed ({} in a row), retrying in about {}s: {}",
                            codeType, failures, wait.toSeconds(), e.toString());
                    if (!pause(wait)) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Registers the definitions, retrying for up to two minutes: registration is an idempotent
     * upsert, and a worker starting while the server restarts, or while other workers register
     * the same definitions, should wait rather than exit.
     */
    /**
     * Waits, up to {@code max}, for this host to register its definitions when it hosts
     * {@code spec} and has not registered yet; returns at once otherwise. A start in the same
     * process right after {@link #start()} would otherwise find no definition.
     */
    void awaitRegistered(WorkflowSpec spec, java.time.Duration max) {
        boolean hosted = specsByCodeType.values().stream()
                .anyMatch(s -> s.name.equals(spec.name) && s.version == spec.version);
        long until = System.nanoTime() + max.toNanos();
        while (hosted && running && !registered && System.nanoTime() < until) {
            if (!pause(java.time.Duration.ofMillis(50))) {
                return;
            }
        }
    }

    /** Whether the definitions have been registered since {@link #start()}. */
    public boolean registered() {
        return registered;
    }

    private void registerUntilDone() {
        for (int attempt = 1; running; attempt++) {
            try {
                List<Object> defined = new ArrayList<>(taskObjects);
                defined.addAll(definedTaskObjects);
                Registration.register(conductor.client(), specsByCodeType.values(), defined,
                        devMode ? 3600 : Math.max(1, continuationTimeout.toSeconds()), discovery);
                registered = true;
                LOGGER.info("Registered the definitions of {} after {} attempt(s)", specsByCodeType.keySet(), attempt);
                return;
            } catch (RuntimeException e) {
                java.time.Duration wait = OUTAGE.delay(attempt);
                LOGGER.error("Registering the definitions of {} failed (attempt {}), retrying in about {}s: {}",
                        specsByCodeType.keySet(), attempt, wait.toSeconds(), e.toString());
                if (!pause(wait)) {
                    return;
                }
            }
        }
    }

    /** Sleeps for between half and all of {@code wait}; false when interrupted by shutdown. */
    private boolean pause(java.time.Duration wait) {
        long ms = wait.toMillis();
        try {
            Thread.sleep(ms / 2 + java.util.concurrent.ThreadLocalRandom.current().nextLong(ms / 2 + 1));
            return running;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** How long an update's reply is kept for its caller; the caller waits at most 3 minutes. */
    static final long REPLY_TTL_MILLIS = java.time.Duration.ofMinutes(10).toMillis();

    /** At most this many replies are kept, newest first, whatever their age. */
    static final int REPLY_LIMIT = 500;

    private static Map<String, Object> reply(String key, Object value) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put(key, value);
        r.put("at", System.currentTimeMillis());
        return r;
    }

    /**
     * Drops replies older than {@link #REPLY_TTL_MILLIS} and keeps at most {@link #REPLY_LIMIT},
     * so an execution that serves many updates does not grow its variables without bound.
     * Replies without a timestamp, from older workers, count as expired.
     */
    static Map<String, Object> pruneReplies(Map<String, Object> replies, long now) {
        List<Map.Entry<String, Object>> fresh = new ArrayList<>();
        for (Map.Entry<String, Object> e : replies.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> r && r.get("at") instanceof Number at
                    && now - at.longValue() <= REPLY_TTL_MILLIS) {
                fresh.add(e);
            }
        }
        int from = Math.max(0, fresh.size() - REPLY_LIMIT);
        Map<String, Object> kept = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : fresh.subList(from, fresh.size())) {
            kept.put(e.getKey(), e.getValue());
        }
        return kept;
    }

    /** How many handled message ids an execution remembers for de-duplication. */
    static final int SEEN_LIMIT = 500;

    /** Processes one continuation (or handler run) of an execution. */
    void process(String codeType, Task task) {
        String snapshot = "(not fetched)";
        try {
            WorkflowSpec spec = specsByCodeType.get(codeType);
            TaskContinuation marker = task.getContinuation();
            if (marker == null) {
                throw new IllegalStateException("task " + task.getTaskId() + " carries no continuation");
            }
            Execution exec = fetch(task.getWorkflowInstanceId());
            snapshot = describe(exec, marker);
            if ("handlers".equals(marker.getLane())) {
                processHandlers(codeType, spec, exec, task);
                return;
            }
            Map<String, Object> extra = null;
            if (devMode && "main".equals(marker.getLane()) && marker.getN() == 1) {
                extra = Map.of("__timeScale", timeScale);
            }
            Replayer.Outcome outcome =
                    new Replayer(spec, spec.newInstance(), exec)
                            .process(marker.getLane(), marker.getN(), extra);
            logLaneError(task, marker.getLane(), outcome.output);
            complete(codeType, task, TaskResult.Status.COMPLETED, outcome.output, null);
        } catch (NonDeterminismException e) {
            LOGGER.error("Execution {} diverged from its code: {}", task.getWorkflowInstanceId(), e.getMessage());
            LOGGER.error("Execution {} as replayed: {}", task.getWorkflowInstanceId(), snapshot);
            complete(codeType, task, TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, Map.of(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Processing {} of execution {} failed", task.getReferenceTaskName(), task.getWorkflowInstanceId(), e);
            complete(codeType, task, TaskResult.Status.FAILED, Map.of(), e.toString());
        }
    }

    /**
     * A lane started with {@code async} that ended with an exception fails the workflow only when
     * the code waits for it, with {@code join} or {@code get}; one nothing waits for would leave no
     * trace in the execution's status. Its exception goes to the log of the task the lane ran last,
     * where the execution page shows it.
     */
    @SuppressWarnings("unchecked")
    private void logLaneError(Task task, String lane, Map<String, Object> output) {
        if ("main".equals(lane) || !Boolean.TRUE.equals(output.get("done")) || !(output.get("error") instanceof Map<?, ?>)) {
            return;
        }
        Map<String, Object> error = (Map<String, Object>) output.get("error");
        String message = "Lane " + lane + " ended with " + error.get("message") + "."
                + " It fails the workflow only if the code waits for this lane with join or get.";
        try {
            taskClient.logMessageForTask(task.getTaskId(), message);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not log the error of lane {} of execution {}: {}", lane, task.getWorkflowInstanceId(), e.toString());
        }
        LOGGER.warn("Execution {}: {}", task.getWorkflowInstanceId(), message);
    }

    /**
     * Runs signal, update and query handlers for the messages the server claimed for this handler
     * run, on an instance restored from the current variables; then names the condition waits
     * whose condition now holds, which the server completes with the same update.
     */
    @SuppressWarnings("unchecked")
    private void processHandlers(String codeType, WorkflowSpec spec, Execution exec, Task task) {
        List<Map<String, Object>> messages =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("messages", List.of());
        Object instance = spec.newInstance();
        spec.restoreVariables(instance, exec.variables);
        Map<String, Object> replies = new LinkedHashMap<>(exec.replies());
        List<String> seen = exec.seen();
        for (Map<String, Object> message : messages) {
            Map<String, Object> payload = (Map<String, Object>) message.getOrDefault("payload", Map.of());
            String kind = String.valueOf(payload.get("kind"));
            String name = String.valueOf(payload.get("name"));
            // the sender's id survives a retried send; the queue's id does not
            String id = payload.get("id") != null ? String.valueOf(payload.get("id")) : String.valueOf(message.get("id"));
            if (seen.contains(id)) {
                LOGGER.debug("message {} of execution {} was already handled", id, exec.workflowId);
                continue;
            }
            seen.add(id);
            // a query (sent as a message by older clients) must not change the execution:
            // whatever its handler does to the instance is undone
            Map<String, Object> beforeQuery = "query".equals(kind) ? spec.captureVariables(instance) : null;
            try {
                Method handler = spec.handler(kind, name);
                Object value = Replayer.invokeHandler(instance, handler, payload.get("args"));
                if (!"signal".equals(kind)) {
                    replies.put(id, reply("value", Json.plain(value) == null ? "" : Json.plain(value)));
                }
            } catch (RuntimeException e) {
                LOGGER.warn("handler {} {} of execution {} failed: {}", kind, name, exec.workflowId, e.toString());
                if (!"signal".equals(kind)) {
                    replies.put(id, reply("error", e.toString()));
                }
            } finally {
                if (beforeQuery != null) {
                    spec.restoreVariables(instance, beforeQuery);
                }
            }
        }
        Map<String, Object> vars = spec.captureVariables(instance);
        TaskContinuation marker = task.getContinuation();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("protocol", 2);
        output.put("n", marker.getN());
        output.put("fragment", List.of());
        output.put("variables", vars);
        // what this handler started from: the server applies only what the handler changed,
        // and runs it again if another completion changed the same variables meanwhile
        output.put("resumedWith", spec.captureVariablesOf(exec.variables));
        output.put("replies", pruneReplies(replies, System.currentTimeMillis()));
        // the ids of recent messages, so a message queued twice by a retried send applies once
        output.put("seen", seen.size() > SEEN_LIMIT ? new ArrayList<>(seen.subList(seen.size() - SEEN_LIMIT, seen.size())) : seen);
        output.put("releases", releases(spec, exec, vars));
        output.put("done", false);
        complete(codeType, task, TaskResult.Status.COMPLETED, output, null);
    }

    /** The condition waits whose condition holds over the variables the handlers produced. */
    private List<Map<String, Object>> releases(WorkflowSpec spec, Execution exec, Map<String, Object> vars) {
        boolean waiting = exec.tasks.stream().anyMatch(t -> t.ref.startsWith("condition") && "IN_PROGRESS".equals(t.status));
        if (!waiting) {
            return List.of();
        }
        List<Map<String, Object>> releases = new ArrayList<>();
        Replayer.Outcome outcome = new Replayer(spec, spec.newInstance(), exec).process(null, 0, null);
        spec.restoreVariables(outcome.instance, vars);
        for (Map.Entry<String, BooleanSupplier> c : outcome.pendingConditions.entrySet()) {
            if (c.getValue().getAsBoolean()) {
                Map<String, Object> release = new LinkedHashMap<>();
                release.put("ref", c.getKey());
                release.put("output", Map.of("satisfied", true));
                releases.add(release);
            }
        }
        return releases;
    }

    /** What a replay saw, for the log when the code diverges: status, tasks, markers, replay order. */
    private static String describe(Execution exec, TaskContinuation active) {
        StringBuilder sb = new StringBuilder();
        sb.append("status=").append(exec.status).append(" active=").append(active.getLane()).append('#').append(active.getN());
        sb.append(" replay=");
        for (Execution.Marker m : exec.segments()) {
            sb.append(m.lane).append('#').append(m.n).append("@").append(m.step).append(m.done ? "!" : "").append(' ');
        }
        sb.append("tasks=");
        for (Execution.TaskView t : exec.tasks) {
            sb.append(t.seq).append(':').append(t.ref).append('=').append(t.status);
            if (t.marker != null) {
                sb.append('[').append(t.marker.lane).append('#').append(t.marker.n).append(' ').append(t.marker.state).append('@').append(t.marker.step).append(']');
            }
            sb.append(' ');
        }
        sb.append("entries=").append(exec.entries.keySet());
        return sb.toString();
    }

    private Execution fetch(String workflowId) {
        return new Execution(Http.getMap(conductor.client(), "/workflow/" + workflowId, Map.of("includeTasks", "true")));
    }

    private void complete(String codeType, Task task, TaskResult.Status status, Map<String, Object> output, String reason) {
        TaskResult result = new TaskResult(task);
        result.setStatus(status);
        result.setWorkerId(workerId);
        result.setOutputData(new LinkedHashMap<>(output));
        TaskContinuation marker = task.getContinuation();
        if (marker != null) {
            // the server applies the completion only to the lease it handed out; the protocol
            // marks the result as the code's, also when the code failed, so that it never
            // lands on the task the code paused on
            result.getOutputData().put("protocol", 2);
            result.getOutputData().put("n", marker.getN());
            result.getOutputData().put("epoch", marker.getEpoch());
        }
        if (reason != null) {
            result.setReasonForIncompletion(reason.length() > 1000 ? reason.substring(0, 1000) : reason);
        }
        // A server error (the workflow briefly busy) is retried for about 2 s; a refusal (4xx, e.g.
        // the lease was superseded) is final. Longer retries hold a code thread, and under a
        // database outage every code thread ends up waiting (found by the durability bench);
        // past 2 s the server's continuation timeout retries the continuation instead.
        for (int attempt = 1; ; attempt++) {
            try {
                // update-v2 hands back this worker's next continuation, if one is queued: it is
                // leased to us now, so it must be processed, not dropped
                Task next = taskClient.updateTaskV2(result);
                if (next != null && next.getContinuation() != null && next.getContinuation().isLeased() && running) {
                    codeExecutor.submit(() -> process(codeType, next));
                }
                return;
            } catch (Exception e) {
                boolean refused = e instanceof io.orkes.conductor.client.http.ApiException api
                        && api.getStatusCode() >= 400 && api.getStatusCode() < 500;
                if (!running) {
                    // shutting down: the server retries the continuation if this one did not land
                    LOGGER.debug("completing {} of execution {} interrupted by shutdown", task.getReferenceTaskName(), task.getWorkflowInstanceId());
                    return;
                }
                if (refused || attempt >= 4) {
                    LOGGER.warn("completing {} of execution {} was refused: {}", task.getReferenceTaskName(), task.getWorkflowInstanceId(), e.toString());
                    return;
                }
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
