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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Functions.Fn1;
import org.conductoross.conductor.sdk.Functions.Pred;
import org.conductoross.conductor.sdk.internal.ActionCase;
import org.conductoross.conductor.sdk.internal.CodeLoops;
import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Entries;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.TaskStubs;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

import com.netflix.conductor.client.http.ConductorClient;

/**
 * Native workflows for Conductor.
 *
 * <p>Inside a {@link Workflow} method, a call to a {@code @WorkerTask} method of a task class the
 * workflow takes in its constructor, and the static methods of this class, are the workflow's
 * calls to the server: each one becomes a task in the execution's graph. Everything else in the
 * method is ordinary Java. Import the static methods statically:
 *
 * <pre>{@code
 * import static Conductor.*;
 *
 * public class OrderWorkflow {
 *     private final PaymentTasks payments; // a stub Conductor passes: its calls schedule the tasks
 *
 *     public OrderWorkflow(PaymentTasks payments) {
 *         this.payments = payments;
 *     }
 *
 *     @Workflow(name = "order", version = 1)
 *     public OrderResult process(OrderRequest order) {
 *         Payment payment = payments.charge(order.id(), order.total());
 *         if (payment.declined()) return OrderResult.declined();
 *         sleep(Duration.ofMinutes(5));
 *         return OrderResult.ok(payment);
 *     }
 * }
 * }</pre>
 *
 * <p>Outside workflow code, an instance of this class runs native workflows over a {@link
 * ConductorClient}: create one with {@link #of(ConductorClient)}, run workflows and tasks with
 * {@link #workflows} and {@link #tasks}, and start executions with {@link #start}. The client is configured as for any
 * Conductor client (its builder, {@link ConductorClient#fromEnvironment()}, an Orkes {@code
 * ApiClient} with keys, or the Spring bean), and stays its creator's to shut down.
 *
 * <pre>{@code
 * ConductorClient client = ConductorClient.fromEnvironment();
 * try (Conductor conductor = Conductor.of(client)) {
 *     conductor.workflows(OrderWorkflow.class).tasks(new PaymentTasks()).start();
 *     OrderResult result = conductor.execute(OrderWorkflow::process, order, Duration.ofMinutes(1));
 * }
 * client.shutdown();
 * }</pre>
 *
 * <p>Rules for workflow code: anything non-deterministic or with a side effect that is not a task
 * goes through {@link #once}, {@link #now} or {@link #random}; blocking I/O belongs in tasks.
 */
public final class Conductor implements AutoCloseable {


    // ================================================================== workflow code

    /**
     * A task stub: an instance of a class with {@code @WorkerTask} methods whose calls schedule
     * those tasks. Conductor passes one for each task-class parameter of a workflow's
     * constructor; this is for a factory given to {@code conductor.workflows(Type.class, factory)},
     * when the constructor also takes something else:
     *
     * <pre>{@code
     * conductor.workflows(OrderWorkflow.class, () -> new OrderWorkflow(Conductor.stub(OrderTasks.class), config))
     * }</pre>
     */
    public static <T> T stub(Class<T> taskClass) {
        return TaskStubs.stub(taskClass);
    }

    /** A task by name: for names known only at runtime, or tasks owned by another team. */
    public static TaskCall task(String name) {
        return new TaskCall(name);
    }

    // ------------------------------------------------------------------ values recorded once

    /** Runs the supplier here, once, and records the result; a re-run returns the recorded value. */
    @SuppressWarnings("unchecked")
    public static <T> T once(String name, Supplier<T> supplier) {
        return (T) Engine.current().once(name, supplier);
    }

    /** The current time, recorded so that a re-run sees the same instant. */
    public static Instant now() {
        return Engine.current().now();
    }

    /** A random number in [0, 1), recorded. */
    public static double random() {
        return Engine.current().random();
    }

    /** The id of the execution this code is running for. */
    public static String workflowId() {
        return Engine.current().workflowId();
    }

    // ------------------------------------------------------------------ parallelism

