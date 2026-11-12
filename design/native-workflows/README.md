# Native workflows

Status: implemented. The SDK lives in `conductor-client` under `org.conductoross.conductor.sdk`
(public API), `.internal` (replay engine, host internals) and `.testing` (in-memory test
environment and replay check). The runnable kitchensink is the `native-examples` module. The
server side is on `conductor-oss/conductor` branch `design/native-workflows`; its
`proposals/native-workflows/06-implementation.md` lists what was validated and where the
implementation differs from this design (fan-out lanes instead of dynamic-fork stages, a
stateless always-replaying worker, `startWorkflow` returning nothing, and the embedded test tier
and debugger not yet built).

Workflows written as ordinary Java. Any method on any class becomes a workflow with `@Workflow`.
Tasks are today's `@WorkerTask` classes, unchanged. A workflow takes its task classes as
constructor parameters and calls them directly (`tasks.reserve(order)`), or calls them by method
reference (`task(OrderTasks::reserve, order)`); the two schedule the same task. Real `if`,
`switch`, loops, `try`/`catch`, local variables and helper methods. No base class, no interface,
no compiler, no annotation processor. The SDK uses `SerializedLambda` to resolve method
references, reflection on the referenced methods for input and output types, `StackWalker` for
call sites, ASM to read compiled code when it draws the graph, and ByteBuddy for task stubs. ASM
and ByteBuddy are bundled inside the `conductor-client` jar under
`org.conductoross.conductor.shaded`, not dependencies of it, so an application's own versions
never clash with them.

**Task stubs.** The SDK creates the workflow object, a new one for each replay, and passes a stub
for each constructor parameter whose class has `@WorkerTask` methods. A call on the stub schedules
the task (on replay it returns the recorded result), as `task(...)` does; calling a method that is
not a `@WorkerTask` method throws. The stub is a generated subclass, created without running the
task class's constructor, so a task class with dependencies needs nothing extra; a final task
class or task method fails with a clear message. A real task object in the workflow would run its
methods inside the workflow on every replay, so it is rejected: a field holding one fails when the
workflow is created, and `new` of a task class in workflow code fails when the code is read.
Outside Spring a workflow constructor may take only task classes; anything else needs
`host().workflow(Type.class, factory)`, whose factory gets the task stubs from
`Conductor.stub(OrderTasks.class)`. In Spring other parameters are injected.

**The one sentence:** every static call imported from `Conductor` goes to the server and shows in
the graph; everything else is just Java.

## Files

| File | What it is |
|---|---|
| `native-examples/src/main/java/kitchensink/KitchenSinkWorkflow.java` | a plain class whose `@Workflow` method mirrors `rest/src/main/resources/kitchensink/kitchensink.json` in section 1, and adds timers, loops, signals, updates, queries, human tasks, callbacks, compensation and LLM in section 2; its decisions are plain `if` and `switch`, its loops plain `for` and `while` |
| `native-examples/src/main/java/kitchensink/SubFlow.java` | `sub_flow_1` as a `@Workflow` method, called as a typed child |
| `native-examples/src/main/java/kitchensink/KitchenSinkTasks.java` | the tasks: today's `@WorkerTask` class, unchanged |
| `native-examples/src/main/java/kitchensink/KitchenSinkModel.java` | input, output and payload records |
| `native-examples/src/main/java/kitchensink/Main.java` | host and client through one `Conductor` entry point |
| `native-examples/src/main/java/conformance/Conformance.java` | 151 scenarios against a server, each with an expected result and graph; exits non-zero on any failure |
| `native-examples/src/main/java/switches/Switches.java` | `switchOn` edge cases against a server: a case with tasks, a nested switch, an empty case, the default case, and a value no case handles (the workflow fails) |
| `native-examples/src/main/java/failures/Failures.java` | an unchecked exception from workflow code: thrown by the workflow method it fails the workflow with the exception as the reason; thrown in an `async` lane nothing joins, the workflow completes and the exception is in the log of the task the lane ran last |
| `native-examples/src/test/java/kitchensink/KitchenSinkTest.java` | unit tests with a mocked task class and the in-memory environment, plus a replay test |

## Round 4: what changed and why

| Before | Now | Why |
|---|---|---|
| `class X extends DurableWorkflow<I, O>` with an abstract `run` | `@Workflow` on any method of any class | the annotation is the whole contract; nothing to extend or implement |
| `ActivityInterface` + `ActivityImpl`, stub by proxy | one `@WorkerTask` class, taken as a constructor parameter and called directly, `tasks.task1(mod, oddEven)`, or by method reference, `task(KitchenSinkTasks::task1, mod, oddEven)` | today's worker model, no interface to write; the SDK generates the stub, the compiler checks the arguments, reflection on the method gives the types at runtime |
| "activities" | "tasks" everywhere: `task(...)`, `TaskFailure`, `.tasks(...)` on the host | Conductor's term |
| inherited methods | static methods on `Conductor`, normally static-imported so calls read bare | no base class to occupy the inheritance slot |
| all fields durable, `transient` opts out | `@Variable` fields are workflow variables; other fields are ordinary | a service class has injected clients and caches; those must never be persisted, and `@Variable` maps to `workflow.variables` by name |
| `run(name, () -> ...)` | `once(name, () -> ...)` | `run` collided with the natural name for a workflow method |
| generics on the workflow class | generics only where Java infers them: `Promise<T>`, `WorkflowHandle<R>`, `Optional<T>` | erasure never matters because the runtime takes every type from the referenced method, not from a type parameter |
| `switchOn(...)` for a decision the graph should show; a plain `if` or `switch` left no trace | a plain `if` or `switch` whose arms call the server is a SWITCH with every arm, read from the compiled code; `switchOn` stays for a name and keys of your choosing | workflow code reads as ordinary Java, and the graph still shows every branch and which one was taken |

