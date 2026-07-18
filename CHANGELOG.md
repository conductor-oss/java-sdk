# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Merged the Agentspan agent SDK into this repository as four new modules: `conductor-client-ai` (durable AI agents — `Agent`, `AgentRuntime`, `@Tool` functions, guardrails, handoffs, multi-agent strategies), `conductor-client-ai-spring` (Spring Boot auto-configuration), `agent-examples` (150+ runnable examples), and `e2e` (e2e suites, gated behind `-Pe2e`) — docs under [docs/agents/](docs/agents/index.md)
- `conductor-client-ai` and `conductor-client-ai-spring` publish as `org.conductoross:conductor-client-ai` and `org.conductoross:conductor-client-ai-spring`, superseding `org.conductoross.conductor:conductor-agent-sdk[-spring]@0.1.0`; the java package `org.conductoross.conductor.ai[.spring]` is unchanged apart from the client relocation below, so migrating is a dependency-coordinate swap plus — only if those types are imported directly — the relocated imports
- Agent control-plane clients now live in `conductor-client`: `AgentClient` is an interface (implemented by `io.orkes.conductor.client.http.OrkesAgentClient`) covering the full `/agent/*` control plane — `compileAgent`, `deployAgent`, `startAgent`, `getAgentStatus`, `getExecution`, `listExecutions`, `respond`, `stopAgent`, `signalAgent`, `streamSse`, `close` — handed out by `OrkesClients.getAgentClient()`; the SSE streaming client `SseClient` moved to `io.orkes.conductor.client`; their transport DTOs (`AgentRequest`, `StartResponse`, `AgentStatusResponse`, `PendingTool`, `RespondBody`, `CompileResponse`) moved to `io.orkes.conductor.client.model.agent` and the typed exceptions (`AgentspanException`, `AgentAPIException`, `AgentNotFoundException`, `SSEUnavailableException`) to `io.orkes.conductor.client.exceptions`
- SSE streaming hardened: the initial connect throws `SSEUnavailableException` when the server rejects streaming (never a silently-empty stream — `stream()` degrades to status polling), `id:` frames are tracked, and mid-stream drops reconnect with a `Last-Event-ID` header; `AgentRuntime.getClient()` exposes the runtime's own `AgentClient` instance
- Verb contract: `serve` = deploy + serve (each served agent is compiled + registered first, idempotently) with a non-blocking variant `serve(false, agents...)` that returns once workers are polling; `AgentHandle` gains the lifecycle verbs `send(message)` (now actually delivers `{"message": ...}` — it previously discarded the message), `stop()` (graceful, deterministic), `pause()`/`resume()` (un-pause; distinct from `AgentRuntime.resume(executionId, agent)` which re-attaches workers), `cancel(reason)`, and `getStatus()`
- `RunSettings` — per-run LLM overrides (`model`, `temperature`, `maxTokens`, `reasoningEffort`, `thinkingBudgetTokens`) on `run`/`start`/`stream` and their async variants; only non-null fields override the agent (zero values apply), mutating the serialized root config so overrides flow into the LLM tasks
- Connection environment: standard `CONDUCTOR_SERVER_URL`/`CONDUCTOR_AUTH_KEY`/`CONDUCTOR_AUTH_SECRET` now win over the legacy `AGENTSPAN_*` names (still honored as fallbacks); the default server URL is `http://localhost:8080` (was `6767`); blank variables no longer clobber the chain. The resolution lives in the client layer — `ApiClient.builder().useEnvVariables(true)` / `new ApiClient()` — not on `AgentRuntime` (matching the Python SDK, where `AgentRuntime()` defaults to `Configuration()`); `AgentRuntime` exposes no client factories
- `AgentConfig` knobs with lenient env parsing (invalid/empty → default): `autoStartWorkers`, `daemonWorkers`, `streamingEnabled`, `livenessEnabled`, `livenessStallSeconds`, `livenessCheckIntervalSeconds`
- Worker credentials ride the `runtimeMetadata` wire contract: declared secret names are stamped on `TaskDef.runtimeMetadata` at (every) registration and a capable server (agentspan > 0.4.2 / conductor-oss ≥ 3.32.0-rc, conductor-oss PR #1255) delivers values on the wire-only `Task.runtimeMetadata` at poll time; dispatch is fail-closed (missing delivery → terminal failure; ambient process env is never read) — the `/workers/secrets` fetch path and its transport exceptions are deleted; see [docs/design/secret-injection-contract.md](docs/design/secret-injection-contract.md)
- Liveness for stateful runs: a `SCHEDULED` task with zero polls beyond `livenessStallSeconds` surfaces as `WorkerStallError` from the handle's wait instead of burning the full timeout
- Swarm hand-offs: transfer tools echo the hand-off `message`; `check_transfer` is first-wins with `transfer_message` and surfaces non-winning transfers in `dropped_transfers` (with a warning) instead of silently discarding them
- `agent-e2e` GitHub workflow runs the e2e suites as a two-server matrix: the Conductor OSS server boot JAR `3.32.0-rc.8` (Maven Central) and the released `agentspan-server-0.4.4.jar`

### Removed

- The source-less publishing shim modules `java-sdk` (`org.conductoross:java-sdk`), `orkes-client` (`io.orkes.conductor:orkes-conductor-client`), and `orkes-spring` (`io.orkes.conductor:orkes-conductor-client-spring`) are deleted — depend on `org.conductoross:conductor-client` / `conductor-client-spring` directly instead
- File lifecycle objects and automatic worker integration (`FileHandler`, `ManagedFileHandler`, `LocalFileHandler`, `FileUploader`, `WorkflowFileClient`, `Task.getFileUploader()`, `Task.getInputFileHandler()`, and `TaskRunnerConfigurer.withFileClient()`) are removed. Workers now inject `FileClient` and explicitly upload/download opaque handle strings.

### Changed

- `useEnvVariables(true)` (and the `new ApiClient()` no-arg constructor) no longer throws when `CONDUCTOR_SERVER_URL` is unset — it falls back to `AGENTSPAN_SERVER_URL`, then `http://localhost:8080/api`; the resolved URL is normalized to end in `/api`, and credentials gain the `AGENTSPAN_AUTH_KEY`/`AGENTSPAN_AUTH_SECRET` fallback
- `FileClient` now requires workflow context for every operation, returns raw `conductor://file/<id>` strings from uploads, selects multipart automatically, and performs atomic explicit downloads. Existing workflows that exchange `{fileHandleId, fileName, contentType}` objects must be drained or upgraded in coordination because new workers exchange raw strings. See the [FileClient guide](docs/file-client.md) and [design note](docs/design/file-client.md).

## [5.1.0]

### Added

- Standardized Prometheus metrics: `PrometheusMetricsCollector` now emits the harmonized cross-SDK metric surface — [details](conductor-client-metrics/README.md)
- Automatic metrics wiring: `ConductorClient.Builder.withMetricsCollector(...)` installs the HTTP interceptor and auto-registers listeners on `TaskClient`, `WorkflowClient`, and `TaskRunnerConfigurer`
- HTTP API client metrics via OkHttp interceptor (`http_api_client_request_seconds`, `task_result_size_bytes`, `workflow_input_size_bytes`)
- Event-driven metrics architecture with `EventDispatcher` and typed event POJOs
- File storage support: `FileClient` for uploading and downloading files via S3, Azure Blob, GCS, or local storage backends, with single-part and multipart upload support
- `FileHandler` abstraction for passing files into and out of workers — the SDK auto-resolves `conductor://file/` references in task input and uploads `FileHandler` values in task output
- `@InputParam`-annotated worker parameters of type `FileHandler` are automatically deserialized from file references
- Spring auto-configuration for `FileClient` and `FileClientProperties` (`conductor.file-client.*` properties)
- Automatic token refresh via `TokenRefreshInterceptor`: transparently retries requests that fail with `EXPIRED_TOKEN` or `INVALID_TOKEN` (401/403), minting a fresh token and replaying the request once
- `FatalAuthenticationException` and JVM termination when token refresh is permanently exhausted (5 consecutive failures), preventing workers from silently spinning on bad credentials

### Changed

- `PrometheusMetricsCollector` metric names updated to the harmonized cross-SDK catalog (e.g. `task_poll_total`, `task_execute_time_seconds`)
- `micrometer-registry-prometheus` is now a transitive (`api`) dependency
- Token refresh reworked to a reactive interceptor model replacing the previous scheduled refresh mechanism; includes exponential backoff and thundering-herd prevention

### Removed

- Removed non-functioning scheduled token refresh mechanism (replaced by automatic reactive refresh)

### Deprecated

- `TaskClient.ack(String, String)` — use `ack(String taskType, String taskId, String workerId)`

## [4.0.0] - 2024-10-09
- New major release – [Read more](https://orkes.io/blog/conductor-java-client-v4/)

## [4.0.1] - 2024-10-30
- Improve Spring modules with auto-configuration - https://github.com/conductor-oss/conductor/pull/287
- Added Jackson Kotlin module to client ObjectMapper - https://github.com/conductor-oss/conductor/pull/294
- Fix chronounit issue in java sdk - https://github.com/conductor-oss/conductor/pull/298

## [4.0.2] - 2024-12-09
- Added ZoneId to `SaveScheduleRequest` - https://github.com/conductor-oss/conductor/pull/302
- Add callTimeout field to ConductorClient builder - https://github.com/conductor-oss/conductor/pull/317
- Task poll update v2 - https://github.com/conductor-oss/conductor/pull/328

## [4.0.3] - 2024-12-17
- Add testWorkflow to OrkesWorkflowClient - https://github.com/conductor-oss/conductor/pull/333

## [4.0.4] - 2025-01-07
- Added lease extension to java sdk and fixed tests - https://github.com/conductor-oss/conductor/pull/349
- Unify environment variable usage - https://github.com/conductor-oss/conductor/pull/353
- Add support for configurable MetricsCollector in Spring client - https://github.com/conductor-oss/conductor/pull/356