    /**
     * Runs the block as a parallel branch (a lane of its own in a FORK_JOIN) and carries on without
     * waiting for it. The branch's result is a {@link Future}: wait with {@link #join} and read it
     * with {@link Future#resultNow()}, or wait and read at once with {@link Future#get()}, which
     * wraps what the branch threw in an {@link ExecutionException}. For one
     * branch per item of a list, waited for together, use {@link #fanOut}.
     *
     * <pre>{@code
     * Future<Quote> a = async(() -> tasks.quote("a"));
     * Future<Quote> b = async(() -> tasks.quote("b"));
     * join(a, b);                                   // the branches' failures are thrown here
     * Quote best = cheaper(a.resultNow(), b.resultNow());
     * }</pre>
     *
     * The wait is durable: the workflow pauses on a JOIN and no thread waits. A branch cannot be
     * cancelled ({@code cancel} returns false), and {@code get(timeout, unit)} is not supported. A
     * branch that is never joined still runs, the workflow stays running until it finishes, and its
     * failure does not fail the workflow.
     */
    public static <T> Future<T> async(Supplier<T> body) {
        return Engine.current().async(body);
    }

    /**
     * Waits for the given branches and everything they started, then throws the first failure
     * among them, in the order given, as the branch threw it (a {@link TaskFailure}, say).
     */
    public static void join(Future<?>... branches) {
        Engine.current().join(List.of(branches));
    }

    /**
     * Runs the body once per item, in parallel, each item a branch of a FORK_JOIN, and waits for
     * them all: returns the results in item order, as {@link #forEach} does for items one after
     * another. A failed item throws here, as the item threw it.
     */
    public static <I, R> List<R> fanOut(String name, List<I> items, Function<I, R> body) {
        return Engine.current().fanOut(name, new ArrayList<>(items), body);
    }

    public static <I, R> List<R> fanOut(List<I> items, Function<I, R> body) {
        return fanOut("fanout", items, body);
    }

    // ------------------------------------------------------------------ control flow helpers

    /**
     * A switch the engine can see. Declares every case up front, so the execution's graph shows
     * the decision with all its branches and which one the value took (a SWITCH task, with the
     * taken case in its output); the body of the taken case runs as ordinary workflow code, and
     * its calls appear inside that branch.
     *
     * <pre>{@code
     * switchOn("oddEvenDecision", stats.oddEven())
     *     .when("0", () -> { tasks.even(); })
     *     .when("1", () -> { tasks.odd(); })
     *     .otherwise(() -> notes.add("unexpected"))
     *     .run();
     *
     * String carrier = switchOn("region", region)
     *     .when("eu", () -> tasks.euCarrier(item))
     *     .otherwise(() -> tasks.hold(item))
     *     .get();
     * }</pre>
     *
     * A case with no task calls is an empty branch; a value no case handles runs {@code otherwise},
     * or fails the workflow when there is none. Plain {@code if}/{@code else} remains ordinary
     * code; use this when the branch should be visible in the execution.
     */
    public static SwitchStart switchOn(String name, Object value) {
        return new SwitchStart(name, value);
    }

    /** A switch before its first case; the first case's body fixes the result type. */
    public static final class SwitchStart {
        private final String name;
        private final Object value;

        SwitchStart(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        /** The body for this case; the key is compared with the value's string form. */
        public <R> Switch<R> when(Object key, Functions.CaseBody<R> body) {
            return new Switch<R>(name, value).when(key, body);
        }

        public Switch<Void> when(Object key, Functions.CaseAction body) {
            return new Switch<Void>(name, value).when(key, body);
        }
    }

    /** A switch under construction; see {@link #switchOn(String, Object)}. */
    public static final class Switch<T> {
        private final String name;
        private final Object value;
        private final LinkedHashMap<String, Supplier<?>> cases = new LinkedHashMap<>();
        private Supplier<?> otherwise;

