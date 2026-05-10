# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- **Metrics harmonization** - canonical metric surface aligned with the cross-SDK catalog, opt-in via `WORKER_CANONICAL_METRICS=true`
  - New `CanonicalPrometheusMetricsCollector` emits the harmonized cross-SDK catalog: `task_poll_total`, `task_poll_time_seconds{status}`, `task_poll_error_total{exception}`, `task_execution_started_total`, `task_execute_time_seconds{status}`, `task_execute_error_total{exception}`, `task_update_time_seconds{status}`, `task_update_error_total{exception}`, `task_ack_failed_total`, `task_ack_error_total{exception}`, `task_execution_queue_full_total`, `task_paused_total`, `thread_uncaught_exceptions_total{exception}`, `external_payload_used_total{entityName,operation,payloadType}`, `task_result_size_bytes`, `workflow_input_size_bytes{workflowType,version}`, `workflow_start_error_total{workflowType,exception}`, `active_workers` (gauge), and `http_api_client_request_seconds{method,uri,status}`. Time histograms use buckets `0.001…10s`; size histograms use `100…10_000_000` bytes.
  - `MetricsCollectorFactory.create()` selects between `LegacyPrometheusMetricsCollector` (default) and `CanonicalPrometheusMetricsCollector` based on `WORKER_CANONICAL_METRICS` (truthy values: `true`, `1`, `yes`, case-insensitive). `WORKER_LEGACY_METRICS` is reserved for a future default-flip phase and is not currently read.
  - `MetricsBundle.create(port)` convenience that builds the factory-selected collector and starts the Prometheus scrape server in one call.
  - `ApiClientMetrics` SPI with an internal OkHttp interceptor. `ConductorClient.Builder.withMetricsCollector(...)` installs the interceptor and enables automatic listener registration on downstream clients. `ConductorClient.Builder.withHttpMetrics(...)` installs only the interceptor, leaving listener registration to the caller. The interceptor records HTTP-client latency for every request and, for requests tagged with a `PayloadKind`, the body size from `RequestBody.contentLength()` — populating `task_result_size_bytes{taskType}` and `workflow_input_size_bytes{workflowType,version}` without an extra JSON serialization. The `uri` label uses the path template (e.g. `/workflow/{workflowId}`) rather than the resolved URL, so the label space is bounded by the number of distinct API endpoints regardless of how many unique workflow or task IDs are processed.
  - New `PayloadKind` sealed interface (supported by Java 21 which the project targets) with `TaskResult` and `WorkflowInput` record implementations, used to tag outbound `ConductorClientRequest`s so the metrics interceptor can label payload-size histograms without a second serialization pass.
  - `TaskClient` and `WorkflowClient` auto-register as event listeners when the underlying `ConductorClient` is built with a metrics collector **and** `MetricsCollector.isAutoWiringEnabled()` returns `true` (canonical defaults to `true`; legacy defaults to `false` for backward compatibility). Call `setAutoWiringEnabled(true)` on any collector to opt in. New event POJOs under `events/taskrunner/` and `events/listeners/` thread task-runner and workflow events into the metrics collector.
  - `TaskRunnerConfigurer.Builder.withActiveWorkersTracking(boolean)` explicitly enables or disables `ActiveWorkersChanged` event publishing on the task-execution hot path. When not set, the default is derived from `MetricsCollector.isActiveWorkersTrackingEnabled()` (canonical: `true`, legacy: `false`). Legacy SDK upgraders see zero additional hot-path overhead by default.
  - `TaskRunnerConfigurer.Builder.withDiagnosticEvents(boolean)` explicitly enables or disables per-poll-cycle diagnostic events (`TaskPaused`, `TaskExecutionQueueFull`). When not set, the default is derived from `MetricsCollector.isDiagnosticEventsEnabled()` (canonical: `true`, legacy: `false`). Legacy SDK upgraders see zero additional hot-path overhead by default.
  - New `TaskClient.updateTask(TaskResult, String taskType)` and `TaskClient.updateTaskV2(TaskResult, String taskType)` overloads carry the task definition name through to the canonical size histogram (which `TaskResult` itself does not). The single-argument forms are retained and emit the histogram with `taskType=""`.
  - Harness deployment manifest sets `WORKER_CANONICAL_METRICS=true` so certification runs exercise the canonical surface; `HarnessMain` logs which collector is active.

### Changed

