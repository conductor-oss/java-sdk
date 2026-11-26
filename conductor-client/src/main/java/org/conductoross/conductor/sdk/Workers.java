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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.internal.CodeReader;
import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.TaskStubs;
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
import com.netflix.conductor.sdk.workflow.executor.task.WorkerConfiguration;
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
 * <p>An execution's code stays in memory between its continuations ({@link ParkedExecutions}):
 * its lanes are left parked where they paused, and the next continuation wakes its lane with the
 * result of the task it waited for, without re-running the method or fetching the execution. For
 * that the execution is pinned to this process. The workers name a task domain of their own,
 * {@code pin-<workerId>-<random>}, new on every start; each completion tells the server that the
 * execution is held there, and the server offers the execution's next continuation to that
 * domain's queue first. If this process does not take it within the server's pin timeout (it
 * died, or is stalled), any host takes it from the shared queue and replays, as before. So the
 * recording stays what an execution is and the copy in memory only saves work: it is dropped
 * after {@code parkTimeout} without a continuation (an hour by default), when more than {@code
 * maxParked} executions of a workflow are held (1000), and whenever the server says the
 * execution moved on without it (another host continued it; it was retried, rerun or restarted).
 * A retry or rerun therefore always replays. {@code parkTimeout=0} turns this off for a workflow;
 * a server that does not pin executions turns it off for all.
 *
 * <p>Per workflow, {@code conductor.workflow.<name>.<setting>}, else {@code
 * conductor.workflow.all.<setting>}, read from the system properties (see {@link
 * #settings(Function)}): {@code threadCount}, {@code pollingInterval} and {@code pollTimeout}
 * (defaults from {@link Workflow}: 20, 100 ms and 500 ms), {@code paused} (read before each poll),
 * {@code continuationTimeout} and {@code parkTimeout} (seconds, or an ISO-8601 duration) and
 * {@code maxParked}. The task domains a run is started with do not apply to workflow code: every
 * host of a workflow polls its shared queue, and its own queue while it holds executions.
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
    /** The continuation lease a workflow gets when no setting names one. */
    static final Duration CONTINUATION_TIMEOUT = Duration.ofSeconds(30);
    private Duration continuationTimeout = CONTINUATION_TIMEOUT;
    private Duration parkTimeout = Duration.ofHours(1);
    private final ParkedExecutions parked = new ParkedExecutions();
    private final AtomicLong stamps = new AtomicLong();
    /** The task domain of this process alone, where the server offers the executions held here. */
    private String pinDomain;
    private TaskRunnerConfigurer pinnedRunner;
    private Thread parkSweeper;
    private Function<String, String> settings = System::getProperty;
    private TaskRunnerConfigurer workflowRunner;
    private AnnotatedWorkerExecutor taskExecutor;
    private Thread registrar;
    private volatile boolean running;
    private boolean started;
    private boolean closed;
    private volatile boolean registered;

    /**
     * Waits between attempts while the server cannot be reached: 1, 2, 4... seconds, at most a
     * minute, each shortened by up to half at random so that many hosts do not retry in step.
     */
    static final Backoff OUTAGE = Backoff.every(Duration.ofSeconds(1)).factor(2).max(Duration.ofMinutes(1));

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

    /**
     * Development mode: generous continuation timeouts, for breakpoints, and no code kept in
     * memory between continuations, so that every continuation replays.
     */
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
    public Workers continuationTimeout(Duration timeout) {
        this.continuationTimeout = timeout;
        return this;
    }

    /**
     * How long an execution's code stays in memory without a continuation before it is dropped
     * (its next continuation then replays); default an hour. Zero keeps nothing in memory: every
     * continuation replays. {@code conductor.workflow.<name>.parkTimeout} overrides it for one
     * workflow.
     */
    public Workers parkTimeout(Duration timeout) {
        this.parkTimeout = timeout;
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
        List<String> refused = new ArrayList<>();
        for (WorkflowSpec spec : specsByCodeType.values()) {
            for (String site : CodeReader.threadSleeps(spec.method)) {
                LOGGER.warn("Workflow {} calls Thread.sleep at {}: workflow code runs again each time it continues,"
                        + " so the sleep is repeated on every replay and holds a worker thread. Use"
                        + " Conductor.sleep(Duration) for a durable wait.", spec.name, site);
            }
            String report = nonDeterministicCode(spec);
            if (report != null) {
                refused.add(report);
            }
        }
        if (!refused.isEmpty()) {
            // before anything is registered or polled: none of these workers is hosted
            String message = String.join("\n\n", refused);
            LOGGER.error(message);
            throw new NonDeterminismException(message);
        }
        running = true;
        registrar = Thread.ofVirtual().name("conductor-register").start(this::registerUntilDone);
        int tasks = 0;
        if (!taskObjects.isEmpty()) {
            taskExecutor = new AnnotatedWorkerExecutor(taskClient, new TaskSettings());
            for (Object o : taskObjects) {
                for (Method m : TaskStubs.workerMethods(o.getClass()).values()) {
                    m.setAccessible(true); // a task method need not be public
                    taskExecutor.addMethod(m.getAnnotation(WorkerTask.class), m, o);
                    tasks++;
                }
            }
            taskExecutor.startPolling();
        }
        List<Worker> codeWorkers = new ArrayList<>();
        List<Worker> pinnedWorkers = new ArrayList<>();
        Map<String, Integer> threadCounts = new LinkedHashMap<>();
        Map<String, Integer> pollTimeouts = new LinkedHashMap<>();
        for (Map.Entry<String, WorkflowSpec> e : specsByCodeType.entrySet()) {
            WorkflowSpec spec = e.getValue();
            String codeType = e.getKey();
            Workflow annotation = spec.method.getAnnotation(Workflow.class);
            threadCounts.put(codeType, setting(spec, "threadCount", annotation.threadCount()));
            pollTimeouts.put(codeType, setting(spec, "pollTimeout", annotation.pollTimeout()));
            int pollingInterval = setting(spec, "pollingInterval", annotation.pollingInterval());
            BooleanSupplier paused = () -> Boolean.parseBoolean(setting(spec, "paused"));
            codeWorkers.add(new CodeWorker(codeType, pollingInterval, paused));
            if (parkMillis(spec) > 0) {
                // its own queue is polled only while it holds an execution of this workflow: the
                // server offers there only what is pinned here, and a queue nobody polls reads as
                // a worker that is gone, so nothing waits on it
                pinnedWorkers.add(new CodeWorker(codeType, pollingInterval,
                        () -> paused.getAsBoolean() || parked.held(codeType) == 0));
            }
        }
        if (!codeWorkers.isEmpty()) {
            workflowRunner = runner(codeWorkers, threadCounts, pollTimeouts, null, "conductor-workflow");
        }
        if (!pinnedWorkers.isEmpty()) {
            pinDomain = newPinDomain(workerId);
            pinnedRunner = runner(pinnedWorkers, threadCounts, pollTimeouts, pinDomain, "conductor-workflow-pinned");
            parkSweeper = Thread.ofVirtual().name("conductor-parked").start(this::dropIdleUntilClosed);
        }
        LOGGER.info("Hosting workflows {} and {} tasks as {}{}", specsByCodeType.keySet(), tasks, workerId,
                pinDomain == null ? "" : "; executions stay in memory between continuations, pinned to domain " + pinDomain);
        return this;
    }

    /**
     * Polls the workers' continuations on virtual threads, each completion handing back the next
     * one (update-v2): from the shared queue of each code type, or from {@code domain} alone.
     */
    private TaskRunnerConfigurer runner(List<Worker> workers, Map<String, Integer> threadCounts,
            Map<String, Integer> pollTimeouts, String domain, String namePrefix) {
        TaskRunnerConfigurer runner = new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withTaskThreadCount(threadCounts)
                .withTaskPollTimeout(pollTimeouts)
                .withUseVirtualThreads(true)
                .withTaskUpdateV2(true)
                .withDomains(false)
                .withFixedDomain(domain)
                .withWorkerNamePrefix(namePrefix)
                .build();
        runner.init();
        return runner;
    }

    /**
     * A task domain no other process has: this host's id, as far as it can be part of a queue
     * name, and a random part that changes on every start. A process that restarted holds nothing
     * in memory, so it must not be offered what was pinned to the one before it.
     */
    static String newPinDomain(String workerId) {
        String host = workerId == null ? "" : workerId.replaceAll("[^A-Za-z0-9_.-]", "_");
        host = host.length() > 40 ? host.substring(0, 40) : host;
        String random = Long.toString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE), 36);
        return "pin-" + (host.isEmpty() ? "" : host + "-") + random.substring(0, Math.min(8, random.length()));
    }

    /** Drops the executions that sat idle past their {@code parkTimeout}, once a second. */
    private void dropIdleUntilClosed() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            parked.dropIdle();
        }
    }

    /** The executions kept in memory between continuations. */
    ParkedExecutions parked() {
        return parked;
    }

    /** The task domain the executions held here are pinned to, once started; null when none are kept. */
    String pinDomain() {
        return pinDomain;
    }

    /** Stops polling and the background registration. */
    @Override
    public void close() {
        running = false;
        if (registrar != null) {
            registrar.interrupt();
        }
        if (parkSweeper != null) {
            parkSweeper.interrupt();
        }
        if (workflowRunner != null) {
            workflowRunner.shutdown();
        }
        if (pinnedRunner != null) {
            pinnedRunner.shutdown();
        }
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
        // after the pollers: nothing runs a continuation now. Their next ones replay elsewhere
        if (pinDomain != null && !closed) {
            LOGGER.info("Executions kept in memory as {}: {} continuations continued without a replay, {} replayed,"
                    + " {} executions dropped while held", pinDomain, parked.continued, parked.replayed, parked.dropped);
        }
        closed = true;
        parked.closeAll();
    }

    /** The task runner of the hosted workflows, once started. */
    TaskRunnerConfigurer workflowRunner() {
        return workflowRunner;
    }

    /**
     * Checks a workflow's compiled code for places that ask the JVM itself for a random value or
     * the time. Such code can take another path when it runs again, as it does on every
     * continuation and on a retry or rerun. Each place is logged as a warning; with {@code
     * conductor.workflow.<name>.strictDeterminism=true} (or {@code conductor.workflow.all.}) the
     * workflow is refused instead, and this returns the report that says why. Null when the
     * workflow may be hosted.
     */
    private String nonDeterministicCode(WorkflowSpec spec) {
        List<CodeReader.RawSource> found;
        try {
            found = CodeReader.rawSources(spec.method);
        } catch (RuntimeException e) {
            LOGGER.warn("Workflow {} could not be checked for non-deterministic code: {}", spec.name, e.toString());
            return null;
        }
        if (found.isEmpty()) {
            return null;
        }
        if (!Boolean.parseBoolean(setting(spec, "strictDeterminism"))) {
            for (CodeReader.RawSource raw : found) {
                LOGGER.warn("Workflow {} calls {} at {}: workflow code runs again each time it continues, and on a"
                        + " retry or rerun, so this can give another value there and take the code down another"
                        + " path. Use {}. Set conductor.workflow.{}.strictDeterminism=true to refuse to host"
                        + " code like this.", spec.name, raw.call(), raw.site(), raw.replacement(), spec.name);
            }
            return null;
        }
        int site = found.stream().mapToInt(r -> r.site().length()).max().orElse(0);
        int call = found.stream().mapToInt(r -> r.call().length()).max().orElse(0);
        StringBuilder report = new StringBuilder("Workflow ").append(spec.name).append(" cannot be hosted: ")
                .append(found.size() == 1 ? "1 place in its code is" : found.size() + " places in its code are")
                .append(" not deterministic.");
        for (CodeReader.RawSource raw : found) {
            report.append(String.format("%n  %-" + site + "s  %-" + call + "s  use %s", raw.site(), raw.call(), raw.replacement()));
        }
        report.append(String.format("%nA retry or rerun runs this code again and must take the same path. Anything else that can"
                + "%nchange between runs goes in Conductor.once(name, () -> ...)."
                + "%nTo host it anyway: conductor.workflow.%s.strictDeterminism=false", spec.name));
        return report.toString();
    }

    /** A workflow's setting: {@code conductor.workflow.<name>.<key>}, else {@code conductor.workflow.all.<key>}. */
    private String setting(WorkflowSpec spec, String key) {
        return setting(settings, "conductor.workflow", spec.name, key);
    }

    /** {@code <prefix>.<name>.<key>}, else {@code <prefix>.all.<key>}: trimmed, null when unset or blank. */
    private static String setting(Function<String, String> settings, String prefix, String name, String key) {
        String value = settings.apply(prefix + "." + name + "." + key);
        if (value == null || value.isBlank()) {
            value = settings.apply(prefix + ".all." + key);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** A workflow's duration setting, in seconds or as an ISO-8601 duration; {@code otherwise} when unset. */
    private static Duration duration(Function<String, String> settings, WorkflowSpec spec, String key, Duration otherwise) {
        String value = setting(settings, "conductor.workflow", spec.name, key);
        return value == null ? otherwise
                : value.chars().allMatch(Character::isDigit) ? Duration.ofSeconds(Long.parseLong(value))
                : Duration.parse(value);
    }

    /**
     * A workflow's continuation lease in seconds, read from these settings: {@code
     * conductor.workflow.<name>.continuationTimeout}, else {@code conductor.workflow.all.}, else
     * {@code otherwise}.
     */
    static long continuationTimeoutSeconds(Function<String, String> settings, WorkflowSpec spec, Duration otherwise) {
        return Math.max(1, duration(settings, spec, "continuationTimeout", otherwise).toSeconds());
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
        return continuationTimeoutSeconds(settings, spec, continuationTimeout);
    }

    /**
     * How long an execution of a workflow stays in memory without a continuation, in milliseconds:
     * {@code conductor.workflow.<name>.parkTimeout} (seconds, or an ISO-8601 duration), else what
     * {@link #parkTimeout(java.time.Duration)} was given. Zero: its code is not kept in memory.
     * Always zero in dev mode.
     */
    long parkMillis(WorkflowSpec spec) {
        if (devMode) {
            // every continuation replays, which shows code that is not deterministic while it is
            // being written; and a breakpoint may hold a continuation longer than a server waits
            // for the worker an execution is pinned to
            return 0;
        }
        return Math.max(0, duration(settings, spec, "parkTimeout", parkTimeout).toMillis());
    }

    /**
     * The settings of a {@code @WorkerTask} method, as Spring's worker configuration reads them:
     * {@code conductor.worker.<task>.<setting>}, else {@code conductor.worker.all.<setting>}, from
     * {@link #settings(Function)}; unset (0 or null) leaves the annotation's value.
     */
    private final class TaskSettings extends WorkerConfiguration {

        private int number(String task, String key) {
            String value = setting(settings, "conductor.worker", task, key);
            return value == null ? 0 : Integer.parseInt(value);
        }

        @Override
        public int getThreadCount(String task) {
            return number(task, "threadCount");
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
            return setting(settings, "conductor.worker", task, "domain");
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
    void awaitRegistered(WorkflowSpec spec, Duration max) {
        boolean hosted = specsByCodeType.values().stream()
                .anyMatch(s -> s.name.equals(spec.name) && s.version == spec.version);
        long until = System.nanoTime() + max.toNanos();
        while (hosted && running && !registered && System.nanoTime() < until) {
            if (!pause(Duration.ofMillis(50))) {
                return;
            }
        }
    }

    /** The spec of a workflow class these workers host, with the factory it was given. */
    Optional<WorkflowSpec> hostedSpec(Class<?> type) {
        return specsByCodeType.values().stream().filter(s -> s.type == type).findFirst();
    }

    /** Whether the definitions have been registered since {@link #start()}. */
    boolean isRegistered() {
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
                Duration wait = OUTAGE.delay(attempt);
                LOGGER.error("Registering the definitions of {} failed (attempt {}), retrying in about {}s: {}",
                        specsByCodeType.keySet(), attempt, wait.toSeconds(), e.toString());
                if (!pause(wait)) {
                    return;
                }
            }
        }
    }

    /** Sleeps for between half and all of {@code wait}; false when interrupted by shutdown. */
    private boolean pause(Duration wait) {
        long ms = wait.toMillis();
        try {
            Thread.sleep(ms / 2 + ThreadLocalRandom.current().nextLong(ms / 2 + 1));
            return running;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** How long an update's reply is kept for its caller; the caller waits at most 3 minutes. */
    static final long REPLY_TTL_MILLIS = Duration.ofMinutes(10).toMillis();

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
     * A reply without a timestamp counts as expired.
     */
    static Map<String, Object> pruneReplies(Map<String, Object> replies, long now) {
        List<Map.Entry<String, Object>> fresh = new ArrayList<>();
        for (Map.Entry<String, Object> e : replies.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> r && r.get("at") instanceof Number at
                    && now - at.longValue() <= REPLY_TTL_MILLIS) {
                fresh.add(e);
            }
        }
        // oldest first, by when each was handled: the map itself comes back in no particular order
        fresh.sort(Comparator.comparingLong(e -> ((Number) ((Map<?, ?>) e.getValue()).get("at")).longValue()));
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
        Supplier<String> snapshot = () -> "(not fetched)"; // the execution as replayed, built only to report a divergence
        String workflowId = task.getWorkflowInstanceId();
        Replayer replayer = null; // the lanes of this continuation, until they are parked or closed
        try {
            WorkflowSpec spec = specsByCodeType.get(codeType);
            TaskContinuation marker = task.getContinuation();
            if (marker == null) {
                throw new IllegalStateException("task " + task.getTaskId() + " carries no continuation");
            }
            String lane = laneOf(task);
            int n = numberOf(task);
            if ("handlers".equals(lane)) {
                Execution exec = fetch(workflowId);
                snapshot = () -> describe(exec, lane, n);
                return processHandlers(spec, exec, task);
            }
            // A server that pins executions hands out the variables with the continuation; only
            // then is the code kept in memory, since only then does its next continuation come here
            long keepMillis = spec == null || pinDomain == null || marker.getVariables() == null ? 0 : parkMillis(spec);
            Replayer.Outcome outcome = null;
            ParkedExecutions.Parked held = parked.take(codeType, workflowId);
            if (held != null) {
                // The stamp is what this process sent with the execution's last continuation. The
                // server still naming it means that completion applied and nothing has changed
                // what the code did since: the lanes in memory are where the execution is.
                if (keepMillis > 0 && held.stamp().equals(marker.getStamp()) && held.replayer().awaits(lane, n)) {
                    replayer = held.replayer();
                    try {
                        outcome = replayer.readsTasks(lane) || task.getExternalOutputPayloadStoragePath() != null
                                ? replayer.proceed(lane, n, fetch(workflowId))
                                : replayer.proceed(lane, n, marker.getVariables(), arrived(task));
                        parked.continued.incrementAndGet();
                        LOGGER.debug("Execution {} continued {}#{} from the code kept in memory", workflowId, lane, n);
                    } catch (NonDeterminismException | IllegalStateException e) {
                        // nothing of this continuation was sent, so the recording is still what
                        // the execution is: replay it
                        LOGGER.warn("Execution {} could not continue {}#{} from the code kept in memory ({}): replaying it",
                                workflowId, lane, n, e.getMessage());
                        replayer.close();
                        replayer = null;
                        outcome = null;
                    }
                } else {
                    // another host continued the execution, or it was retried, rerun or restarted
                    LOGGER.debug("Execution {} moved on without the code kept in memory: replaying it", workflowId);
                    held.replayer().close();
                }
            }
            if (outcome == null) {
                Execution exec = fetch(workflowId);
                snapshot = () -> describe(exec, lane, n);
                replayer = new Replayer(spec, spec.newInstance(), exec);
                outcome = replayer.begin(lane, n);
                parked.replayed.incrementAndGet();
            }
            logLaneError(task, lane, outcome.output);
            if (!"main".equals(lane)) {
                outcome.output.remove("error"); // the server reads what the main lane raised only
            }
            if (keepMillis > 0 && replayer.hasPausedLanes()) {
                // the completion says so: the server pins the execution to this process and hands
                // the stamp back with its next continuation
                String stamp = pinDomain + "." + stamps.incrementAndGet();
                outcome.output.put("pin", Map.of("domain", pinDomain, "stamp", stamp));
                replayer.forgetHistory();
                parked.park(codeType, workflowId, replayer, stamp, keepMillis, setting(spec, "maxParked", 1000));
            } else {
                replayer.close();
            }
            replayer = null;
            return result(task, TaskResult.Status.COMPLETED, outcome.output, null);
        } catch (NonDeterminismException e) {
            LOGGER.error("Execution {} diverged from its code: {}", workflowId, e.getMessage());
            LOGGER.error("Execution {} as replayed: {}", workflowId, snapshot.get());
            return result(task, TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, Map.of(), e.getMessage());
        } catch (Exception | StackOverflowError e) {
            LOGGER.error("Processing {} of execution {} failed", task.getReferenceTaskName(), workflowId, e);
            return result(task, TaskResult.Status.FAILED, Map.of(), e.toString());
        } finally {
            if (replayer != null) {
                replayer.close();
            }
        }
    }

    /** The task a poll handed out, as the lane that waited for it reads it. */
    private static Execution.TaskView arrived(Task task) {
        return Execution.TaskView.of(task.getReferenceTaskName(), task.getStatus() == null ? null : task.getStatus().name(),
                task.getOutputData(), task.getReasonForIncompletion());
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
            // a query sent as a message must not change the execution:
            // whatever its handler does to the instance is undone
            Map<String, Object> beforeQuery = "query".equals(kind) ? spec.captureVariables(instance) : null;
            try {
                Method handler = spec.handler(kind, name);
                Object value = Replayer.invokeHandler(instance, handler, payload.get("args"));
                // a signal's sender waits for no reply: its entry only says it was handled. So
                // does the entry of a handler that returned null: the caller reads no value, null
                Object returned = Json.plain(value);
                replies.put(id, "signal".equals(kind) || returned == null ? reply(null, null) : reply("value", returned));
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
        // what this handler started from, kept on the marker for replay; the server applies the
        // variables as sent (one continuation runs at a time, so nothing else changed them)
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
        Object instance = spec.newInstance();
        Replayer.Outcome outcome = new Replayer(spec, instance, exec).process(null, 0);
        spec.restoreVariables(instance, vars);
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