        Switch(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        /** The body for this case; the key is compared with the value's string form. */
        public Switch<T> when(Object key, Functions.CaseBody<? extends T> body) {
            if (cases.putIfAbsent(String.valueOf(key), body) != null) {
                throw new IllegalArgumentException("case " + key + " of " + name + " is declared twice");
            }
            return this;
        }

        public Switch<T> when(Object key, Functions.CaseAction body) {
            return when(key, new ActionCase<T>(body));
        }

        /** The body when no case matches; without it an unmatched value fails the workflow. */
        public Switch<T> otherwise(Functions.CaseBody<? extends T> body) {
            this.otherwise = body;
            return this;
        }

        public Switch<T> otherwise(Functions.CaseAction body) {
            return otherwise(new ActionCase<T>(body));
        }

        /** Runs the taken case and returns its value. */
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) Engine.current().switchOn(name, value, cases, otherwise);
        }

        /** Runs the taken case. */
        public void run() {
            get();
        }
    }

    /** Waits on the server (a WAIT task); the code resumes when the timer fires. */
    public static void sleep(Duration duration) {
        Command wait = new Command(Command.Kind.WAIT, "sleep");
        wait.input.put("duration", Entries.duration(duration));
        wait.resultType = Void.class;
        Engine.current().call(wait);
    }

    /**
     * Waits on the server until the given instant (a WAIT task with {@code until}). The server
     * resolves {@code until} to the minute, so the wait ends at the start of the minute after
     * {@code when} at the latest.
     */
    public static void sleepUntil(Instant when) {
        Instant minute = when.truncatedTo(ChronoUnit.MINUTES);
        if (minute.isBefore(when)) {
            minute = minute.plus(Duration.ofMinutes(1));
        }
        String until = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(ZoneOffset.UTC)
                .format(minute);
        Command wait = new Command(Command.Kind.WAIT, "sleep_until");
        wait.input.put("until", until);
        wait.resultType = Void.class;
        Engine.current().call(wait);
    }

    /**
     * Runs a task once per item, one after another, on the server (a DO_WHILE over the items),
     * and returns the results in order. The code is woken once, when all items are done. The task
     * is named through a stub, as the rest of the workflow calls it: {@code forEach("labels",
     * items, tasks::labelItem)}; the stub is not called. The task method takes the item as its
     * only parameter. For items in parallel, use {@link #fanOut}.
     */
    @SuppressWarnings("unchecked")
    public static <I, R> List<R> forEach(String name, List<I> items, Functions.TaskOf<I, R> task) {
        Command command = TaskStubs.taskCommand(MethodRefs.resolve(task), new Object[] {null});
        String itemParameter = command.input.keySet().iterator().next();
        command.input.clear();
        return (List<R>) (List<?>) Engine.current().forEach(name, new ArrayList<>(items), command, itemParameter);
    }

    public static <I, R> List<R> forEach(List<I> items, Functions.TaskOf<I, R> task) {
        return forEach("for_each", items, task);
    }

    /**
     * Repeats a single task call on the server until the condition holds or the attempts run out,
     * waiting between attempts (a DO_WHILE with a WAIT). The code is woken once, at the end. The
     * condition is a method reference to a boolean property of the result, e.g. {@code
     * JobStatus::done}, so that the server can evaluate it.
     */
    @SuppressWarnings("unchecked")
    public static <T> T repeatUntil(Supplier<T> body, Pred<T> until, Backoff backoff) {
        return (T) Engine.current().repeatUntil(body, until, backoff);
    }

    /**
     * A loop the execution shows as one, named: runs {@code body}, then runs it again while {@code
     * condition} holds, as a Java {@code do}-{@code while} does. The body is ordinary workflow code:
     * it may call tasks, branch, fork, start sub workflows and loop again, and each call wakes the
     * code as usual. The execution diagram draws the calls as one loop named {@code name}, with a run
     * for each pass, as it draws a plain Java loop, which is read from the compiled code; this one is
     * named by the code instead.
     *
     * <pre>{@code
     * doWhile("poll_shipment", () -> {
     *     status = tasks.shipmentStatus(orderId);
     *     if (!status.delivered()) {
     *         sleep(Duration.ofHours(1));
     *     }
     * }, () -> !status.delivered());
     * }</pre>
     *
     * For one task the server repeats on its own, waking the code once at the end, use {@link
     * #forEach} or {@link #repeatUntil}.
     */
    public static void doWhile(String name, Functions.CaseAction body, Functions.Condition condition) {
        CodeLoops.Named loop = CodeLoops.enter(name, body, condition);
        // no engine outside workflow code (a handler, say): a plain loop there, as nothing replays it
        Engine engine = Engine.CURRENT.get();
        do {
            body.run();
        } while (engine == null ? condition.getAsBoolean() : engine.loopAgain(name, condition));
        // not in a finally: when the body throws, the mark of where it threw is still in the loop
        loop.leave();
    }