- **Metrics harmonization** - defaults preserved; legacy metrics emit unchanged when `WORKER_CANONICAL_METRICS` is unset
  - `conductor-client-metrics`: `micrometer-registry-prometheus` is now an `api` dependency so consumers see it transitively. `okhttp` is an `implementation` dependency (not leaked transitively).
  - Default behavior is unchanged: with no env var set, `LegacyPrometheusMetricsCollector` emits the previously released six meters (`poll_started{type}`, `poll_success{type}`, `poll_failure{type}`, `task_execution_started{type}`, `task_execution_completed{type}`, `task_execution_failure{type}`) byte-for-byte identically.
  - Rewrote `conductor-client-metrics/README.md` with full legacy and canonical catalogs, label conventions, a legacy → canonical migration table, and troubleshooting guidance.
  - The Prometheus meter registry is now a `static final` singleton **per concrete subclass** (`LegacyPrometheusMetricsCollector` and `CanonicalPrometheusMetricsCollector` each own their own static registry). This preserves the 4.0.x safety property that multiple instances of the same collector type share a single registry (so accidentally creating two instances cannot cause silent metric loss), while keeping legacy and canonical meter names isolated from each other. The base class `AbstractPrometheusMetricsCollector` now receives the registry via its constructor rather than creating one per instance.
  - `TaskRunnerConfigurer.shutdown()` now calls `taskClient.unregisterListeners()` to clean up auto-wired listener registrations from `ListenerRegister`'s static dedup map, preventing stale entries in long-lived JVMs that re-create configurer instances.
  - Updated `README.md` "Monitoring Workers" and `INTERCEPTOR.md` to use `MetricsCollectorFactory.create()` and reference the env var.
  - Removed `WorkflowInputPayloadSizeEvent` publish from `checkAndUploadToExternalStorage` and `TaskResultPayloadSizeEvent` publish from `handleExternalStorage`. These were dead code on `main`: the call site was gated behind `isEnforceThresholds()` (default `false`), the `PrometheusMetricsCollector.consume()` handlers were unimplemented `//TODO` stubs, and neither `workflowClient.registerListener()` nor `taskClient.registerListener()` was ever called in any wiring path — no deployment ever received these metrics. Payload-size observability is now provided unconditionally by the `PayloadKind` tag on outbound requests, recorded at wire time by the `ApiClientMetrics` OkHttp interceptor as `task_result_size_bytes{taskType}` and `workflow_input_size_bytes{workflowType,version}`, without double-serialization or manual listener registration.
  - **Transitive dependency change**: `conductor-client-metrics` now exposes `micrometer-registry-prometheus` as an `api` (transitive) dependency instead of `implementation`. Consumers with their own Micrometer version on the classpath should verify compatibility with 1.15.1.
  - **Graceful shutdown improvement**: `TaskRunner` now catches `RejectedExecutionException` when submitting a polled task to the executor during shutdown, logging a descriptive message and releasing the semaphore permit. Previously, the same exception was caught by a broad `catch (Throwable t)` that only logged the message string and did not release the permit. The task outcome is unchanged in both cases (polled but never executed; times out server-side).
  - **OkHttp request tagging**: Outbound requests are tagged with `ConductorClient.PathTemplateTag` (not `String.class`) so the metrics interceptor can read the URI template without conflicting with user-installed interceptors.
  - **ListenerRegister thread safety**: All `ListenerRegister.register()` and `unregister()` methods are now `synchronized`, closing a check-then-act race that could double-register listeners under concurrent initialization.
  - **Diagnostic events gating**: Per-poll-cycle events (`TaskPaused`, `TaskExecutionQueueFull`) and ack diagnostic events (`TaskAckFailure`, `TaskAckError`) are now gated behind `MetricsCollector.isDiagnosticEventsEnabled()` (canonical: `true`, legacy: `false`). When disabled, no event objects are allocated on the hot path.

### Deprecated

- `TaskClient.ack(String taskId, String workerId)` is deprecated in favor of `ack(String taskType, String taskId, String workerId)`. The 2-arg form lacks the `taskType` parameter, so canonical ack metrics (`task_ack_failed_total{taskType}`, `task_ack_error_total{taskType}`) cannot be labeled. The deprecated overload delegates with `taskType=null`, preserving identical runtime behavior — no metrics are emitted and no try-catch wrapping takes effect.

- `com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector` is deprecated and is now a thin alias for `LegacyPrometheusMetricsCollector`. Existing 4.0.x callers that wrote `new PrometheusMetricsCollector()` continue to compile and emit the same six legacy meter names (`poll_started{type}`, `poll_success{type}`, `poll_failure{type}`, `task_execution_started{type}`, `task_execution_completed{type}`, `task_execution_failure{type}`) byte-for-byte. The shim deliberately pins to the legacy collector rather than `MetricsCollectorFactory.create()`, so upgraders with `WORKER_CANONICAL_METRICS=true` already set in their environment are not silently flipped to the canonical surface. New code should use `MetricsCollectorFactory.create()` or `MetricsBundle.create()` to opt into env-var-driven selection.

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
