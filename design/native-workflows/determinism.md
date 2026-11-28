# Determinism in native workflows

For developers who write `@Workflow` methods and for operators who retry or rerun their
executions. It explains why workflow code must be deterministic, what the SDK records and what it
does not, what a retry or rerun does with the code, and how to write a workflow that always takes
the same path. The design as a whole is in [README.md](README.md).

## The one thing to know

**Workflow code runs again from the top, many times, for one execution.** It runs each time the
execution continues after a task, and again when an operator retries or reruns it. The results of
what it already did are given back to it, so nothing is done twice. For that to work the code must
ask for the same things in the same order every time it runs. That is what deterministic means
here.

Code that only depends on its input and on the results of its tasks is deterministic without any
effort. Code that asks the JVM for a random number, the time or a new id is not, unless the value
is recorded.

## How an execution runs

A workflow is a plain method:

```java
@Workflow(name = "ship_order")
public String ship(Order order) {
    String label = tasks.printLabel(order);        // 1
    String carrier = tasks.bookCarrier(order);     // 2
    return tasks.notify(order, label, carrier);    // 3
}
```

What an execution is lives on the server: the tasks it ran and what they returned. Nothing a
worker holds is needed to continue it. Each time the server has a result for the code, a worker
does the following (a *continuation*):

1. creates a new instance of the class and calls the method from the top;
2. answers every call the execution already made from the recording, at once, without scheduling
   anything;
3. reaches the first call the execution has not made, schedules it, and stops there.

So for the workflow above the method runs four times: up to line 1, then through line 1 to line 2,
then through lines 1 and 2 to line 3, then through all three to the `return`. Each run except the
last ends at a call whose result is not known yet.

A worker avoids most of those runs. After a continuation it keeps the method where it stopped, in
memory, and when the execution's next continuation comes to the same process it goes on from
there with the task's result: steps 1 and 2 are skipped. That is a saving, not a different model.
The copy in memory is dropped when it sits idle, when the process restarts or is deployed, and
whenever the server says the execution moved on without it, and then the next continuation runs
the method from the top as above. A retry or a rerun always does. So the code must be correct
under replay even though, most of the time, it is not replayed.

A call is matched to its recording by its lane (the main method, or an `async` block), its task
name and how many calls of that name came before it: `print_label`, `print_label_2`,
`print_label_3`. Source lines and the shape of the code play no part, which is why a log line can
be added to a workflow with executions in flight.

When a run disagrees with the recording, by calling a task where the recording has another, or
none, the worker raises `NonDeterminismException` and the server holds the execution, retrying
with backoff, until a build that agrees with the recording runs it. This is a *divergence*.

## What is recorded

| What the code does | Recorded | On a later run |
|---|---|---|
| calls a task, `http`, `llm`, `child`, `sleep`, any server step | the task and its result | the result is returned; nothing is scheduled |
| `once(name, () -> ...)` | the value | the value is returned; the lambda does not run |
| `now()`, `random()` | the value | the same value is returned |
| `switchOn(name, value)` | the case taken (`switchCaseValue`) | **the recorded case runs**, whatever the value is now |
| `doWhile(name, body, condition)` | the outcome of the condition for each pass | **the recorded number of passes run** |
| `forEach`, `repeatUntil`, `fanOut` | the tasks the server ran for them | the results are returned |
| a `@Variable` field | its value at each continuation | restored to what it was at that point |
| a plain `if` or `switch` whose arms call tasks | the arm taken | **compared**: another arm is a divergence |
| any other plain Java | nothing | it simply runs again |

Three of these rows are worth a closer look.

**`switchOn` and `doWhile` follow the recording.** The SDK is handed their cases and their body as
lambdas, so it chooses what runs. Once a `switchOn` has been decided, every later run takes the
recorded case. Once a `doWhile` pass has been decided, every later run makes the same number of
passes. The code still computes its value each time (a condition may itself call tasks, and those
calls must be reached), but the recording decides. When the two disagree the worker logs a warning
that names the switch or the loop, because it means the value came from something that is not
recorded.

**Plain `if`, `switch`, `for` and `while` cannot be steered.** They are ordinary Java, and nothing
at run time can make an `if` take the other arm. They take the same path only when what they test
is the same on every run. When it is not, the code calls something the recording does not have,
and the execution is held.