    /**
     * Waits until the condition, typically over variables a signal handler changes, holds; returns
     * whether it holds at the end of the wait.
     */
    public static boolean awaitCondition(BooleanSupplier condition, Duration timeout) {
        return Engine.current().awaitCondition(condition, timeout);
    }

    // ------------------------------------------------------------------ other workflows

    /** Runs a {@code @Workflow} method as a child workflow (SUB_WORKFLOW) and waits for its result. */
    @SuppressWarnings("unchecked")
    public static <T, A, R> R child(Fn1<T, A, R> workflow, A input) {
        Method method = MethodRefs.resolve(workflow);
        WorkflowSpec spec = WorkflowSpec.forMethod(method);
        Command command = new Command(Command.Kind.SUB_WORKFLOW, spec.name);
        command.fields.put("subWorkflowParam", Map.of("name", spec.name, "version", spec.version));
        command.input.putAll(WorkflowSpec.encodeInput(method, new Object[] {input}));
        command.resultType = method.getGenericReturnType();
        command.childMethod = method;
        command.childInput = input;
        return (R) Engine.current().call(command);
    }

    /** Starts a {@code @Workflow} method as an independent execution and does not wait. */
    public static <T, A, R> void startWorkflow(Fn1<T, A, R> workflow, A input) {
        Method method = MethodRefs.resolve(workflow);
        WorkflowSpec spec = WorkflowSpec.forMethod(method);
        startWorkflowCommand(spec.name, spec.version, WorkflowSpec.encodeInput(method, new Object[] {input}));
    }

    /** Starts any registered workflow by name and does not wait. */
    public static void startWorkflow(String name, int version, Map<String, ?> input) {
        Map<String, Object> plain = new LinkedHashMap<>();
        input.forEach((k, v) -> plain.put(k, Json.plain(v)));
        startWorkflowCommand(name, version, plain);
    }

    private static void startWorkflowCommand(String name, int version, Map<String, Object> input) {
        Command command = new Command(Command.Kind.START_WORKFLOW, "start_" + name);
        Map<String, Object> start = new LinkedHashMap<>();
        start.put("name", name);
        start.put("version", version);
        start.put("input", input);
        command.input.put("startWorkflow", start);
        Engine.current().call(command);
    }

    // ------------------------------------------------------------------ server steps

    public static HttpCall http(String ref) {
        return new HttpCall(ref);
    }

    public static HumanCall human(String ref) {
        return new HumanCall(ref);
    }

    /**
     * A wait any system ends by completing the task {@code ref}: {@code
     * external(ref).await(Type.class, timeout)}.
     */
    public static ExternalWait external(String ref) {
        return new ExternalWait(ref);
    }

    public static EventCall event(String ref) {
        return new EventCall(ref);
    }

    public static LlmCall llm(String ref) {
        return new LlmCall(ref);
    }

    /** Calls an MCP server's tools from the Conductor server. */
    public static McpCall mcp(String ref) {
        return new McpCall(ref);
    }

    /** Calls an AI agent (remote over A2A, or a Conductor agent) from the Conductor server. */
    public static AgentCall agent(String ref) {
        return new AgentCall(ref);
    }

    // ================================================================== client

    private final ConductorClient client;
    /** Read by starts and queries while other threads add workers: iterated as a snapshot. */
    private final List<Workers> started = new CopyOnWriteArrayList<>();
    private final Set<CompletableFuture<?>> waiting = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService poller;

    private Conductor(ConductorClient client) {
        this.client = client;
    }

    /**
     * Native workflows over a client. The client is not owned: {@link #close()} stops the workers
     * this instance started and leaves the client to its creator, so a shared or injected client
     * keeps working.
     */
    public static Conductor of(ConductorClient client) {
        return new Conductor(Objects.requireNonNull(client, "client"));
    }