## How it reads

```java
public class OrderService {

    private final InventoryTasks inventory;   // stubs from Conductor: calls schedule the tasks
    private final PaymentTasks payments;

    public OrderService(InventoryTasks inventory, PaymentTasks payments) {
        this.inventory = inventory;
        this.payments = payments;
    }

    @Variable private String stage = "new";

    @Signal("cancel")
    public void cancel(CancelRequest r) { stage = "cancelled"; }

    @Workflow(name = "order", version = 1)
    public OrderResult processOrder(OrderRequest order) {
        Promise<FraudResult> fraud = async(() -> http("fraud").post(FRAUD_URL).body(order).as(FraudResult.class));
        Promise<List<Reservation>> reserved = fanOut(order.items(), item -> inventory.reserve(item));
        join(fraud, reserved);
        if (fraud.get().flagged()) return OrderResult.rejected();

        Payment payment = switch (order.paymentMethod()) {
            case CARD    -> payments.chargeCard(order);
            case INVOICE -> payments.issueInvoice(order,
                                 human("approval").assignee("finance").await(Approval.class, Duration.ofDays(2)).orElseThrow());
        };
        sleep(Duration.ofMinutes(5));
        return OrderResult.ok(payment);
    }
}
```

`InventoryTasks.reserve` and `PaymentTasks.chargeCard` are `@WorkerTask` methods, exactly as a
worker is written today. The workflow takes the task classes as constructor parameters and gets
task stubs, so each call schedules the task (see Task stubs above). The workflow has a compile-time dependency on the task classes, which is
what makes the arguments type-checked; a task owned by another team or written in another language
is called by name with `task("name").input(...).as(T)`, or through a typed interface of
`@WorkerTask` methods that nothing in this process implements or hosts (`reuse/Reuse.java`). A
worker that polls in a domain is reached with `.taskToDomain(...)` on the start builder. A task
definition owned by someone else is never overwritten at registration, so its retries and timeouts
stay its owner's.

If deployed code stops matching a running execution, the execution is held, not failed: it stays
running, shows why on the execution page, and continues when a compatible build is running. Graph
discovery and switch previews read the compiled code and run nothing; `workers.discovery(false)`
leaves the graph out of the registered definition.

The `switch` on the payment method is plain Java, and because its arms call the server the
execution shows it as a SWITCH, `switch_paymentMethod`: every arm, the one taken, and what the
others would have called (see Plain ifs and switches below). The fraud check returns without a
call, so it is plain code and shows nothing. `switchOn` is the same SWITCH with a name and case
keys of your choosing, and the value itself recorded:

```java
Payment payment = switchOn("payment", order.paymentMethod())
        .when(CARD,    () -> payments.chargeCard(order))
        .when(INVOICE, () -> payments.issueInvoice(order,
                             human("approval").assignee("finance").await(Approval.class, Duration.ofDays(2)).orElseThrow()))
        .get();
```

## The API, in one table

Static methods on `org.conductoross.conductor.sdk.Conductor`.