**A value is recorded when the continuation that produced it completes.** If a worker dies in the
middle of a continuation, nothing that continuation did was kept, and another worker runs it
again: a `once` lambda can therefore run more than once, like a task.

## Retry, rerun and restart

An operator action changes the recording and then lets the code run again against it.

| Action | What the server does | What the code does |
|---|---|---|
| **Retry** a failed execution | schedules a new attempt of the failed task; everything before it stays | runs from the top; every earlier call, value, case and pass is answered from the recording; the failed call now gets the new attempt's result |
| **Rerun** from a task | drops that task and everything after it; keeps what was recorded before it | runs from the top; what is before the task is answered from the recording, what is after is done again |
| **Restart** | drops everything | runs as a new execution: new values, new decisions |

In detail, for a decision made in code:

- rerun from a task **after** or **inside** a `switchOn` case keeps the decision;
- rerun from a task **before** a `switchOn` decides again, since the switch itself was dropped;
- rerun from the switch task itself, or from a fork or join task, is refused: those belong to the
  code, not to the operator;
- restart decides again.

`native-examples` `operator.OperatorChecks` checks each of these against a running server.

Note what a retry does **not** do: it does not jump into the middle of the method. The code before
the failed task runs again. It does no work, since every call is answered from the recording, but
it does run, and it must arrive at the failed task by the same path as the first time.

## What goes wrong

A coupon, 20% for the lucky and 10% for the rest:

```java
@Workflow(name = "random_coupon")
public Coupon issue(String customer) {
    boolean lucky = Math.random() < 0.5;             // a new number on every run
    int percent;
    if (lucky) {
        percent = tasks.applyDiscount(customer, 20);
    } else {
        percent = tasks.applyDiscount(customer, 10);
    }
    String code = UUID.randomUUID().toString();      // a new id on every run
    Instant issued = Instant.now();                  // a new time on every run
    return new Coupon(code, percent, issued, tasks.sendCoupon(customer, code));
}
```

The first run draws `0.3`, takes the lucky arm and schedules `apply_discount`. When that task
completes the method runs again and draws `0.8`. Now it takes the other arm:

- the plain `if` took `true` in the recording and takes `false` now: a divergence, and the
  execution is held with `if_... took true in the recorded execution but the code now takes false`;
- had the decision gone the same way by luck, `send_coupon` would be scheduled with one id and the
  workflow would return another, because `code` is drawn again on the run after it.

Half of these executions get stuck after their first task. The other half get stuck one task
later, or complete with a coupon code that was never sent. A retry or a rerun rolls the dice again.

## Writing deterministic workflows

### 1. Take random values, the time and ids from the SDK

| Instead of | Use | What it gives |
|---|---|---|
| `Math.random()`, `new Random()`, `ThreadLocalRandom.current()` | `random()` | a `double` in [0, 1), recorded |
| `Instant.now()`, `System.currentTimeMillis()`, `new Date()`, `LocalDate.now()` | `now()` | an `Instant`, recorded |
| `UUID.randomUUID()`, or anything else that can differ between runs | `once(name, () -> ...)` | whatever the lambda returns, recorded |

All three are static imports from `org.conductoross.conductor.sdk.Conductor`. The coupon, fixed:

```java
@Workflow(name = "recorded_coupon")
public Coupon issue(String customer) {
    boolean lucky = random() < 0.5;                  // drawn once, the same on every run after
    int percent;
    if (lucky) {
        percent = tasks.applyDiscount(customer, 20);
    } else {
        percent = tasks.applyDiscount(customer, 10);
    }
    String code = once("coupon_code", () -> UUID.randomUUID().toString());
    Instant issued = now();
    return new Coupon(code, percent, issued, tasks.sendCoupon(customer, code));
}
```

Both versions are in `native-examples`,
[`examples/ex15_strict_determinism/StrictDeterminism.java`](../../native-examples/src/main/java/examples/ex15_strict_determinism/StrictDeterminism.java).

Things that need a time derived from a clock take the recorded one:
`LocalDate.ofInstant(now(), zone)`, or `LocalDate.now(Clock.fixed(now(), zone))`. A `Random` made
with a seed that is itself deterministic (`new Random(order.id().hashCode())`) gives the same
numbers on every run and is fine.

### 2. Put everything else that can change in a task or in `once`

The JVM's clock and random sources are the obvious ones. These change between runs too, and
between the machines that may run the next continuation:

- reading a file, a database, a cache, an HTTP endpoint;
- environment variables, system properties, configuration that can be reloaded, feature flags;
- static fields and singletons that other code changes;
- the host name, the process id, the thread name.

Read them in a `@WorkerTask`, which is retried and shows in the execution, or in `once(...)` when
the read is small and may fail the workflow. Either way the workflow sees one value for the life of
the execution.

```java
int limit = once("credit_limit", () -> config.creditLimit());   // not: config.creditLimit()
```

### 3. Make calls in an order that does not depend on the JVM

Calls are matched by name and by how many came before, so the order they are made in is part of
the path. Iterating a `HashMap` or `HashSet` and calling a task for each element makes an order
that can differ between JVMs. Iterate a `List`, a `LinkedHashMap`, a `TreeMap`, or sort first.

```java
for (String sku : new TreeSet<>(order.skus())) {   // not: for (String sku : order.skus()) when skus() is a HashSet
    tasks.reserve(sku);
}
```

### 4. Wait, and work in parallel, through the SDK

- `sleep(Duration)`, never `Thread.sleep`: a `Thread.sleep` is repeated on every run and holds a
  worker thread. The workers warn about each one at start.
- `async(() -> ...)`, `join` and `fanOut`, never your own threads or executors: what a thread of
  your own does is not recorded, its timing differs on every run, and a call to the SDK from it
  fails (`this call is only valid inside a @Workflow method run by Conductor`).

### 5. Keep side effects out of plain code

Plain code runs on every continuation. A counter incremented, a metric emitted, an email sent
from the workflow method happens once per run, not once per execution. The same goes for a
`finally` block, which also runs each time the code stops at a call. Side effects belong in tasks.
Logging is the exception people accept: a line logged from workflow code is logged again on each
run.

### 6. Use `switchOn` and `doWhile` for decisions and loops you want protected

A plain `if` or loop is the default, and it is correct whenever what it tests is recorded or comes
from the input. `switchOn` and `doWhile` give one thing more: once decided, they stay decided, even
if the value they were decided on turns out to differ on a later run.

```java
String route = tasks.pickRoute(order);                 // a task result: the same on every run
String shipped = switchOn("route", route)
        .when("air", () -> tasks.shipByAir(order))
        .when("sea", () -> tasks.shipBySea(order))
        .get();
```

This protects the path, not the value. If `route` came from something unrecorded, the recorded
case would still run, but the local variable `route` would hold the new value on a later run, and
anything computed from it afterwards would differ. Rules 1 and 2 are what make values right; this
rule is a second line of defence for the path.

### 7. Change code with executions in flight carefully

A new build runs against the recordings of the executions the old build started.

| Change | Executions in flight |
|---|---|
| a log line, a refactoring, a renamed local variable or method | unaffected |
| a call added, removed or reordered **after** the point they have reached | unaffected |
| a call added, removed or reordered **before** the point they have reached | held: the code and the recording disagree |
| the condition of a plain `if` changed so that it takes another arm for them | held |
| a `switchOn` case removed that they took, with no `otherwise` | held |
| a task renamed (`@WorkerTask` value) | held, where they already called it |

For a change of the last four kinds, give the new code a new version, `@Workflow(name = "ship_order",
version = 2)`, and keep hosting version 1 until its executions end. Each version has its own queue.

## The check when workers start

`Workers.start()` reads the compiled code of every workflow it hosts, the workflow method, its
lambdas and the helper methods it calls, and looks for calls that ask the JVM for a random value
or the time:

- `Math.random()`, `new Random()` and `new SplittableRandom()` without a seed, `SecureRandom`,
  `ThreadLocalRandom.current()`, `RandomGenerator.getDefault()` and `of(..)`
- `UUID.randomUUID()`
- `System.currentTimeMillis()`, `System.nanoTime()`
- `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `ZonedDateTime.now()` and the other
  `java.time` `now()`s, with or without a zone, but not `now(Clock)`
- `Clock.systemUTC()` and the other system clocks, `new Date()`, `Calendar.getInstance()`

Nothing is rewritten and nothing is run: the code is only read.

**By default each place is a warning** in the log, naming the workflow, the call, its line and the
replacement, and the workflow is hosted.

**With strict determinism the workflow is refused.**

| Setting | Effect |
|---|---|
| `conductor.workflow.<name>.strictDeterminism=true` | refuse this workflow when its code has such a call |
| `conductor.workflow.all.strictDeterminism=true` | the same for every workflow; a workflow's own setting wins |

They are system properties, or properties of the Spring environment, like the other workflow
settings. When any hosted workflow is refused, `start()` throws `NonDeterminismException` before
anything is registered with the server and before any poller starts, so none of the workers of
that `start()` runs. The message, also logged as an error, lists every place of every refused
workflow:

```
Workflow random_coupon cannot be hosted: 3 places in its code are not deterministic.
  StrictDeterminism$RandomCoupon.issue:93     Math.random()      use Conductor.random()
  StrictDeterminism$RandomCoupon.newCode:108  UUID.randomUUID()  use Conductor.once(name, () -> UUID.randomUUID())
  StrictDeterminism$RandomCoupon.issue:101    Instant.now()      use Conductor.now()