    public ConductorClient client() {
        return client;
    }

    /**
     * Registers the definitions of these workflow classes, and their schedules, without starting
     * workers: for a deploy step that publishes what a build contains. {@link Workers#start()}
     * does the same on start-up. Each workflow's continuation lease is read as a host reads it,
     * from the system properties ({@code conductor.workflow.<name>.continuationTimeout}).
     */
    public void register(Class<?>... workflows) {
        List<WorkflowSpec> specs = new ArrayList<>();
        for (Class<?> type : workflows) {
            specs.addAll(WorkflowSpec.of(type));
        }
        Registration.register(client, specs, List.of(), Conductor::continuationTimeoutSeconds, true);
    }

    /**
     * The definition that would be registered for a workflow class: its discovered graph, input
     * parameters and native metadata, as JSON. For review, or to diff in a change.
     */
    public static List<Map<String, Object>> definitions(Class<?> workflow) {
        List<Map<String, Object>> defs = new ArrayList<>();
        for (WorkflowSpec spec : WorkflowSpec.of(workflow)) {
            defs.add(Registration.skeleton(spec, continuationTimeoutSeconds(spec), true));
        }
        return defs;
    }

    /** A workflow's continuation lease as a host started with the system properties has it. */
    private static long continuationTimeoutSeconds(WorkflowSpec spec) {
        return Workers.continuationTimeoutSeconds(System::getProperty, spec, Workers.CONTINUATION_TIMEOUT);
    }

    /**
     * Runs these workflow classes in this process: add task objects and options to what this
     * returns, then {@code start()} registers the definitions and starts polling. Each call starts
     * a separate set of workers; closing this Conductor stops them all.
     */
    public Workers workflows(Class<?>... workflowClasses) {
        return newWorkers().workflows(workflowClasses);
    }

    /** As {@link #workflows}, for a workflow class whose instances need constructor arguments. */
    public <T> Workers workflows(Class<T> workflowClass, Supplier<T> factory) {
        return newWorkers().workflows(workflowClass, factory);
    }

    /** As {@link #workflows}, for objects with {@code @WorkerTask} methods. */
    public Workers tasks(Object... taskObjects) {
        return newWorkers().tasks(taskObjects);
    }

    /** The spec of a workflow class these workers host, with its factory, if any of them does. */
    Optional<WorkflowSpec> hostedSpec(Class<?> type) {
        for (Workers workers : started) {
            Optional<WorkflowSpec> spec = workers.hostedSpec(type);
            if (spec.isPresent()) {
                return spec;
            }
        }
        return Optional.empty();
    }

    private Workers newWorkers() {
        Workers workers = new Workers(this);
        started.add(workers);
        return workers;
    }

    /** Starts an execution of a {@code @Workflow} method. */
    public <T, A, R> WorkflowHandle<R> start(Fn1<T, A, R> workflow, A input) {
        return workflow(workflow).start(input);
    }

    /** Start options: correlation id, idempotency key. */
    public <T, A, R> StartBuilder<A, R> workflow(Fn1<T, A, R> workflow) {
        return new StartBuilder<>(this, MethodRefs.resolve(workflow));
    }

    /** Starts an execution and waits for its result. */
    public <T, A, R> R execute(Fn1<T, A, R> workflow, A input, Duration timeout) {
        return start(workflow, input).result(timeout);
    }

    /** Attaches to an existing execution of a {@code @Workflow} method. */
    public <T, A, R> WorkflowHandle<R> attach(Fn1<T, A, R> workflow, String workflowId) {
        return new WorkflowHandle<>(this, workflowId, MethodRefs.resolve(workflow).getGenericReturnType());
    }

    /**
     * Stops the workers this instance started and cancels the result futures still waiting. The
     * client is left running: it is its creator's.
     */
    @Override
    public void close() {
        started.forEach(Workers::close);
        List.copyOf(waiting).forEach(future -> future.cancel(false));
        synchronized (this) {
            if (poller != null) {
                poller.shutdownNow();
            }
        }
    }

