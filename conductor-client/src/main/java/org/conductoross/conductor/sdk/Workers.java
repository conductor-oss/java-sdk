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
import java.util.function.BooleanSupplier;
import java.util.function.Function;
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
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorkerExecutor;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * Hosts workflow classes and task classes in this process.
 *
 * <p>{@link #start()} registers, from the annotations, a skeleton definition for each workflow and
 * a task definition for each task, then starts polling. The {@code @WorkerTask} methods run as
 * {@link AnnotatedWorkerExecutor} runs them: thread count, polling interval, poll timeout, poller
 * count and domain from {@code conductor.worker.<task>.<setting>}, else {@code
 * conductor.worker.all.<setting>} (read like the workflow settings below), else the annotation; a
 * task whose thread count none of them sets gets 8 threads. Each workflow runs on a {@link
 * com.netflix.conductor.client.automator.TaskRunner} too, as the worker of its code type: a
 * continuation arrives as the task the code paused on, carrying its continuation marker, and is
 * processed by re-running the workflow method against the execution's recorded tasks, so any host
 * of the same code can process any continuation. Continuations of one execution are processed one
 * at a time, and completed with update-v2, which hands back the next one.
 *
 * <p>Per workflow, {@code conductor.workflow.<name>.<setting>}, else {@code
 * conductor.workflow.all.<setting>}, read from the system properties (see {@link
 * #settings(Function)}): {@code threadCount}, {@code pollingInterval} and {@code pollTimeout}
 * (defaults from {@link Workflow}: 20, 100 ms and 500 ms), {@code paused} (read before each poll),
 * and {@code continuationTimeout} (seconds, or an ISO-8601 duration). Workflows have no task
 * domains: every host of a workflow polls its shared queue.
 */
public final class Workers implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Workers.class);

    private final Conductor conductor;
    private final TaskClient taskClient;
    private final Map<String, WorkflowSpec> specsByCodeType = new LinkedHashMap<>();
    private final List<Object> taskObjects = new ArrayList<>();
    private final List<Object> definedTaskObjects = new ArrayList<>();
    private String workerId;
    private boolean devMode;
    private boolean discovery = true;
    private java.time.Duration continuationTimeout = java.time.Duration.ofSeconds(30);
    private Function<String, String> settings = System::getProperty;
    private TaskRunnerConfigurer workflowRunner;
    private AnnotatedWorkerExecutor taskExecutor;
    private Thread registrar;
    private volatile boolean running;
    private boolean started;
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

    /** Development mode: generous continuation timeouts, for breakpoints. */
    public Workers devMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    /**
     * How long the server waits for a continuation it handed to a worker before retrying it
     * elsewhere (the continuation lease); default 30 seconds. It bounds how long an
     * execution stalls when a poll response is lost to a network failure or the worker dies while
     * processing. It must exceed the longest replay of an execution. Dev mode uses an hour, so a
     * breakpoint does not trigger a retry. {@code conductor.workflow.<name>.continuationTimeout}
     * overrides it for one workflow.
     */
    public Workers continuationTimeout(java.time.Duration timeout) {
        this.continuationTimeout = timeout;
        return this;
    }

    /**
     * Where the per-workflow settings are read, by key ({@code conductor.workflow.<name>.threadCount}
     * and so on); {@link System#getProperty(String)} by default.
     */
    public Workers settings(Function<String, String> settings) {
        this.settings = settings;
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
    public <T> Workers workflows(Class<T> type, Supplier<T> factory) {
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
        if (started) {
            throw new IllegalStateException("these workers were already started: make new ones with Conductor.workflows(..)");
        }
        started = true;
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
        registrar = Thread.ofVirtual().name("conductor-register").start(this::registerUntilDone);
        int tasks = 0;
        if (!taskObjects.isEmpty()) {
            taskExecutor = new AnnotatedWorkerExecutor(taskClient, new TaskSettings());
            for (Object o : taskObjects) {
                for (Method m : Registration.workerMethods(o.getClass()).values()) {
                    taskExecutor.addMethod(m.getAnnotation(WorkerTask.class), m, o);
                    tasks++;
                }
            }
            taskExecutor.startPolling();
        }
        List<Worker> codeWorkers = new ArrayList<>();
        Map<String, Integer> threadCounts = new LinkedHashMap<>();
        Map<String, Integer> pollTimeouts = new LinkedHashMap<>();
        for (Map.Entry<String, WorkflowSpec> e : specsByCodeType.entrySet()) {
            WorkflowSpec spec = e.getValue();
            Workflow annotation = spec.method.getAnnotation(Workflow.class);
            threadCounts.put(e.getKey(), setting(spec, "threadCount", annotation.threadCount()));
            pollTimeouts.put(e.getKey(), setting(spec, "pollTimeout", annotation.pollTimeout()));
            codeWorkers.add(new CodeWorker(e.getKey(), setting(spec, "pollingInterval", annotation.pollingInterval()),
                    () -> Boolean.parseBoolean(setting(spec, "paused"))));
        }
        if (!codeWorkers.isEmpty()) {
            workflowRunner = new TaskRunnerConfigurer.Builder(taskClient, codeWorkers)
                    .withTaskThreadCount(threadCounts)
                    .withTaskPollTimeout(pollTimeouts)
                    .withUseVirtualThreads(true)
                    .withTaskUpdateV2(true)
                    .withDomains(false)
                    .withWorkerNamePrefix("conductor-workflow")
                    .build();
            workflowRunner.init();
        }
        LOGGER.info("Hosting workflows {} and {} tasks as {}", specsByCodeType.keySet(), tasks, workerId);
        return this;
    }

    /** Stops polling and the background registration. */
    @Override
    public void close() {
        running = false;
        if (registrar != null) {
            registrar.interrupt();
        }
        if (workflowRunner != null) {
            workflowRunner.shutdown();
        }
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
    }

    /** The task runner of the hosted workflows, once started. */
    TaskRunnerConfigurer workflowRunner() {
        return workflowRunner;
    }

    /** A workflow's setting: {@code conductor.workflow.<name>.<key>}, else {@code conductor.workflow.all.<key>}. */
    private String setting(WorkflowSpec spec, String key) {
        String value = settings.apply("conductor.workflow." + spec.name + "." + key);
        if (value == null || value.isBlank()) {
            value = settings.apply("conductor.workflow.all." + key);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int setting(WorkflowSpec spec, String key, int otherwise) {
        String value = setting(spec, key);
        return value == null ? otherwise : Integer.parseInt(value);
    }

    /** A workflow's continuation lease in seconds: an hour in dev mode. */
    long continuationTimeoutSeconds(WorkflowSpec spec) {
        if (devMode) {
            return 3600;
        }
        String value = setting(spec, "continuationTimeout");
        java.time.Duration timeout = value == null ? continuationTimeout
                : value.chars().allMatch(Character::isDigit) ? java.time.Duration.ofSeconds(Long.parseLong(value))
                : java.time.Duration.parse(value);
        return Math.max(1, timeout.toSeconds());
    }

    /**
     * The settings of a {@code @WorkerTask} method, as Spring's worker configuration reads them:
     * {@code conductor.worker.<task>.<setting>}, else {@code conductor.worker.all.<setting>}, from
     * {@link #settings(Function)}; unset (0 or null) leaves the annotation's value.
     */
    private final class TaskSettings extends com.netflix.conductor.sdk.workflow.executor.task.WorkerConfiguration {

        private String get(String task, String key) {
            String value = settings.apply("conductor.worker." + task + "." + key);
            if (value == null || value.isBlank()) {
                value = settings.apply("conductor.worker.all." + key);
            }
            return value == null || value.isBlank() ? null : value.trim();
        }

        private int number(String task, String key) {
            String value = get(task, key);
            return value == null ? 0 : Integer.parseInt(value);
        }

        @Override
        public int getThreadCount(String task) {
            return number(task, "threadCount");
        }

        /** What every hosted task had before tasks were configured one by one. */
        @Override
        public int getDefaultThreadCount() {
            return 8;
        }

        @Override
        public int getPollingInterval(String task) {
            return number(task, "pollingInterval");
        }

        @Override
        public int getPollTimeout(String task) {
            return number(task, "pollTimeout");
        }

        @Override
        public int getPollerCount(String task) {
            return number(task, "pollerCount");
        }

        @Override
        public String getDomain(String task) {
            return get(task, "domain");
        }
    }

    /** The worker of a workflow's code type: running a task processes a continuation. */
    private final class CodeWorker implements Worker {

        private final String codeType;
        private final int pollingInterval;
        private final BooleanSupplier paused;

        CodeWorker(String codeType, int pollingInterval, BooleanSupplier paused) {
            this.codeType = codeType;
            this.pollingInterval = pollingInterval;
            this.paused = paused;
        }

        @Override
        public String getTaskDefName() {
            return codeType;
        }

        @Override
        public TaskResult execute(Task task) {
            return process(codeType, task);
        }

        @Override
        public int getPollingInterval() {
            return pollingInterval;
        }

        @Override
        public boolean paused() {
            return paused.getAsBoolean();
        }

        @Override
        public String getIdentity() {
            return workerId;
        }
    }

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

    /** The spec of a workflow class these workers host, with the factory it was given. */
    java.util.Optional<WorkflowSpec> hostedSpec(Class<?> type) {
        return specsByCodeType.values().stream().filter(s -> s.type == type).findFirst();
    }

    /** Whether the definitions have been registered since {@link #start()}. */
    public boolean isRegistered() {
        return registered;
    }

    /**
     * Registers the definitions, retrying with backoff for as long as it takes: registration is an
     * idempotent upsert, and a worker starting while the server is down, or while other workers
     * register the same definitions, waits rather than exits.
     */
    private void registerUntilDone() {
        for (int attempt = 1; running; attempt++) {
            try {
                List<Object> defined = new ArrayList<>(taskObjects);
                defined.addAll(definedTaskObjects);
                Registration.register(conductor.client(), specsByCodeType.values(), defined,
                        this::continuationTimeoutSeconds, discovery);
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

    /** A handled message's entry: its reply, if it has one ({@code key} null for a signal), and when. */
    private static Map<String, Object> reply(String key, Object value) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (key != null) {
            r.put(key, value);
        }
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


    /**
     * Processes one continuation (or handler run) of an execution, and returns its completion. It
     * never throws: a failure is the code's result, not the task's.
     */
    TaskResult process(String codeType, Task task) {
        String snapshot = "(not fetched)";
        try {
            WorkflowSpec spec = specsByCodeType.get(codeType);
            TaskContinuation marker = task.getContinuation();
            if (marker == null) {
                throw new IllegalStateException("task " + task.getTaskId() + " carries no continuation");
            }
            Execution exec = fetch(task.getWorkflowInstanceId());
            String lane = laneOf(task);
            snapshot = describe(exec, lane, numberOf(task));
            if ("handlers".equals(lane)) {
                return processHandlers(spec, exec, task);
            }
            Replayer.Outcome outcome =
                    new Replayer(spec, spec.newInstance(), exec).process(lane, numberOf(task));
            logLaneError(task, lane, outcome.output);
            if (!"main".equals(lane)) {
                // the server reads what a lane returned or raised for the main lane only
                outcome.output.remove("result");
                outcome.output.remove("error");
            }
            return result(task, TaskResult.Status.COMPLETED, outcome.output, null);
        } catch (NonDeterminismException e) {
            LOGGER.error("Execution {} diverged from its code: {}", task.getWorkflowInstanceId(), e.getMessage());
            LOGGER.error("Execution {} as replayed: {}", task.getWorkflowInstanceId(), snapshot);
            return result(task, TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, Map.of(), e.getMessage());
        } catch (Exception | StackOverflowError e) {
            LOGGER.error("Processing {} of execution {} failed", task.getReferenceTaskName(), task.getWorkflowInstanceId(), e);
            return result(task, TaskResult.Status.FAILED, Map.of(), e.toString());
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
    private TaskResult processHandlers(WorkflowSpec spec, Execution exec, Task task) {
        List<Map<String, Object>> messages =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("messages", List.of());
        Object instance = spec.newInstance();
        spec.restoreVariables(instance, exec.variables);
        // every handled message is in the replies, by the sender's id: a message queued twice by
        // a retried send applies once
        Map<String, Object> replies = new LinkedHashMap<>(exec.replies());
        for (Map<String, Object> message : messages) {
            Map<String, Object> payload = (Map<String, Object>) message.getOrDefault("payload", Map.of());
            String kind = String.valueOf(payload.get("kind"));
            String name = String.valueOf(payload.get("name"));
            // the sender's id survives a retried send; the queue's id does not
            String id = payload.get("id") != null ? String.valueOf(payload.get("id")) : String.valueOf(message.get("id"));
            if (replies.containsKey(id)) {
                LOGGER.debug("message {} of execution {} was already handled", id, exec.workflowId);
                continue;
            }
            // a query (sent as a message by older clients) must not change the execution:
            // whatever its handler does to the instance is undone
            Map<String, Object> beforeQuery = "query".equals(kind) ? spec.captureVariables(instance) : null;
            try {
                Method handler = spec.handler(kind, name);
                Object value = Replayer.invokeHandler(instance, handler, payload.get("args"));
                // a signal's sender waits for no reply: its entry only says it was handled
                replies.put(id, "signal".equals(kind)
                        ? reply(null, null)
                        : reply("value", Json.plain(value) == null ? "" : Json.plain(value)));
            } catch (RuntimeException e) {
                LOGGER.warn("handler {} {} of execution {} failed: {}", kind, name, exec.workflowId, e.toString());
                replies.put(id, "signal".equals(kind) ? reply(null, null) : reply("error", e.toString()));
            } finally {
                if (beforeQuery != null) {
                    spec.restoreVariables(instance, beforeQuery);
                }
            }
        }
        Map<String, Object> vars = spec.captureVariables(instance);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("variables", vars);
        // what this handler started from: the server applies only what the handler changed,
        // and runs it again if another completion changed the same variables meanwhile
        output.put("resumedWith", spec.captureVariablesOf(exec.variables));
        output.put("replies", pruneReplies(replies, System.currentTimeMillis()));
        output.put("releases", releases(spec, exec, vars));
        return result(task, TaskResult.Status.COMPLETED, output, null);
    }

    /** The condition waits whose condition holds over the variables the handlers produced. */
    private List<Map<String, Object>> releases(WorkflowSpec spec, Execution exec, Map<String, Object> vars) {
        boolean waiting = exec.tasks.stream().anyMatch(t -> t.ref.startsWith("condition") && "IN_PROGRESS".equals(t.status));
        if (!waiting) {
            return List.of();
        }
        List<Map<String, Object>> releases = new ArrayList<>();
        Replayer.Outcome outcome = new Replayer(spec, spec.newInstance(), exec).process(null, 0);
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

    /**
     * The lane of the code a handed-out continuation runs: its entry names it, as does the entry the
     * server builds for the first continuation and the handler lane.
     */
    private static String laneOf(Task task) {
        Object lane = entryMetadata(task).get("lane");
        return lane == null ? "main" : lane.toString();
    }

    /** The continuation's number in its lane. */
    private static int numberOf(Task task) {
        return entryMetadata(task).get("continuation") instanceof Number n ? n.intValue() : 1;
    }

    private static Map<String, Object> entryMetadata(Task task) {
        return task.getWorkflowTask() == null || task.getWorkflowTask().getMetadata() == null
                ? Map.of()
                : task.getWorkflowTask().getMetadata();
    }

    /** What a replay saw, for the log when the code diverges: status, tasks, markers, replay order. */
    private static String describe(Execution exec, String lane, int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("status=").append(exec.status).append(" active=").append(lane).append('#').append(n);
        sb.append(" replay=");
        for (Execution.Marker m : exec.segments()) {
            sb.append(m.lane).append('#').append(m.n).append("@").append(m.endTime).append(m.done ? "!" : "").append(' ');
        }
        sb.append("tasks=");
        for (Execution.TaskView t : exec.tasks) {
            sb.append(t.seq).append(':').append(t.ref).append('=').append(t.status);
            if (t.marker != null) {
                sb.append('[').append(t.marker.lane).append('#').append(t.marker.n).append(' ').append(t.marker.state()).append('@').append(t.marker.endTime).append(']');
            }
            sb.append(' ');
        }
        sb.append("entries=").append(exec.entries.keySet());
        return sb.toString();
    }

    private Execution fetch(String workflowId) {
        return new Execution(Http.getMap(conductor.client(), "/workflow/" + workflowId, Map.of("includeTasks", "true")));
    }

    private TaskResult result(Task task, TaskResult.Status status, Map<String, Object> output, String reason) {
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
            result.getOutputData().put("n", numberOf(task));
            result.getOutputData().put("retryCount", marker.getRetryCount());
        }
        if (reason != null) {
            result.setReasonForIncompletion(reason.length() > 1000 ? reason.substring(0, 1000) : reason);
        }
        return result;
    }
}