| Call in workflow code | Server task | Wakes the code |
|---|---|---|
| `tasks.method(args...)` on a task stub, or `task(Tasks::method, args...)` | SIMPLE, named by `@WorkerTask`; retried per its definition (3 retries, exponential backoff by default), then marked optional so a final failure reaches the code | when the task completes, or fails after its retries |
| `task(name).input(...).as(T)` | SIMPLE, name chosen at runtime | when the task completes |
| `once(name, () -> ...)` | none; the value is recorded (`recorded`) on the continuation marker of the task the code pauses on next | never; runs in place |
| an `if` or `switch` whose arms call the server | SWITCH named after what it decides on (`if_flagged`, `switch_paymentMethod`; `value-param` on `switchCaseValue`). An `if` has the case `true` and its else as the default case; a `switch` has a case per arm, keyed by its labels (`CARD`, `odd`, `even,zero`), and `default`. The arm taken holds the calls it made; the others show what they would call, as previews. Read from the compiled code; see Plain ifs and switches | never by itself; the arm's calls wake the code as usual |
| a Java `for`, `while` or `do`-`while` | ordinary code, so it may hold ifs, switches, forks, sub workflows and other loops. There is no `DO_WHILE` task: the compiled method says which lines a loop covers, each step is tagged with the loops around it in `metadata.loops`, and the execution diagram draws those steps as one loop | at each call inside it |
| `async(() -> ...)`, `join(p1, p2)`, `Promise.get()` | FORK_JOIN + JOIN, one branch per async block that calls the server, each with its own continuation points; a block that returns without a server call is plain Java. `async` does not wait: lanes still running at the next durable call get a JOIN that completes at once, and a later `join` or `get` adds a JOIN naming those lanes. It works inside another `async` block, and a join waits for what the lane started | each branch when its own task completes |
| `fanOut(items, item -> ...)` | FORK_JOIN with one branch per item + JOIN; drawn as FORK_JOIN_DYNAMIC + JOIN in an untaken case or the registered definition, where the items are not known | once |
| `child(Other::method, input)` / `childAsync(...)` | SUB_WORKFLOW, typed by the referenced `@Workflow` method | when the child completes |
| `startWorkflow(name, version, input)` / `startWorkflow(Other::method, input)` | START_WORKFLOW | never (fire and forget); a failure shows in the UI only |
| `sleep(duration)` | WAIT with duration | when the timer fires |
| `repeatUntil(body, condition, backoff)` | DO_WHILE { WAIT, task }; the first check runs after one interval | once, when the loop ends |
| `sleepUntil(instant)` | WAIT with `until`; the server resolves it to the minute, so the instant is rounded up to the next minute | when the time is reached |
| `forEach(name, items, tasks::method)` on a task stub, or `forEach(name, items, Tasks::method)` | DO_WHILE over `items`, one task per item, one after another; the method takes the item as its only parameter; the stub is not called | once, when every item is done |
| `mcp(ref).server(url).listTools()` / `.call(tool, args)` | LIST_MCP_TOOLS / CALL_MCP_TOOL, run by the server's MCP client; retried like HTTP (`.retries(count, delay)`) | when the call completes |
| `agent(ref).remote(cardUrl).ask(text)` / `agent(ref).conductor(name, version).ask(text)` | AGENT: an A2A call to a remote agent (an agent card URL is discovered), or a Conductor agent | when the agent replies |
| `awaitExternal(ref, T).await(timeout)` | WAIT completed by `POST /api/tasks/{workflowId}/{ref}/COMPLETED` | when completed or timed out |
| `awaitCondition(() -> ..., timeout)` | WAIT completed by the handler lane | when the condition holds or times out |
| `human(ref)...await(T, timeout)` | HUMAN with a retry-style timeout policy, so a timeout is an empty result, not a failed workflow | when answered or timed out |
| `http(ref).get/post(...).as(T)` / `.asJson()` | HTTP | when the call completes |
| `event(ref).to(sink).send(payload)` | EVENT | never |
| `llm(ref).model(...).prompt(...).as(T)` | LLM_CHAT_COMPLETE | when the call completes |
| `switchOn(name, value).when(key, body)…otherwise(body).get()` / `.run()` | SWITCH named `name` (`evaluatorType: value-param`, `switchCaseValue` the value's string form), with every declared case as a branch and a default branch: the taken case's body runs inline as ordinary workflow code and its calls land inside that branch, in the fragment or after the task the code paused on there; each untaken case shows a preview of the calls its body would have made (`<task>_if_<key>` entries, never run); the first call after the body is anchored after the switch (`metadata.after`). The server records `selectedCase` in the task's output and the UI marks that branch. A key is compared with the value's string form; declaring a key twice is an error at the call | never by itself; the body's calls wake the code as usual |
| `now()`, `random()` | none; recorded with the continuation | never |
| `@Variable` fields | workflow variables, written by every continuation's completion (only what changed); readable as `workflow.variables` | never |
| `@Signal`, `@Update` methods | handler lane: no task; a marker on the execution (`nativeState.handlers`) that each message wakes, and whose poll hands the worker the claimed messages; each message applied once (the client's message id is de-duplicated through `nativeState.seen`); update replies in `nativeState.replies`, kept 10 minutes, at most 500 | handler runs on a worker between continuations |
| `@Query` methods | none: the client restores an instance from the execution's `@Variable` values and runs the method itself | never; works on completed executions, writes nothing, and any change the method makes is discarded. It sees messages the execution has applied, not ones still queued |
| `return value` / `throw WorkflowFailedException` | workflow output / FAILED with reason | |
| any other unchecked exception the workflow method throws | FAILED, with `<SimpleName>: <message>` as the reason; tasks that ran stay as they are, and a FAILED task named after the exception (a `TERMINATE` entry the code appends, never scheduled) marks where it threw, with that line as its site, inside the if or switch arm the code was in; what the code would have called next, had it not thrown, follows as previews (`task2_if_no_error`), read from the compiled code. An uncaught `TaskFailure` shows its own task FAILED instead. A retry removes the mark and runs the code again from its last continuation, so a fixed build continues the execution. In an `async` lane it is raised where the code waits for the lane (`join`, `get`); a lane nothing waits for does not fail the workflow, and the worker writes the exception to the log of the task the lane ran last | |
| `catch (TaskFailure e)` | none; the task stays FAILED in the UI, the lane continues | |

Plain Java control flow costs nothing: a branch or loop that does not call the server is invisible
to it. Seeing every branch and which one was taken is a core Conductor capability, so an `if` or
`switch` whose arms call the server is a real SWITCH task with all its arms, and the arms not taken
show what they would have run (see Plain ifs and switches under "How an execution runs").
`switchOn` is the explicit form: a name and case keys of your choosing, the value recorded as
`switchCaseValue`, and a value no case handles runs `otherwise`, or fails the workflow with
`switch <name> has no case for <value>` when there is none. On replay either kind must take the arm
it recorded; another is a divergence ("took case X in the recorded execution but the code now
takes Y").

Input and output mapping: a `@Workflow` method with one record parameter takes the record's fields
as the workflow input, which is how the JSON kitchensink's `{"mod", "oddEven", "task2Name"}` maps
onto `KitchenSinkInput`. Several parameters map by name. The return value is the workflow output.
Task methods map the same way. Parameter names need `-parameters` or `@InputParam`, as today, and
registration fails fast with that message when neither is present.

Method references and overloads: a `@WorkerTask` or `@Workflow` method that is overloaded cannot be
referenced unambiguously, and the compiler says so. Do not overload them.

### Workflow options

| Option | Where | Server behaviour |
|---|---|---|
| failure workflow | `@Workflow(failureWorkflow = "name", failureWorkflowVersion = n)` | started when the execution fails or times out, with `workflowId`, `reason` and `failureStatus` in its input; the failed execution's output gets `conductor.failure_workflow` |
| schedule | `@Schedule(cron = "0 0 9 * * MON-FRI", zone = "UTC", input = "{}")` on the `@Workflow` method, repeatable | registered with the Conductor scheduler (`POST /api/scheduler/schedules`) when the workers start; needs `conductor.scheduler.enabled=true`. Registration overwrites the schedule, including `paused`, so a schedule paused in the UI is resumed by the next worker start unless the annotation says `paused = true` |
| priority | `conductor.workflow(X::run).priority(5).start(input)` | the execution's priority (0 to 99), used to order its tasks in the queues |
| continuation timeout | `conductor.host().continuationTimeout(Duration.ofSeconds(30))` | the continuation lease (`metadata.native.continuationTimeoutSeconds`): how long a continuation handed to a worker may go unanswered before the server hands it to another worker; 30 s by default, an hour in dev mode; must exceed the longest replay |

## The developer's day

### First five minutes

```bash
conductor server start                      # local server and UI, as today
conductor init java order-service           # scaffolds a Maven project: one workflow, one task class, one test
cd order-service && ./mvnw -q compile exec:java
```

- `Conductor.of(client)` is the single entry point, over any `ConductorClient`: one from its
  builder, `ConductorClient.fromEnvironment()` (reads `CONDUCTOR_SERVER_URL` and
  `CONDUCTOR_AUTH_KEY`/`CONDUCTOR_AUTH_SECRET`, falling back to the local server), an Orkes
  `ApiClient` with keys, or the Spring bean. `conductor.host()...start()` hosts,
  `conductor.start(Cls::method, input)` runs. Closing a `Conductor` stops its workers; the client
  stays its creator's to shut down.
- A server that is down never fails the application. `start()` returns at once; the definitions
  are registered in the background, retried for as long as it takes with waits of 1, 2, 4...
  seconds up to a minute (randomised, so that many hosts do not retry in step), and each failed
  attempt is logged as an error. Polling backs off the same way. Only a fault in the code, such as
  a workflow class that cannot be created, fails `start()`.
- Spring Boot: the starter auto-configures a `Conductor` bean over the configured `ConductorClient`
  bean (the Orkes `ApiClient`, with keys, when one is configured), so it can be injected, and
  hosts the application's workflows with no setup code: every class with a `@Workflow` method
  under the application's packages, found as JPA entities are. A workflow class need not be a
  bean; each replay gets a new instance, with task stubs (below) for its task-class constructor
  parameters and other parameters and `@Autowired` fields injected from the context.
  `@WorkerTask` beans stay on the starter's worker executor (one poller per task); the starter also registers their task definitions. Hosting
  starts when the application has started and stops before the context closes;
  `conductor.workflows.enabled=false` turns it off in a service that only starts workflows.
  Example: `native-examples-spring`. Planned: `spring-boot-devtools` hot restart with in-flight
  executions surviving it. The build sets `-parameters`.
- Existing users: a `@WorkerTask` class is already a task class. Add a `@Workflow` method that
  calls it. Nothing else changes.

### Writing

- Names are optional everywhere except `once()`. Tasks are named by `@WorkerTask`, server steps by
  call site. The `.ref("task_2")` and `"fanout1"` in the kitchensink exist only to match the JSON.
- `@Workflow` without arguments names the workflow after the method in snake case, version 1.
- Records in, records out. The SDK's Jackson is preconfigured for records, `Optional`, `java.time`
  and enums.
- Execution metadata is static too: `workflowId()`, `correlationId()`, `attempt()`.
- Logging: SLF4J works as is; the SDK sets `workflowId` and `taskId` in the MDC on workflow and
  task threads.

### Running and debugging

- Breakpoints just work: the code runs on a plain thread while a continuation is processed.
  `devMode()` stretches the continuation lease to an hour so a pause at a breakpoint is not
  mistaken for a dead worker, scales timers down, and logs every pause with its source line and
  the UI link.
- Attach to any execution: `DurableDebugger.attach(KitchenSinkWorkflow.class, workflowId)` re-runs
  the class in the IDE against the recorded results of a real execution, local or production, with
  breakpoints and the real data. The CLI prints the invocation: `conductor workflow debug <id>`.
- Stack traces point at your code; SDK frames are stripped. `TaskFailure` carries the worker's
  exception class, message and stack.
- Every task links to source: the call site shows in the UI as `KitchenSinkWorkflow.kitchenSink:98`.
- Change code and keep going: restart the worker, in-flight executions continue by name matching.

### Testing

- Tier 1, in memory: `DurableTestEnvironment.inMemory()` runs the workflow code in process and
  simulates server steps. The task class is a Mockito mock, and `tasks.task1(...)`
  in the workflow lands on it. Milliseconds per test. `KitchenSinkTest` shows it.
- Tier 2, embedded: same API on the real server in the same JVM, in-memory persistence, timers
  scaled with the WAIT time-scale knob.
- The graph is assertable: `run.ran("forkx")`, `run.tasks("sub_flow_1")`, `run.task("charge").input()`;
  a `switchOn` counts as ran under its name (`run.ran("route")`), and in memory
  (`DirectEngine.switchOn`) the taken body simply runs; a plain `if` or `switch` is code there,
  not a step.
- The definition is assertable too, with no server: `Conductor.definitions(Fulfilment.class)`
  returns the JSON that would be registered (`DiscoveryTest` checks the task names, the fork
  branches, the switch cases, `inputParameters` and the handler inventory).
- Replay tests: `DurableReplay.assertCompatible(cls, recordedExecution)` re-runs current code
  against an execution exported with `conductor workflow export <id>` and reports divergence.
- Task classes are plain classes; test them with nothing.

### Deploying

- Continuations are fully distributed by default: any machine polling the shared queue can run
  any of them. Build pinning is opt-in: `buildId(...)` on the host adds that build as a polled
  domain, and a start names it with a fallback, `conductor.workflow(X::run).taskToDomain(Map.of("<name>.v1.code",
  "build-7,NO_DOMAIN"))`. The first domain with a poller in the last 10 s wins, else the last
  entry, `NO_DOMAIN` meaning the shared queue, so when old workers are gone name matching continues
  the run on the new code. A worker's build is never captured automatically. The `*` key also
  applies to ordinary tasks, and only workers polling that domain receive them; the SDK's task
  workers poll the shared queue, so name the code type, or end a `*` list with `NO_DOMAIN`.
- Definitions in GitOps: `start()` registers definitions from the annotations by default;
  `conductor.register(OrderService.class, …)` registers them without starting workers, for a
  deploy step, and the static `Conductor.definitions(OrderService.class)` returns the same JSON
  for review or to diff in a change.
- What gets registered is a real definition, not an empty one. `Registration.skeleton` calls
  `Replayer.discover`, which reads the compiled workflow method (`CodeReader`, on ASM) and runs
  nothing. Every call becomes a definition entry under the plain reference name an execution would
  allocate: switches with all their cases (`switchCaseValue: ""`), plain ifs and switches whose
  arms call the server the same way, `async` blocks as FORK_JOIN +
  JOIN when the code joins them and as a fork with a non-waiting JOIN when it does not, a later
  `join` as a JOIN of its own, `fanOut` as FORK_JOIN_DYNAMIC + JOIN, `repeatUntil` and `forEach` as DO_WHILE, `sleep` and
  `awaitCondition` as WAIT, child, HTTP, event, LLM, MCP and agent entries; no `metadata.preview`
  marks. Every SIMPLE entry gets an inline `taskDefinition` so registration validates without a
  registered task definition. The definition also carries `inputParameters` (the components of a
  single record parameter, or the parameter names) and, under `metadata.native`, the handler
  inventory (`signals`, `updates`, `queries`) and `discovered` (`false` with a `discoveryError` when
  the code could not be read). It shows every call the code can make, not one path: every arm of
  an `if` or `switch`, a loop body once, and a catch block after its try.
  Reading follows lambdas and methods of the workflow's own package. Inputs are not read beyond
  constant reference names, case keys, an HTTP method and URI and an event sink; a task name
  computed at run time shows as `task`; a call through an interface or into another package is not
  followed.
- Migration: `conductor workflow convert --java <name>` generates a `@Workflow` method from an
  existing JSON definition. Existing workers and JSON sub-workflows keep working, since task names
  are task names and `child("legacy_flow", 2, input, Out.class)` calls any registered definition.

### Errors a developer will actually hit

| Situation | What happens |
|---|---|
| Parameter names missing (no `-parameters`) | registration fails with the flag to add, or `@InputParam("name")` |
| A `@Variable` field is not serializable | registration fails naming the field |
| Method reference to an overloaded task method | compile error (ambiguous reference) |
| No worker in this process for a referenced task | warning at start: tasks will wait for another worker |
| `once()` lambda throws | the exception is raised in the code at once, without retries; uncaught, the workflow fails with it |
| A task fails and nothing catches `TaskFailure` | the workflow fails with that reason; the task shows FAILED, the failure is one continuation later than a JSON workflow's |
| A task fails inside an `async` lane and the lane does not catch it | `join(...)` and `get()` raise it in the caller |
| An `async` block makes no server call | allowed; it is plain Java and never appears in the graph |
| Replay diverges | the execution is held with a message naming the call and its source line, and retried with backoff until a compatible build runs it |
| `switchOn` value matches no case and there is no `otherwise` | the SWITCH is drawn with every case, then `WorkflowFailedException` at the call: the workflow fails with `switch <name> has no case for <value>` |
| A task is called before `async` lanes are joined | both run; the code goes on and the lanes run on their own |
| The method returns with lanes never joined | they still run: the execution stays `RUNNING` until they finish |
| A lane nothing joins fails | the execution is not failed; the failure shows on the lane's task |
| A `switchOn` case key is declared twice | `IllegalArgumentException` at the call (`case <key> of <name> is declared twice`) |
| The code now takes a different `switchOn` case than the recorded execution did | a divergence: the execution is held, naming both cases |
| Two workers hold the same continuation | only one holds the lease; the other's completion is refused (409) and that worker stops |

## How an execution runs

**Lanes and continuation markers.** The `@Workflow` method is a lane. Each `async` block is
another lane. A lane pauses when it needs a result from the server, and the pause is not a task:
the last entry the lane scheduled, the task it waits on, carries `metadata.continuation = {lane,
n}`, and the server copies it onto the task as a continuation marker. Everything the lane
scheduled since its last pause is sent as the completion of its previous continuation, and the
server appends it right after the task that lane last paused on: at the top level, inside a fork
branch, or inside a switch case. An execution holds exactly the tasks the code called. Lanes never
wait for each other except at `join` and `Promise.get()`.

**Switches.** `switchOn` emits the SWITCH entry with every declared case (`Replayer.Lane.switchOn`)
and then runs the taken case's body inline, with the lane's output redirected into that case's
list: the body's calls are placed inside the case in the same fragment, and when the body pauses,
the lane's marker is on a task inside the case, so the next continuation's fragment is inserted
there too. Nesting is automatic; a `switchOn` inside a case does the same one level down. When a
continuation resumes inside a case and the body then returns, the lane remembers the switch
(`anchorAfter`) and the next entry it emits carries `metadata.after = <switch ref>`, so the server
inserts it after the switch rather than after the task the code paused on inside the case. An
anchor the server cannot find is a rejected fragment, and the workflow fails. A body that makes no
call is an empty branch and the code continues inline, with no anchor needed.

**Untaken cases are previewed.** When the switch is first emitted, every case the value did not
take (and the `otherwise` body, when a declared case was taken) is read from its compiled code
(`Replayer.previewOf`, `CodeReader`); nothing runs, so the live run cannot be touched and no value
can stop the reading. Case bodies are serializable lambdas, so the runtime lambda names its compiled
method. Each call becomes an entry with `metadata.preview: true` and a reference name
`<base>_if_<caseKey>` (nested previews compose the keys, `shipA_if_c_x`; `_2`, `_3` on a clash; the
names are reserved so later real calls never reuse them). `once`, `now` and `random` are not steps;
`async` bodies become the branches of one `fork_if_<key>` FORK_JOIN with its `_join`, marked as not
waiting when the code does not join them, and a later `join` or `get` a JOIN of its own; `fanOut` becomes
a FORK_JOIN_DYNAMIC + JOIN; `repeatUntil` and `forEach` become a DO_WHILE with `loopCondition: "false"`;
a nested `switchOn` becomes a SWITCH with `switchCaseValue: ""` and every case previewed; `awaitCondition`
a WAIT. A case that cannot be read is drawn empty and the run goes on. Preview
entries are never scheduled, since the switch takes another case; the server validates them like
any fragment entry (unique references, known types, lane). In the kitchensink's odd run, case
`"0"` shows `task_4_if_0`, then the dynamic fork `fanout1_if_0` and its join.

**Plain ifs and switches.** When `CodeReader` reads a method, each `if` and `switch` whose arms
call the server (directly, or through a lambda or a helper it follows) becomes a branch: its arms
as instruction ranges, the lines each arm's calls are made from, what each arm would call, and
whether the code can go through an arm without a call. javac's shapes are recognised: the jumps of
an `a && b` or `a || b` condition are one if; a jump that ends a loop is the loop's condition, not
an if; an else is the code a then-arm jumps over (a then-arm ending in `return`, `throw`, `break`
or `continue` has none, and the code after it follows the switch); string switches are labelled by
their strings and enum switches by their constants. A branch is named after what it decides on:
the last method its condition calls, else a field, else a local variable (`if_flagged`,
`switch_kind`, `if_pass`), numbered when repeated (`if_pass_2`).

While the code runs, each call reports the lines its workflow frames are on (`StackWalker`), and
those lines say which arms it is in, since no other code of the method calls from an arm's lines.
The lane (`Replayer.Lane.follow`) keeps the branches it is inside: a call in an arm it has not
entered draws the branch as a SWITCH with every arm and the others previewed, and the calls that
follow go in that arm's case, exactly as for a `switchOn`, including resuming inside it in a later
continuation (`metadata.after`). A call outside the arm ends it. A call from a line before the last
one in the same arm means a loop around the branch came round: the branch is drawn again. A branch
the code went past without a call, between two calls of the same frame, is drawn with the arm that
must have been taken, when only one arm can be passed without a call and no loop around it could
have run it more than once; after the lane's last call, also only when every return of the method
comes after it.

What is not drawn stays plain code, as it was before: an `if` whose arms share a line with each
other or with code outside them (a one-line ternary with a call in each arm), a switch whose cases
fall through, a branch passed without a call where more than one arm could have been. An execution
recorded by a build that did not draw a branch replays without it, and its calls stay where they
were; one recorded with another arm than the code now takes is a divergence.

**How a continuation reaches a worker.** When the task a lane paused on finishes, its marker
becomes runnable and the server pushes a message to the continuation queue, the workflow's code
type (`<workflow>.v<version>.code`). By default that is one shared queue and any worker can run
any continuation; a start can name domains instead (see Deploying). The SDK holds no per-execution
lock: the server's one-continuation-per-execution slot (`nativeState.activeContinuation`) is what
serializes an execution. Workers poll with the ordinary batch task poll; the poll leases the marker (`LEASED`, with an epoch) and hands back the finished
task itself. The worker completes with the ordinary task update (`update-v2`), naming the lease's
`n` and `epoch`; a completion from a stale lease is refused with 409. The first continuation
(`<workflowId>|main`) and each handler run (`<workflowId>|handlers`) have no task, so the poll
builds one from the execution's `nativeState`, with the workflow input or the claimed messages.

**Every continuation is a replay.** The worker is stateless. For each continuation it creates a
fresh instance of the class, fetches the execution with its tasks, replays every applied
continuation in the order the server applied them (`step` on the markers, with `@Variable` fields
restored to each one's `resumedWith`), then runs the active continuation against the current
variables. Each call is matched by lane, task name and ordinal; a recorded result is returned, a
missing one is scheduled. Handler runs are not replayed; their effect is in the variables. This is
a replay engine, a small one: its input is the markers and the task list, not a generic event
history. The same mechanism powers `DurableReplay`.

**Failures.** A task the code waits on is first retried like any Conductor task: worker tasks
three times with exponential backoff by default (`@WorkerTask(retryCount, retryDelaySeconds)`),
HTTP and LLM steps with the same policy (`http(ref).retries(count, delay)`). A worker that throws
`NonRetryableException` is not retried. When retries are exhausted the task is FAILED in the UI
and, because it is emitted as `optional`, the server schedules the lane's next continuation
instead of failing the workflow. The SDK throws `TaskFailure` at the call. Caught, the code
decides; uncaught, the lane ends with the error: the main lane fails the workflow with that
reason, and an `async` lane raises it in whoever calls `get()`. Fire-and-forget steps, events and
started workflows, never throw; their failures are visible in the UI. Timeouts take the same path:
a human task that nobody answers returns an empty result, and the task shows TIMED_OUT.

**Divergence policy.** Divergence is strict. A replay that reaches a call, fork, loop, wait,
switch or recorded value the recording does not have raises `NonDeterminismException`, with a
message naming the call and its source line, and the server holds the execution, retrying with
backoff, until a compatible build runs it. A `switchOn` replays against the recorded `switchCaseValue`: the code
taking another case is a divergence too ("switch <ref> took case X in the recorded execution but
the code now takes Y").

**Variables.** `@Variable` fields are written to `workflow.variables` by every continuation's
completion, together with the values the continuation started from (`resumedWith`); the server
applies only what changed and defers a completion whose base another completion has moved. A
replay restores the fields to each continuation's `resumedWith`, so handler writes are seen at the
same points as the first time.

## Rules for workflow code

1. Anything non-deterministic or with a side effect that is not a task goes through `once()`,
   `now()` or `random()`. `once()` is at-least-once on worker loss, like any task.
2. Calls are matched by task name and ordinal within their lane on a re-run. Adding a log line,
   reordering independent calls, or adding a call in code that running executions have not reached yet is safe. A call added
   before the point they have reached holds them.
3. Blocking I/O belongs in tasks, which are retried, or in `once()` when it is small and safe to fail. Both are recorded and
   visible.
4. Wait with `sleep(Duration)`, never `Thread.sleep`: plain code runs again on every replay, so a
   `Thread.sleep` is repeated each time the code continues and holds a worker thread. The host
   reads the compiled workflow code at `start()` and logs a warning naming the workflow and the line
   of every `Thread.sleep` it can reach, through lambdas and helpers.
5. Plain code in a `finally` block also runs each time the lane pauses, as the lane is torn down.
   Keep side effects out of it. An untaken case or arm is only read from the compiled code, so
   nothing in it runs.

## The diagram

The live execution shows the path taken, with status, inputs, outputs and source line per task.
There is nothing to fold: the execution holds exactly the tasks the code called, and a task the
code paused on shows its continuation (lane, number, state, worker, applied time) in its panel.

A `switchOn` shows as a SWITCH node with every declared case as a branch, the default included,
and the taken case marked from the task's `selectedCase`; the calls its body made sit inside that
branch, and the other branches show their preview entries (`<task>_if_<key>`) without a status,
as the untaken branches of a JSON switch do (`ui-next`'s switch mapper, unchanged). A plain `if`
or `switch` whose arms call the server shows the same way, named after what it decides on.

**Reading a definition against an execution.** The definition page shows the discovered graph:
everything the code can call, every switch with all its cases, every fork, every loop, with the
reference names an execution would use (`pick`, `fork`, `fork_join`, `sleep`, `region`, `ship`,
`ship_2` for `DiscoveryTest`'s fulfilment workflow). An execution shows what the code did: the
server empties the discovered tasks from the execution's own copy of the definition at start
(`NativeContinuationService.initialize`), and that copy grows one fragment per continuation, so it
holds exactly the tasks the code called plus the previews of the switch cases it did not take.
A hello world registered as `[greet, shout]` runs with exactly two tasks; the kitchensink
registers 37 top-level entries and an odd-path execution grows a 45-entry copy. Every entry still
records its source line (`metadata.site`, via `StackWalker` skipping SDK frames), so both views
link to the code.

## Mapping to the JSON kitchensink

| JSON task | Type | In `KitchenSinkWorkflow.kitchenSink` |
|---|---|---|
| `task_1` | SIMPLE | `tasks.task1(...)` |
| `event_0` | EVENT | `event("event_0").to("conductor").send(stats)` |
| `task_2` | DYNAMIC | `task(input.task2Name()).ref("task_2")...as(OddEven.class)` |
| `oddEvenDecision` | DECISION | `switch (oddEven.oddEven()) { case "0" -> {...} case "1" -> {...} default -> throw new WorkflowFailedException(...); }`, a plain Java switch drawn as the SWITCH `switch_oddEven` (`value-param` on `switchCaseValue`) with cases `"0"` and `"1"`; the taken case holds the calls below. The JSON's reference name would need `switchOn("oddEvenDecision", ...)` |
| `task_4`, `fanout1`, `join1` | SIMPLE, FORK_JOIN_DYNAMIC, JOIN | `tasks.task4(...)`, `fanOut("fanout1", ...)`, inside case `"0"`; on the odd path the case shows the preview `task_4_if_0` |
| `forkx`, `task_10`, `wf3`, `task_11`, `wf4`, `join2` | FORK_JOIN, SIMPLE, SUB_WORKFLOW, JOIN | two `async` blocks and `join(wf3, wf4)`, inside case `"1"`; each branch is `task → child` as in the JSON; fork and join refs derive as `fork`, `fork_join`, child refs as `sub_flow_1`, `sub_flow_1_2` |
| `get_es_1` | HTTP | `http("get_es_1").get(...).asJson()` |
| `task_30` | SIMPLE | `tasks.task30(...)` |
| `outputParameters` | | the returned record |

## Server and CLI changes

Phase A runs the kitchensink with no server change: an outer DO_WHILE with a step task and an
inline SUB_WORKFLOW per fragment, the pattern the harness SDK proved. Branch independence and the
flat graph need Phase B:

1. Continuation markers: `continuation` on the task the code paused on (copied from the entry's
   `metadata.continuation`), and `nativeState` on the execution for the first continuation and
   the handler lane. The ordinary batch poll of the code type leases a marker and hands back the
   task; the ordinary `update-v2` completes it with a fragment the server appends immediately
   after that task, including inside a FORK_JOIN branch or a SWITCH case, or after the entry the
   fragment's first element names in `metadata.after` (code that left a `switchOn` case).
2. JOIN with live targets: a fork branch ends when its last task is terminal and its marker, if
   any, is applied and ended the lane.
3. Lease epoch: the server accepts a completion only for the current lease (`n` and `epoch`).
4. Recorded values: `once`, `now` and `random` ride in the completion's `recorded` map and are
   stored on the marker; no task.
5. The definition view: planned as an observed graph merged from executions by call site; built
   instead as the graph the SDK discovers from the code at registration (above), with the server
   clearing it from each execution's copy (`NativeContinuationService.initialize`).
6. Per-execution time scale for WAIT, used by dev mode and the embedded test environment.
7. Standing items: JOIN any-of, early-signal buffering in the per-workflow message queue,
   idempotency key on the public start endpoint, hide-by-metadata and source links in the diagram.

SDK: `retryCount`, `retryDelay` and `responseTimeout` attributes on `@WorkerTask`, so the task
definition comes from the annotation.

CLI: `conductor init java`, `conductor workflow debug <id>`, `conductor workflow export <id>`,
`conductor workflow convert --java <name>`.

Latency (push transport) and very large loops (incremental decide) are deferred to their own rounds.

## Open questions for review

1. **Compile-time dependency on task classes.** Calling by method reference means the workflow
   module depends on the task class, which may carry heavy dependencies. Teams can put task
   classes in a thin module, or call by name. Worth a stated convention.
2. **In-memory test fidelity.** Tier 1 simulates server steps rather than running the server. If
   the embedded server can boot in a second or two, it could be the only tier.
3. **`fanOut` stages.** Resolved: `fanOut` runs one lane per item (a FORK_JOIN generated at run
   time), so items progress independently; there are no stages and no hidden tasks.
4. **`once()` visibility versus cost.** Resolved: a recorded value costs nothing beyond its bytes
   on the continuation marker; it is visible in the task panel of the task the code paused on.

## Durability

The guarantees and how each was verified are in the server repository's
`proposals/native-workflows/DURABILITY.md`; how they compare with other durable execution engines
is in `proposals/native-workflows/COMPARISON.md`. `native-examples/chaos.sh` runs the
kitchensink while killing and restarting the worker and the server; every check must still pass.

## Running the kitchensink

```bash
export CONDUCTOR_SERVER_URL=http://localhost:8099/api   # a server built from the conductor branch
./gradlew :native-examples:run     # odd path, even path and charge-failure path, with checks
./gradlew :native-examples:test    # in-memory tests and the replay test against a recorded execution
```

`operator.OperatorChecks` in the same module exercises retry, rerun from a task and restart on a
native execution.