    /**
     * A future of an execution's outcome: {@code check} runs every 250 ms on this instance's one
     * polling thread and returns the execution once it has finished (null until then), and {@code
     * outcome} turns it into the result or throws. The future completes on the common pool, so a
     * dependent stage cannot hold up the polling of other executions; cancelling it stops the
     * checks.
     */
    <E, R> CompletableFuture<R> whenFinished(Supplier<E> check, Function<E, R> outcome) {
        CompletableFuture<R> future = new CompletableFuture<>();
        AtomicBoolean settled = new AtomicBoolean();
        Runnable tick = () -> {
            if (future.isDone() || settled.get()) {
                return;
            }
            Runnable complete;
            try {
                E execution = check.get();
                if (execution == null) {
                    return;
                }
                R result = outcome.apply(execution);
                complete = () -> future.complete(result);
            } catch (RuntimeException e) {
                complete = () -> future.completeExceptionally(e);
            }
            settled.set(true);
            ForkJoinPool.commonPool().execute(complete);
        };
        ScheduledFuture<?> checks = poller().scheduleWithFixedDelay(tick, 0, 250, TimeUnit.MILLISECONDS);
        waiting.add(future);
        future.whenComplete((result, error) -> {
            checks.cancel(false);
            waiting.remove(future);
        });
        return future;
    }

    private synchronized ScheduledExecutorService poller() {
        if (poller == null) {
            poller = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "conductor-results");
                thread.setDaemon(true);
                return thread;
            });
        }
        return poller;
    }

    /** Options for starting an execution. */
    public static final class StartBuilder<A, R> {
        private final Conductor conductor;
        private final Method method;
        private String correlationId;
        private String idempotencyKey;
        private Map<String, String> taskToDomain;
        private Integer priority;

        StartBuilder(Conductor conductor, Method method) {
            this.conductor = conductor;
            this.method = method;
        }

        /** An id of your own to find the execution by, such as an order id; not unique. */
        public StartBuilder<A, R> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /** A second start with the same key returns the execution the first one created. */
        public StartBuilder<A, R> idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        /**
         * Sends a task to workers polling in a domain, for reusing workers that already run
         * in one: {@code taskToDomain(Map.of("legacy_lookup", "team-a"))}.
         */
        public StartBuilder<A, R> taskToDomain(Map<String, String> taskToDomain) {
            this.taskToDomain = taskToDomain;
            return this;
        }

        /** Queue priority of the execution's tasks, 0 (lowest, the default) to 99. */
        public StartBuilder<A, R> priority(int priority) {
            if (priority < 0 || priority > 99) {
                throw new IllegalArgumentException("priority must be between 0 and 99");
            }
            this.priority = priority;
            return this;
        }

        /** Starts the execution with these options. */
        public WorkflowHandle<R> start(A input) {
            WorkflowSpec spec = WorkflowSpec.forMethod(method);
            // a host of this process registers in the background: a start right after it waits
            for (Workers host : conductor.started) {
                host.awaitRegistered(spec, Duration.ofSeconds(30));
            }
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("name", spec.name);
            request.put("version", spec.version);
            request.put("input", WorkflowSpec.encodeInput(method, new Object[] {input}));
            if (correlationId != null) {
                request.put("correlationId", correlationId);
            }
            if (priority != null) {
                request.put("priority", priority);
            }
            if (taskToDomain != null) {
                request.put("taskToDomain", taskToDomain);
            }
            if (idempotencyKey != null) {
                request.put("idempotencyKey", idempotencyKey);
                request.put("idempotencyStrategy", "RETURN_EXISTING");
            }
            // a start with an idempotency key can be repeated safely, so a server error or a lost
            // connection is retried: the server returns the execution the first attempt created if
            // it got that far. A start without a key is never retried, since a repeat could create
            // a second execution.
            String id = idempotencyKey == null
                    ? Http.post(conductor.client, "/workflow", request, String.class)
                    : Http.retrying(() -> Http.post(conductor.client, "/workflow", request, String.class));
            return new WorkflowHandle<>(conductor, id, method.getGenericReturnType());
        }
    }
}
