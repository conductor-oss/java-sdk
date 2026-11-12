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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Functions.Fn0;
import org.conductoross.conductor.sdk.Functions.Fn1;
import org.conductoross.conductor.sdk.Functions.Fn2;
import org.conductoross.conductor.sdk.Functions.Fn3;
import org.conductoross.conductor.sdk.Functions.Fn4;
import org.conductoross.conductor.sdk.Functions.Pred;
import org.conductoross.conductor.sdk.Functions.Proc0;
import org.conductoross.conductor.sdk.Functions.Proc1;
import org.conductoross.conductor.sdk.Functions.Proc2;
import org.conductoross.conductor.sdk.Functions.Proc3;
import org.conductoross.conductor.sdk.Functions.Proc4;
import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Conductors;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.TaskStubs;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * Native workflows for Conductor.
 *
 * <p>Inside a {@link Workflow} method, the static methods of this class are the workflow's calls
 * to the server: each one becomes a task in the execution's graph. Everything else in the method
 * is ordinary Java. Import them statically:
 *
 * <pre>{@code
 * import static org.conductoross.conductor.sdk.Conductor.*;
 *
 * @Workflow(name = "order", version = 1)
 * public OrderResult process(OrderRequest order) {
 *     Payment payment = task(PaymentTasks::charge, order.id(), order.total());
 *     if (payment.declined()) return OrderResult.declined();
 *     sleep(Duration.ofMinutes(5));
 *     return OrderResult.ok(payment);
 * }
 * }</pre>
 *
 * <p>Outside workflow code, an instance of this class runs native workflows over a {@link
 * ConductorClient}: create one with {@link #of(ConductorClient)}, host workflows and tasks with
 * {@link #host()}, and start executions with {@link #start}. The client is configured as for any
 * Conductor client (its builder, {@link ConductorClient#fromEnvironment()}, an Orkes {@code
 * ApiClient} with keys, or the Spring bean), and stays its creator's to shut down.
 *
 * <pre>{@code
 * ConductorClient client = ConductorClient.fromEnvironment();
 * try (Conductor conductor = Conductor.of(client)) {
 *     conductor.host().workflows(OrderWorkflow.class).tasks(new PaymentTasks()).start();
 *     OrderResult result = conductor.execute(OrderWorkflow::process, order, Duration.ofMinutes(1));
 * }
 * client.shutdown();
 * }</pre>
 *
 * <p>Rules for workflow code: anything non-deterministic or with a side effect that is not a task
 * goes through {@link #once}, {@link #now} or {@link #random}; blocking I/O belongs in tasks.
 */
public final class Conductor implements AutoCloseable {

    private static final Map<Method, WorkflowSpec> SPECS = new ConcurrentHashMap<>();

    // ================================================================== workflow code

    // ------------------------------------------------------------------ tasks by method reference

    public static <T, R> R task(Fn0<T, R> task) {
        return call(task);
    }

    public static <T, A, R> R task(Fn1<T, A, R> task, A a) {
        return call(task, a);
    }

    public static <T, A, B, R> R task(Fn2<T, A, B, R> task, A a, B b) {
        return call(task, a, b);
    }

    public static <T, A, B, C, R> R task(Fn3<T, A, B, C, R> task, A a, B b, C c) {
        return call(task, a, b, c);
    }

    public static <T, A, B, C, D, R> R task(Fn4<T, A, B, C, D, R> task, A a, B b, C c, D d) {
        return call(task, a, b, c, d);
    }

    public static <T> void task(Proc0<T> task) {
        call(task);
    }

    public static <T, A> void task(Proc1<T, A> task, A a) {
        call(task, a);
    }

    public static <T, A, B> void task(Proc2<T, A, B> task, A a, B b) {
        call(task, a, b);
    }

    public static <T, A, B, C> void task(Proc3<T, A, B, C> task, A a, B b, C c) {
        call(task, a, b, c);
    }

    public static <T, A, B, C, D> void task(Proc4<T, A, B, C, D> task, A a, B b, C c, D d) {
        call(task, a, b, c, d);
    }

    /**
     * A task stub: an instance of a class with {@code @WorkerTask} methods whose calls schedule
     * those tasks. Conductor passes one for each task-class parameter of a workflow's
     * constructor; this is for a factory given to {@code host().workflow(Type.class, factory)},
     * when the constructor also takes something else:
     *
     * <pre>{@code
     * host().workflow(OrderWorkflow.class, () -> new OrderWorkflow(Conductor.stub(OrderTasks.class), config))
     * }</pre>
     */
    public static <T> T stub(Class<T> taskClass) {
        return TaskStubs.stub(taskClass);
    }

    /** A task by name: for names known only at runtime, or tasks owned by another team. */
    public static TaskCall task(String name) {
        return new TaskCall(name);
    }

    @SuppressWarnings("unchecked")
    private static <R> R call(Serializable ref, Object... args) {
        return (R) Engine.current().call(taskCommand(ref, args));
    }

    /** The command for a call to a {@code @WorkerTask} method. */
    public static Command taskCommand(Serializable ref, Object... args) {
        return taskCommand(MethodRefs.resolve(ref), args);
    }

    /** The command for a call to a {@code @WorkerTask} method, by a reference or on a task stub. */
    public static Command taskCommand(Method method, Object[] args) {
        WorkerTask annotation = method.getAnnotation(WorkerTask.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    method.getDeclaringClass().getSimpleName() + "." + method.getName() + " is not a @WorkerTask method");
        }
        Command command = new Command(Command.Kind.SIMPLE, annotation.value(), annotation.value());
        String[] names = MethodRefs.parameterNames(method);
        for (int i = 0; i < names.length; i++) {
            command.input.put(names[i], Json.plain(args[i]));
        }
        command.resultType = method.getGenericReturnType();
        command.method = method;
        command.args = args;
        return command;
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
     * waiting for it. {@link #join} or {@link Promise#get} waits. A lane that is never joined still
     * runs, the workflow stays running until it finishes, and its failure does not fail the workflow.
     */
    public static <T> Promise<T> async(Supplier<T> body) {
        return Engine.current().async(body);
    }

    /** Waits for the given lanes and everything they started, and raises the first failure. */
    public static void join(Promise<?>... promises) {
        Engine.current().join(List.of(promises));
    }

    /** Runs the body once per item, in parallel; each item is a branch of a FORK_JOIN. */
    public static <I, R> Promise<List<R>> fanOut(String name, List<I> items, Function<I, R> body) {
        List<R> results = Engine.current().fanOut(name, new ArrayList<>(items), body);
        return new Promise<>() {
            @Override
            public List<R> get() {
                return results;
            }

            @Override
            public boolean isDone() {
                return true;
            }
        };
    }

    public static <I, R> Promise<List<R>> fanOut(List<I> items, Function<I, R> body) {
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
     *     .when("0", () -> { task(Tasks::even); })
     *     .when("1", () -> { task(Tasks::odd); })
     *     .otherwise(() -> notes.add("unexpected"))
     *     .run();
     *
     * String carrier = switchOn("region", region)
     *     .when("eu", () -> task(Tasks::euCarrier, item))
     *     .otherwise(() -> task(Tasks::hold, item))
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
        private final java.util.LinkedHashMap<String, Supplier<?>> cases = new java.util.LinkedHashMap<>();
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
            return when(key, new org.conductoross.conductor.sdk.internal.ActionCase<T>(body));
        }

        /** The body when no case matches; without it an unmatched value fails the workflow. */
        public Switch<T> otherwise(Functions.CaseBody<? extends T> body) {
            this.otherwise = body;
            return this;
        }

        public Switch<T> otherwise(Functions.CaseAction body) {
            return otherwise(new org.conductoross.conductor.sdk.internal.ActionCase<T>(body));
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
        Command wait = new Command(Command.Kind.WAIT, "sleep", "sleep");
        wait.input.put("duration", Conductors.duration(duration));
        wait.resultType = Void.class;
        wait.seconds = duration.toSeconds();
        Engine.current().call(wait);
    }

    /**
     * Waits on the server until the given instant (a WAIT task with {@code until}). The server
     * resolves {@code until} to the minute, so the wait ends at the start of the minute after
     * {@code when} at the latest.
     */
    public static void sleepUntil(Instant when) {
        Instant minute = when.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        if (minute.isBefore(when)) {
            minute = minute.plus(java.time.Duration.ofMinutes(1));
        }
        String until = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(java.time.ZoneOffset.UTC)
                .format(minute);
        Command wait = new Command(Command.Kind.WAIT, "sleep_until", "sleep_until");
        wait.input.put("until", until);
        wait.resultType = Void.class;
        Engine.current().call(wait);
    }

    /**
     * Runs a task once per item, one after another, on the server (a DO_WHILE over the items),
     * and returns the results in order. The code is woken once, when all items are done. The
     * task method takes the item as its only parameter. For items in parallel, use {@link
     * #fanOut}.
     */
    @SuppressWarnings("unchecked")
    public static <T, I, R> List<R> forEach(String name, List<I> items, Fn1<T, I, R> task) {
        Command command = taskCommand(task, new Object[] {null});
        String itemParameter = command.input.keySet().iterator().next();
        command.input.clear();
        return (List<R>) (List<?>) Engine.current().forEach(name, new ArrayList<>(items), command, itemParameter);
    }

    public static <T, I, R> List<R> forEach(List<I> items, Fn1<T, I, R> task) {
        return forEach("for_each", items, task);
    }

    /**
     * The same, naming the task through a stub, as the rest of the workflow calls it:
     * {@code forEach("labels", items, tasks::labelItem)}. The stub is not called; the server runs
     * the task once per item.
     */
    @SuppressWarnings("unchecked")
    public static <I, R> List<R> forEach(String name, List<I> items, Functions.TaskOf<I, R> task) {
        Command command = taskCommand(task, new Object[] {null});
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
        WorkflowSpec spec = spec(method);
        Command command = new Command(Command.Kind.SUB_WORKFLOW, spec.name, spec.name);
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
        WorkflowSpec spec = spec(method);
        startWorkflowCommand(spec.name, spec.version, WorkflowSpec.encodeInput(method, new Object[] {input}), method, input);
    }

    /** Starts any registered workflow by name and does not wait. */
    public static void startWorkflow(String name, int version, Map<String, ?> input) {
        Map<String, Object> plain = new LinkedHashMap<>();
        input.forEach((k, v) -> plain.put(k, Json.plain(v)));
        startWorkflowCommand(name, version, plain, null, null);
    }

    private static void startWorkflowCommand(String name, int version, Map<String, Object> input, Method method, Object childInput) {
        Command command = new Command(Command.Kind.START_WORKFLOW, "start_" + name, "start_" + name);
        Map<String, Object> start = new LinkedHashMap<>();
        start.put("name", name);
        start.put("version", version);
        start.put("input", input);
        command.input.put("startWorkflow", start);
        command.childMethod = method;
        command.childInput = childInput;
        Engine.current().fire(command);
    }

    // ------------------------------------------------------------------ server steps

    public static HttpCall http(String ref) {
        return new HttpCall(ref);
    }

    public static HumanCall human(String ref) {
        return new HumanCall(ref);
    }

    public static <T> ExternalWait<T> awaitExternal(String ref, Class<T> type) {
        return new ExternalWait<>(ref, type);
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

    public static WorkflowSpec spec(Method method) {
        return SPECS.computeIfAbsent(method, WorkflowSpec::of);
    }

    // ================================================================== client

    private final ConductorClient client;
    private final List<Workers> started = new ArrayList<>();

    private Conductor(ConductorClient client) {
        this.client = client;
    }

    /**
     * Native workflows over a client. The client is not owned: {@link #close()} stops the workers
     * this instance started and leaves the client to its creator, so a shared or injected client
     * keeps working.
     */
    public static Conductor of(ConductorClient client) {
        return new Conductor(java.util.Objects.requireNonNull(client, "client"));
    }

    public ConductorClient client() {
        return client;
    }

    /** Hosts workflow classes and task classes in this process. */
    /**
     * Registers the definitions of these workflow classes (and the task definitions of these
     * task objects, if any) without starting workers: for a deploy step that publishes what a
     * build contains. {@link Workers#start()} does the same on start-up.
     */
    public void register(Class<?>... workflows) {
        List<org.conductoross.conductor.sdk.internal.WorkflowSpec> specs = new java.util.ArrayList<>();
        for (Class<?> type : workflows) {
            specs.addAll(org.conductoross.conductor.sdk.internal.WorkflowSpec.of(type));
        }
        Registration.register(client, specs, List.of(), 30);
    }

    /**
     * The definition that would be registered for a workflow class: its discovered graph, input
     * parameters and native metadata, as JSON. For review, or to diff in a change.
     */
    public static List<Map<String, Object>> definitions(Class<?> workflow) {
        List<Map<String, Object>> defs = new java.util.ArrayList<>();
        for (org.conductoross.conductor.sdk.internal.WorkflowSpec spec : org.conductoross.conductor.sdk.internal.WorkflowSpec.of(workflow)) {
            defs.add(Registration.skeleton(spec, 30));
        }
        return defs;
    }

    /**
     * A new set of workflows and tasks to host in this process: add them, then {@code start()}.
     * Each call returns a new set; closing this Conductor stops them all.
     */
    public Workers host() {
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

    /** Stops the workers this instance started. The client is left running: it is its creator's. */
    @Override
    public void close() {
        started.forEach(Workers::shutdown);
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

        public StartBuilder<A, R> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /** A second start with the same key returns the execution the first one created. */
        public StartBuilder<A, R> idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        /** Queue priority of the execution's tasks, 0 (lowest, the default) to 99. */
        /**
         * Sends a task to workers polling in a domain, for reusing workers that already run
         * in one: {@code taskToDomain(Map.of("legacy_lookup", "team-a"))}.
         */
        public StartBuilder<A, R> taskToDomain(Map<String, String> taskToDomain) {
            this.taskToDomain = taskToDomain;
            return this;
        }

        public StartBuilder<A, R> priority(int priority) {
            if (priority < 0 || priority > 99) {
                throw new IllegalArgumentException("priority must be between 0 and 99");
            }
            this.priority = priority;
            return this;
        }

        public WorkflowHandle<R> start(A input) {
            WorkflowSpec spec = spec(method);
            // a host of this process registers in the background: a start right after it waits
            for (Workers host : conductor.started) {
                host.awaitRegistered(spec, java.time.Duration.ofSeconds(30));
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
            String id = idempotencyKey == null
                    ? Http.post(conductor.client, "/workflow", request, String.class)
                    : postIdempotent(request);
            return new WorkflowHandle<>(conductor, id, method.getGenericReturnType());
        }

        /**
         * A start with an idempotency key can be repeated safely, so a server error or a lost
         * connection is retried for up to two minutes; the server returns the execution the first
         * attempt created if it got that far. A refusal (4xx) is not retried. A start without a key
         * is never retried, since a repeat could create a second execution.
         */
        private String postIdempotent(Map<String, Object> request) {
            long deadline = System.currentTimeMillis() + java.time.Duration.ofMinutes(2).toMillis();
            while (true) {
                try {
                    return Http.post(conductor.client, "/workflow", request, String.class);
                } catch (RuntimeException e) {
                    boolean refused = e instanceof io.orkes.conductor.client.http.ApiException api
                            && api.getStatusCode() >= 400 && api.getStatusCode() < 500;
                    if (refused || System.currentTimeMillis() > deadline) {
                        throw e;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
    }
}