A retry or rerun runs this code again and must take the same path. Anything else that can
change between runs goes in Conductor.once(name, () -> ...).
To host it anyway: conductor.workflow.random_coupon.strictDeterminism=false
```

Turn it on in CI and in production once the warnings are gone: a workflow that would get stuck on
its first retry then fails the deploy instead.

### What the check does not look at, on purpose

- the lambda given to `once(...)`: its value is recorded, so it may call anything;
- `@WorkerTask` methods: a task runs once and its result is recorded;
- `@Signal`, `@Update` and `@Query` handlers: they are not replayed.

### What the check cannot see

It finds direct calls to the JDK's clock and random sources. It does not prove a workflow
deterministic.

- A helper in **another package** than the workflow class's (or its subpackages), or in a library,
  is not followed. A call to `Math.random()` inside it is not found.
- Everything under rules 2 and 3 above: I/O, environment variables, static state, the iteration
  order of unordered collections.
- A call that is there for a harmless reason, such as `System.nanoTime()` to log how long something
  took, is reported all the same. Measure in a task, or leave strict determinism off for that
  workflow.

For these the recording is the net: a run that leaves the recorded path is held and named, and a
replay test finds it before production does.

## Testing

- **A replay test** runs the current code against a recorded execution and fails on any
  divergence. Export an execution (`conductor workflow export <id>`), keep the file with the tests,
  and assert `DurableReplay.assertCompatible(ShipOrder.class, Path.of("ship-order.json"))`. It
  catches the code changes of rule 7 in CI. `native-examples` `KitchenSinkTest` has one.
- **A start test** asserts a workflow passes the check: host it with the setting on and no server;
  a refusal is thrown before anything is sent. `native-examples`
  `examples/ex15_strict_determinism/StrictDeterminismTest` shows both outcomes.
- **The in-memory environment** (`DurableTestEnvironment.inMemory()`) runs the method once, start
  to end, with no replay: it tests what the workflow does, not whether it is deterministic. It
  does not run the check either.
- **Dev mode** (`conductor.workflows(...).devMode(true)`) replays every continuation against a
  real server and keeps no code in memory. A host outside dev mode replays rarely, so code that
  is not deterministic can run for weeks there and diverge on the first deploy; run a workflow
  in dev mode, or with `conductor.workflow.<name>.parkTimeout=0`, before it goes out.

## When an execution is held

The message of the `NonDeterminismException` names what the code did and what the recording has
there, with the source line:

```
The running code differs from what this execution recorded: at ShipOrder.ship:42 it calls
book_carrier, but the recording has print_label there. Deploy a compatible build to continue.
```

Two causes, told apart by whether the code changed:

- **The code changed** (rule 7). Deploy a build that agrees with the recording, or host the old
  version next to the new one. The server retries the continuation with backoff, so the execution
  continues by itself once a compatible worker polls.
- **The code did not change.** Then something it depends on did: look upstream of the named line
  for a value that is not recorded (rules 1 to 3), and for the warnings the workers logged at
  start. A warning that a switch or loop "took ... in the recorded execution and the code now
  computes ..." points at the same kind of value, in a place where the SDK kept the execution on
  its path.

## Summary

- Workflow code runs from the top on every continuation, retry and rerun; calls already made are
  answered from the recording.
- Take random values from `random()`, the time from `now()`, and anything else that can differ
  from `once(...)` or a task.
- `switchOn` and `doWhile` stay on the recorded path; plain `if` and loops rely on their inputs
  being recorded.
- The workers warn about direct clock and random calls at start; `strictDeterminism=true` refuses
  the workflow instead.
- A run that leaves the recorded path is held, never silently continued.
