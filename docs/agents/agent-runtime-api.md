# AgentRuntime — API Reference

`AgentRuntime` is the primary entry point for the Agentspan Java SDK. It manages the connection to the Agentspan server, registers local tool workers, and exposes every operation for running, streaming, deploying, and serving agents.

Implements `AutoCloseable` — always use try-with-resources or call `shutdown()` explicitly.

```java
try (AgentRuntime runtime = new AgentRuntime()) {
    AgentResult result = runtime.run(agent, "Hello!");
    System.out.println(result.getOutput());
}
```

---

## Method summary

| Method | Returns | Description |
|---|---|---|
| [`run`](#run) | `AgentResult` | Execute an agent synchronously |
| [`runAsync`](#runasync) | `CompletableFuture<AgentResult>` | Execute an agent asynchronously |
| [`start`](#start) | `AgentHandle` | Fire-and-forget; returns a handle to poll/approve |
| [`startAsync`](#startasync) | `CompletableFuture<AgentHandle>` | Async fire-and-forget |
| [`stream`](#stream) | `AgentStream` | Execute and iterate events as they arrive |
| [`streamAsync`](#streamasync) | `CompletableFuture<AgentStream>` | Async event stream |
| [`plan`](#plan) | `CompileResponse` | Compile without executing |
| [`deploy`](#deploy) | `List<DeploymentInfo>` | Register workflow definition(s) |
| [`deployAsync`](#deployasync) | `CompletableFuture<List<DeploymentInfo>>` | Async deploy |
| [`serve`](#serve) | `void` | Long-running worker mode (blocks) |
| [`resume`](#resume) | `AgentHandle` | Re-attach to an existing execution |
| [`resumeAsync`](#resumeasync) | `CompletableFuture<AgentHandle>` | Async re-attach |
| [`schedules`](#schedules) | `Schedules` | Access the scheduling API |
| [`shutdown`](#shutdown) | `void` | Stop workers and release HTTP connections |

---

## Constructors

```java
new AgentRuntime()
```
Reads server URL and auth from environment, worker tuning from environment.

```java
new AgentRuntime(AgentConfig config)
```
Reads server URL and auth from environment; explicit worker tuning.

```java
new AgentRuntime(ApiClient conductorClient)
```
Explicit server connection; worker tuning from environment.

```java
new AgentRuntime(ApiClient conductorClient, AgentConfig config)
```
Fully explicit — the canonical constructor all others delegate to.

### Environment variables

Standard Conductor variables win; the legacy Agentspan names are honored as
fallbacks. Invalid or empty values fall back to the default.

| Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_SERVER_URL` → `AGENTSPAN_SERVER_URL` | `http://localhost:8080` | Conductor server base URL |
| `CONDUCTOR_AUTH_KEY` → `AGENTSPAN_AUTH_KEY` | _(none)_ | API key (optional) |
| `CONDUCTOR_AUTH_SECRET` → `AGENTSPAN_AUTH_SECRET` | _(none)_ | API secret (optional) |
| `AGENTSPAN_WORKER_POLL_INTERVAL` | `100` | Worker poll interval (ms) |
| `AGENTSPAN_WORKER_THREADS` | `1` | Worker thread count |
| `AGENTSPAN_AUTO_START_WORKERS` | `true` | Register + start workers on `run`/`start`/`stream` (`serve` always starts) |
| `AGENTSPAN_DAEMON_WORKERS` | `true` | SDK-owned background threads (SSE reader, liveness monitor) run as daemons |
| `AGENTSPAN_STREAMING_ENABLED` | `true` | Use SSE for `stream()`; `false` degrades to status polling |
| `AGENTSPAN_LIVENESS_ENABLED` | `true` | Watch stateful runs for worker stalls (`WorkerStallError`) |
| `AGENTSPAN_LIVENESS_STALL_SECONDS` | `30.0` | Seconds a task may sit unpolled before it counts as a stall |
| `AGENTSPAN_LIVENESS_CHECK_INTERVAL_SECONDS` | `10.0` | Seconds between liveness checks |

---

## Building the ApiClient

Build an `ApiClient` (via its own builder — client construction lives in the client layer, not on the runtime) to pass to the `AgentRuntime(ApiClient)` constructor. The `ApiClient` owns server URL, auth, and HTTP timeouts.

```java
// From environment: CONDUCTOR_SERVER_URL → AGENTSPAN_SERVER_URL → http://localhost:8080/api
// (same resolution the no-arg AgentRuntime() constructor uses internally)
ApiClient client = ApiClient.builder().useEnvVariables(true).build();

// Unauthenticated — local dev
ApiClient client = ApiClient.builder().basePath("http://localhost:8080/api").build();

// Key/secret auth
ApiClient client = ApiClient.builder()
        .basePath("http://myserver:8080/api")
        .credentials("key", "secret")
        .build();
```

The no-arg `AgentRuntime()` constructor additionally applies `connectTimeout=10s`, `readTimeout=30s`, `writeTimeout=30s`. The env path normalizes the URL to end in `/api`; an explicit `basePath` is used as given.

---

## run

Execute an agent and block until it completes. The most common operation.

```java
AgentResult result = runtime.run(agent, "What is the capital of France?");
System.out.println(result.getOutput());
System.out.println(result.getStatus());       // AgentStatus.COMPLETED
System.out.println(result.getTokenUsage());   // TokenUsage{prompt=312, completion=47, total=359}
```

**Overloads:**

```java
AgentResult run(Agent agent, String prompt)

// For PLAN_EXECUTE strategy — bypasses the planner LLM entirely
AgentResult run(Agent agent, String prompt, Plan plan)

// Per-run LLM overrides — only non-null fields override the agent's settings.
// Also available on start()/stream() and every async variant.
AgentResult run(Agent agent, String prompt, RunSettings runSettings)
```

```java
runtime.run(agent, "Summarize this document", new RunSettings()
        .model("openai/gpt-4o")
        .temperature(0.2)
        .maxTokens(2048));
```

**What happens internally:**
1. Workers for the agent's tools are registered with the Conductor task runner.
2. `POST /api/agent/start` — server compiles, registers, and starts the workflow.
3. Polls `GET /api/agent/{id}/status` every 2 seconds until terminal.
4. On completion, calls `GET /api/workflow/{id}` once to aggregate token usage and tool calls into the `AgentResult`.

**Returns `AgentResult`:**

| Method | Type | Description |
|---|---|---|
| `getOutput()` | `Object` | Final LLM output (String or structured object) |
| `getStatus()` | `AgentStatus` | `COMPLETED`, `FAILED`, `TERMINATED`, `TIMED_OUT` |
| `getExecutionId()` | `String` | Conductor workflow ID |
| `getTokenUsage()` | `TokenUsage` | Aggregated `promptTokens`, `completionTokens`, `totalTokens` |
| `getToolCalls()` | `List<Map<String,Object>>` | All tool invocations: `{name, args, result}` |
| `getEvents()` | `List<AgentEvent>` | Full event log (populated by streaming paths) |
| `getError()` | `String` | Failure/termination reason when `status != COMPLETED` |
| `isSuccess()` | `boolean` | `true` when `status == COMPLETED` |

---

## runAsync

Non-blocking variant of `run`. Uses the common `ForkJoinPool`.

```java
CompletableFuture<AgentResult> runAsync(Agent agent, String prompt)
CompletableFuture<AgentResult> runAsync(Agent agent, String prompt, Plan plan)
```

```java
// Run multiple agents concurrently
List<CompletableFuture<AgentResult>> futures = prompts.stream()
    .map(p -> runtime.runAsync(agent, p))
    .toList();
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

`AgentRuntime` is thread-safe — share one instance across threads.

---

## start

Fire-and-forget: registers workers, starts the execution, and returns immediately with an `AgentHandle`. Does not wait for completion.

```java
AgentHandle handle = runtime.start(agent, "Deploy version 2.1 to production");
String executionId = handle.getExecutionId();

// Poll later
AgentResult result = handle.waitForResult();

// Or approve a HITL step
handle.approve("Approved by Alice");
handle.reject("Needs more testing");

// Respond for MANUAL strategy
handle.respond(Map.of("selected", "writer_agent"));
```

**`AgentHandle` methods:**

| Method | Description |
|---|---|
| `getExecutionId()` | Conductor workflow ID |
| `waitForResult()` | Block until completion (default 10-min timeout) |
| `waitForResult(long timeoutMs, long pollMs)` | Block with explicit timeout |
| `waitUntilWaiting(long timeoutMs)` | Block until a HITL task is paused |
| `isWaiting()` | `true` if a HITL task is currently paused |
| `approve()` | Resume with `{"approved": true}` |
| `approve(String comment)` | Resume with approval + comment |
| `reject(String reason)` | Resume with `{"approved": false, "reason": ...}` |
| `respond(Map<String,Object>)` | Send arbitrary response (MANUAL strategy, custom schemas) |

---

## startAsync

```java
CompletableFuture<AgentHandle> startAsync(Agent agent, String prompt)
CompletableFuture<AgentHandle> startAsync(Agent agent, String prompt, Plan plan)
```

---

## stream

Execute and iterate over events as they are emitted by the server via SSE. Blocks the calling thread during iteration.

```java
try (AgentStream stream = runtime.stream(agent, "Tell me a story")) {
    for (AgentEvent event : stream) {
        switch (event.getType()) {
            case MESSAGE    -> System.out.print(event.getContent());
            case TOOL_CALL  -> System.out.println("→ " + event.getToolName() + "(" + event.getArgs() + ")");
            case TOOL_RESULT -> System.out.println("← " + event.getResult());
            case DONE       -> { /* stream ended */ }
        }
    }
}
// After iteration, stream.getResult() returns the completed AgentResult
```

`AgentStream` implements `Iterable<AgentEvent>` and `AutoCloseable`.

**`AgentEvent` fields:**

| Method | Type | Description |
|---|---|---|
| `getType()` | `EventType` | `MESSAGE`, `TOOL_CALL`, `TOOL_RESULT`, `THINKING`, `WAITING`, `HANDOFF`, `ERROR`, `DONE` |
| `getContent()` | `String` | Message text or thinking content |
| `getToolName()` | `String` | Tool name for `TOOL_CALL`/`TOOL_RESULT` |
| `getArgs()` | `Map<String,Object>` | Tool arguments for `TOOL_CALL` |
| `getResult()` | `Object` | Tool result for `TOOL_RESULT` |
| `getExecutionId()` | `String` | Execution that emitted this event |

**HITL approval from a stream** — use the event-targeted overloads so sub-execution approvals route correctly:

```java
for (AgentEvent event : stream) {
    if (event.getType() == EventType.WAITING) {
        stream.approve(event);        // targets event.getExecutionId()
        // or: stream.reject(event, "reason")
    }
}
```

---

## streamAsync

```java
CompletableFuture<AgentStream> streamAsync(Agent agent, String prompt)
```

---

## plan

Compile the agent into a Conductor workflow definition without registering or starting anything. Useful for inspecting the workflow shape, CI/CD validation, or pre-warming the server cache.

```java
CompileResponse compile = runtime.plan(agent);

Map<String, Object> workflowDef = compile.getWorkflowDef();
List<String> requiredWorkers  = compile.getRequiredWorkers();
```

Delegates to `AgentClient.compileAgent` → `POST /api/agent/compile`.

---

## deploy

Register workflow definition(s) on the server without starting an execution. Idempotent — safe to call on every application startup.

```java
// One or more agents
List<DeploymentInfo> infos = runtime.deploy(agentA, agentB);
infos.forEach(i -> System.out.println(i.getRegisteredName()));

// With schedules — deploys the agent and reconciles its cron schedules
Schedule daily = Schedule.builder().name("daily").cron("0 9 * * *").build();
runtime.deploy(agent, List.of(daily));
```

**`DeploymentInfo` fields:** `getRegisteredName()` (server workflow name), `getAgentName()` (SDK agent name).

---

## deployAsync

```java
CompletableFuture<List<DeploymentInfo>> deployAsync(Agent... agents)
```

---

## serve

Deploy the agents (compile + register on the server — idempotent), register their workers, and block indefinitely, polling for tasks from the Conductor server. Designed for long-running worker processes: `serve` = deploy + serve, so a bare `runtime.serve(agent)` is a complete, startable deployment.

```java
// Blocks until the process is killed (SIGTERM triggers graceful shutdown)
runtime.serve(agentA, agentB);

// Non-blocking: returns once agents are deployed and workers are polling —
// for embedding in a host application that owns the process lifecycle.
// The caller is responsible for runtime.shutdown().
runtime.serve(false, agentA, agentB);
```

A JVM shutdown hook calls `workerManager.stop()` on SIGTERM (blocking mode only). Unlike `run()`, `serve()` does not start any executions — it deploys the agents and makes their workers available for tasks the server dispatches.

---

## resume

Re-attach to a running or paused execution that was started in a previous process. Re-registers the agent's workers so they can continue serving tasks.

```java
AgentHandle handle = runtime.resume("a3f92b1c-...", agent);
AgentResult result = handle.waitForResult();
```

Useful for crash recovery or reconnecting after a planned restart.

---

## resumeAsync

```java
CompletableFuture<AgentHandle> resumeAsync(String executionId, Agent agent)
```

---

## schedules

Access the scheduling API lazily (created on first call, shared thereafter).

```java
Schedules schedules = runtime.schedules();

schedules.list("my_agent");                       // List<ScheduleInfo>
schedules.runNow(schedules.get("my_agent-daily")); // trigger immediately (takes ScheduleInfo)
schedules.pause("my_agent-daily");
schedules.resume("my_agent-daily");
schedules.delete("my_agent-daily");
```

See [Scheduling concepts](concepts/scheduling.md) for the full `Schedules` API.

---

## shutdown

Stop all worker threads, drain in-flight tasks, and release HTTP connections (OkHttp connection pool + dispatcher thread pool).

```java
runtime.shutdown();
// equivalent:
runtime.close();   // AutoCloseable — called automatically by try-with-resources
```

Without explicit shutdown, OkHttp's thread pool keeps threads alive for ~60s after the last request. In tests or short-lived processes, always close the runtime.

---

## AgentConfig

Worker-runner tuning. **Does not hold server URL or auth** — those are on `ApiClient`.

```java
new AgentConfig()                  // defaults: 100ms poll, 1 thread
new AgentConfig(pollMs, threads)   // explicit
AgentConfig.fromEnv()              // reads AGENTSPAN_WORKER_* env vars
```

| Parameter | Env var | Default | Description |
|---|---|---|---|
| `workerPollIntervalMs` | `AGENTSPAN_WORKER_POLL_INTERVAL` | `100` | How often workers poll for tasks (ms). 100ms is fast for dev; raise to 500–1000ms in production to reduce server load. |
| `workerThreadCount` | `AGENTSPAN_WORKER_THREADS` | `1` | Thread pool size. The actual pool is `max(configured, numWorkerTypes)` so every task type gets at least one thread. |

---

## Thread safety

`AgentRuntime` is thread-safe. Share one instance across all threads in an application:

```java
// Application lifecycle — create once
private static final AgentRuntime RUNTIME = new AgentRuntime();

// Shut down on application exit
Runtime.getRuntime().addShutdownHook(new Thread(RUNTIME::shutdown));
```

Do not create one `AgentRuntime` per request — each instance owns its own OkHttp connection pool and worker thread pool.
