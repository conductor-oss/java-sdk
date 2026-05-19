# Conductor Client Metrics

**Status: Incubating.**

The `conductor-client-metrics` module provides Prometheus metrics for Java SDK clients and workers. It helps operators monitor worker polling, task execution, task result updates, payload sizes, workflow starts, and HTTP client latency.

This document covers the Java SDK metrics emitted by `MetricsCollectorFactory`, `LegacyPrometheusMetricsCollector`, and `CanonicalPrometheusMetricsCollector`. It does not cover Conductor server metrics or metrics emitted by other SDKs.

## Installation

Add the metrics module to the worker application:

```groovy
dependencies {
    implementation 'org.conductoross:conductor-client-metrics:4.0.1'
}
```

## Usage

The Java SDK offers two ways to wire metrics: automatic wiring (recommended) and manual wiring. Both produce the same metrics output.

### Automatic Wiring

Use `MetricsBundle` to create the collector and start the scrape server, then pass the collector to `ConductorClient.Builder`. All downstream clients and the task runner auto-register themselves as listeners.

```java
import com.netflix.conductor.client.metrics.prometheus.MetricsBundle;

MetricsBundle bundle = MetricsBundle.create(); // port 9991, /metrics

ConductorClient client = ConductorClient.builder()
        .basePath("http://conductor-server:8080/api")
        .withMetricsCollector(bundle.getCollector())
        .build();

TaskClient taskClient = new TaskClient(client);
WorkflowClient workflowClient = new WorkflowClient(client);

TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
        .withThreadCount(10)
        .build();
configurer.init();
```

`MetricsBundle.create()` also accepts `(port)` and `(port, endpoint)` overloads for custom scrape configurations. The client builder accepts the usual timeouts, SSL, authentication, and other options alongside `withMetricsCollector` -- none of them change how metrics wiring works.

In canonical mode, the `ConductorClient` automatically installs an OkHttp interceptor that records `http_api_client_request_seconds`. In legacy mode the interceptor is skipped entirely since there is nothing to record.

### Manual Wiring

For advanced use cases where you need fine-grained control over which listeners are registered where, or you want to mix the metrics collector with custom event listeners, use `withHttpMetrics` instead of `withMetricsCollector` on the builder. This installs only the HTTP interceptor (for `http_api_client_request_seconds`, `task_result_size_bytes`, `workflow_input_size_bytes`) without triggering automatic listener registration on downstream clients:

```java
import com.netflix.conductor.client.metrics.prometheus.AbstractPrometheusMetricsCollector;
import com.netflix.conductor.client.metrics.prometheus.MetricsCollectorFactory;

AbstractPrometheusMetricsCollector metricsCollector = MetricsCollectorFactory.create();
metricsCollector.startServer(); // http://localhost:9991/metrics

ConductorClient client = ConductorClient.builder()
        .basePath("http://conductor-server:8080/api")
        .withHttpMetrics(metricsCollector)
        .build();

TaskClient taskClient = new TaskClient(client);
taskClient.registerListener(metricsCollector);
taskClient.registerTaskRunnerListener(metricsCollector);

TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
        .withThreadCount(10)
        .withMetricsCollector(metricsCollector)
        .build();

configurer.init();

WorkflowClient workflowClient = new WorkflowClient(client);
workflowClient.registerListener(metricsCollector);
```

Use this approach when you need to register the metrics collector on some clients but not others, or when mixing in custom event listeners alongside the metrics collector. With `withHttpMetrics`, none of the listener registrations happen automatically -- you choose exactly which clients get which listeners.

### How Auto-Registration Works

When a `MetricsCollector` is passed to `ConductorClient.Builder.withMetricsCollector()`:

1. The `ConductorClient` installs an OkHttp interceptor that records `http_api_client_request_seconds`. In legacy mode the interceptor is not installed because `getApiClientMetrics()` returns `ApiClientMetrics.NOOP`.
2. `TaskClient` detects the collector from the `ConductorClient` it receives and calls `registerListener` and `registerTaskRunnerListener` on itself.
3. `WorkflowClient` detects the collector from the `ConductorClient` it receives and calls `registerListener` on itself.
4. `TaskRunnerConfigurer.Builder.build()` detects the collector from the `TaskClient`'s `ConductorClient` and registers task-runner events automatically, unless `withMetricsCollector` was called explicitly on the builder.

All registrations are idempotent. If you call both `withMetricsCollector` on the builder and `registerListener` manually with the same collector, events are not duplicated.

To install only the HTTP interceptor (step 1) without triggering automatic listener registration (steps 2-4), use `withHttpMetrics` instead of `withMetricsCollector` on the builder. See [Manual Wiring](#manual-wiring) above.

The collector exposes Prometheus text format from the embedded HTTP server. Metrics are created lazily, so a metric family appears after the corresponding worker or client event has occurred.

## Tuning

Canonical mode enables several hot-path behaviors that legacy mode leaves off by default to preserve zero-overhead backward compatibility. These can be toggled per-collector or overridden per-`TaskRunnerConfigurer`.

### Collector-level flags

Call these on `MetricsCollector` (or `AbstractPrometheusMetricsCollector`) before passing it to the builder:

| Flag | Canonical default | Legacy default | Effect |
|---|---|---|---|
| `setAutoWiringEnabled(boolean)` | `true` | `false` | When `true`, `TaskClient` / `WorkflowClient` constructors auto-register the collector as an event listener. When `false`, callers must register manually. |
| `setActiveWorkersTrackingEnabled(boolean)` | `true` | `false` | When `true`, `TaskRunner` publishes an `ActiveWorkersChanged` event on every task start and finish, driving the `active_workers` gauge. Adds two async event dispatches per task execution. |
| `setDiagnosticEventsEnabled(boolean)` | `true` | `false` | When `true`, `TaskRunner` publishes `TaskPaused` and `TaskExecutionQueueFull` per poll cycle, and `TaskClient` publishes `TaskAckFailure` / `TaskAckError` on ack outcomes. |

### TaskRunnerConfigurer overrides

These builder methods override the collector defaults for a single configurer instance:

| Builder method | Overrides |
|---|---|
| `withActiveWorkersTracking(boolean)` | `MetricsCollector.isActiveWorkersTrackingEnabled()` |
| `withDiagnosticEvents(boolean)` | `MetricsCollector.isDiagnosticEventsEnabled()` |

Example: opt a legacy-mode deployment into active-worker tracking without switching to canonical metrics:

```java
MetricsCollector collector = new LegacyPrometheusMetricsCollector();
collector.setAutoWiringEnabled(true);

TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
        .withActiveWorkersTracking(true)
        .build();
```

### Event dispatch threading

Events are dispatched asynchronously on a single shared daemon thread (`conductor-event-dispatch`). This avoids contention with the application's `ForkJoinPool.commonPool()`. Metrics collector listeners (counter increments, timer recordings) are lock-free and sub-microsecond, so the single thread keeps up under normal load. Custom listeners registered via `EventDispatcher.register()` must be non-blocking; a slow listener will delay delivery of all events across the process.

## Legacy and Canonical Modes

The Java SDK currently supports two mutually exclusive metric surfaces:

- **Legacy metrics** preserve the original Java SDK names and the `type` task label. This is the default.
- **Canonical metrics** use the cross-SDK worker metric catalog with harmonized names, labels, units, and bucket boundaries.

`MetricsCollectorFactory.create()` reads `WORKER_CANONICAL_METRICS` when the collector is created:

| Environment variable | Values | Effect |
|---|---|---|
| `WORKER_CANONICAL_METRICS` | `true`, `1`, or `yes` (case-insensitive, surrounding whitespace ignored) | Selects `CanonicalPrometheusMetricsCollector`. |
| `WORKER_CANONICAL_METRICS` | unset, blank, `false`, `0`, `no`, or any other value | Selects `LegacyPrometheusMetricsCollector`. |

Only one implementation is active at a time. The Java SDK does not dual-emit legacy and canonical names from the same collector. Restart workers after changing `WORKER_CANONICAL_METRICS` so the factory creates the desired collector.

`WORKER_LEGACY_METRICS` is reserved for a future default-flip phase and is not currently read by the Java SDK factory.

## Legacy Metrics Catalog

Legacy mode emits the original Java SDK worker metrics. The table lists the Micrometer meter names registered by the collector. In Prometheus scrape output, Micrometer may add suffixes such as `_total`, `_seconds_count`, `_seconds_sum`, and `_seconds_max` depending on the meter type.

| Meter | Micrometer type | Labels | Meaning |
|---|---|---|---|
| `poll_started` | Counter | `type` | Count of poll attempts for a task type. |
| `poll_success` | Timer | `type` | Duration of successful poll requests. |
| `poll_failure` | Timer | `type` | Duration of failed poll requests. |
| `task_execution_started` | Counter | `type` | Count of tasks dispatched to worker code. |
| `task_execution_completed` | Timer | `type` | Duration of successful task executions. |
| `task_execution_failure` | Timer | `type` | Duration of failed task executions. |

Legacy mode intentionally does not emit canonical-only metrics for task update latency, task ack failures, queue saturation, paused workers, uncaught worker thread exceptions, external payload usage, task result size, workflow input size, workflow start errors, active workers, or HTTP API client latency.

## Canonical Metrics Catalog

Canonical mode emits the harmonized Java SDK metric surface. Time metrics use seconds and the standard canonical bucket boundaries. Size metrics use bytes and the standard canonical size bucket boundaries. Exception labels use bounded exception type names, not exception messages or stack traces.

### Counters

| Meter | Labels | Meaning |
|---|---|---|
| `task_poll_total` | `taskType` | Incremented each time a worker issues a poll request. |
| `task_execution_started_total` | `taskType` | Incremented when a polled task is dispatched to the worker function. |
| `task_poll_error_total` | `taskType`, `exception` | Incremented when polling fails with a client-side exception. |
| `task_execute_error_total` | `taskType`, `exception` | Incremented when worker code throws while executing a task. |
| `task_update_error_total` | `taskType`, `exception` | Incremented when reporting a task result back to Conductor fails. |
| `task_ack_failed_total` | `taskType` | Incremented when an explicit task ack response is unsuccessful. The internal task runner uses batch poll responses as ack and may not emit this during normal polling. |
| `task_ack_error_total` | `taskType`, `exception` | Incremented when an explicit task ack call throws. The internal task runner uses batch poll responses as ack and may not emit this during normal polling. |
| `task_execution_queue_full_total` | `taskType` | Incremented when a poll cycle is skipped because all worker threads are busy (zero permits available). |
| `task_paused_total` | `taskType` | Incremented when a worker is paused and skips acting on a poll. |
| `thread_uncaught_exceptions_total` | `exception` | Incremented when a worker thread raises an uncaught exception. |
| `external_payload_used_total` | `entityName`, `operation`, `payloadType` | Incremented when external payload storage is used for task or workflow payloads. |
| `workflow_start_error_total` | `workflowType`, `exception` | Incremented when starting a workflow fails client-side. |

### Time Metrics

| Meter | Labels | Meaning |
|---|---|---|
| `task_poll_time_seconds` | `taskType`, `status` | Poll request latency. `status` is `SUCCESS` or `FAILURE`. |
| `task_execute_time_seconds` | `taskType`, `status` | Worker function execution latency. `status` is `SUCCESS` or `FAILURE`. |
| `task_update_time_seconds` | `taskType`, `status` | Latency for reporting a task result back to Conductor. `status` is `SUCCESS` or `FAILURE`. |
| `http_api_client_request_seconds` | `method`, `uri`, `status` | Latency of HTTP requests made by the API client. `status` is the HTTP status code as a string, or `0` when no response status is available. |

Time metrics use these service-level objective buckets, in seconds:

```text
0.001, 0.005, 0.010, 0.025, 0.050, 0.100, 0.250, 0.500, 1, 2.5, 5, 10
```

The `uri` label for `http_api_client_request_seconds` uses the path template (e.g. `/workflow/{workflowId}`, `/tasks/poll/batch/{taskType}`) rather than the resolved path. This keeps the label space bounded regardless of how many unique workflow or task IDs are processed.

### Size Metrics

| Meter | Labels | Meaning |
|---|---|---|
| `task_result_size_bytes` | `taskType` | Serialized task result output size, captured from `RequestBody.contentLength()` of the outbound `POST /tasks` (or `POST /tasks/update-v2`) request. `taskType` is empty when the caller used the single-argument `TaskClient.updateTask(TaskResult)` overload. |
| `workflow_input_size_bytes` | `workflowType`, `version` | Serialized workflow input size, captured from `RequestBody.contentLength()` of the outbound `POST /workflow` request. `version` is an empty string when the workflow version is absent. |

Both histograms are populated at wire time by the `ApiClientMetrics` OkHttp interceptor, reading a `PayloadKind` tag attached by `TaskClient`/`WorkflowClient`. The byte count is read off the request body the HTTP layer is about to send, so no extra JSON serialization is needed.

Size metrics use these service-level objective buckets, in bytes:

```text
100, 1000, 10000, 100000, 1000000, 10000000
```

### Gauges

| Meter | Labels | Meaning |
|---|---|---|
| `active_workers` | `taskType` | Current number of worker threads actively executing tasks. |

### Micrometer `_max` sidecars

Micrometer publishes a `*_max` Gauge alongside every Timer and DistributionSummary. These appear in scrape output as e.g. `task_poll_time_seconds_max`, `task_result_size_bytes_max`. The `_max` tracks the maximum observed value within the current reporting interval. This is a Micrometer artifact, not part of the canonical catalog; it is harmless and can be ignored by dashboards that don't use it.

## Labels

| Label | Used by | Values |
|---|---|---|
| `type` | Legacy worker metrics | Task definition name. Replaced by `taskType` in canonical mode. |
| `taskType` | Canonical worker metrics | Task definition name. |
| `workflowType` | Workflow metrics | Workflow definition name. |
| `version` | `workflow_input_size_bytes` | Workflow version as a string. Empty string when the version is absent. |
| `status` | Task time metrics | `SUCCESS` or `FAILURE`. For `http_api_client_request_seconds`, the HTTP status code as a string, or `0` when no response status is available. |
| `exception` | Canonical error counters | Exception type name, such as `SocketTimeoutException`. |
| `entityName` | `external_payload_used_total` | Task type or workflow name associated with the external payload. |
| `operation` | `external_payload_used_total` | External payload operation, such as `READ` or `WRITE`. |
| `payloadType` | `external_payload_used_total` | Payload type, such as `TASK_INPUT`, `TASK_OUTPUT`, `WORKFLOW_INPUT`, or `WORKFLOW_OUTPUT`. |
| `method` | HTTP metrics | HTTP verb. |
| `uri` | HTTP metrics | Path template from the Java HTTP client (e.g. `/workflow/{workflowId}`). Resolved identifiers are not included, keeping cardinality bounded. |

## Migration from 4.0.x

The 4.0.x entry point `PrometheusMetricsCollector` is retained as a deprecated alias for `LegacyPrometheusMetricsCollector`, so existing code keeps compiling and emits the same six legacy meter names byte-for-byte. Use this table to decide what to do at upgrade time:

| 4.0.x usage                                                | 4.x replacement                                                                                          |
|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `new PrometheusMetricsCollector()`                         | `MetricsCollectorFactory.create()` (or `MetricsBundle.create()`) — env-var-selected legacy or canonical |
| `new PrometheusMetricsCollector()` (force legacy names)    | `new LegacyPrometheusMetricsCollector()`                                                                 |
| `metricsCollector.startServer(port, "/metrics")`           | unchanged — still on `AbstractPrometheusMetricsCollector`                                                |

The shim is intentionally pinned to `LegacyPrometheusMetricsCollector` rather than `MetricsCollectorFactory.create()`, so an upgrader who already has `WORKER_CANONICAL_METRICS=true` set in their environment is not silently flipped to the canonical metric surface just by upgrading the SDK. Switch to `MetricsCollectorFactory.create()` when you are ready to opt into env-var-driven selection.

## Migration from Legacy to Canonical

Switching to canonical metrics is an explicit metrics-surface cutover. Enable `WORKER_CANONICAL_METRICS=true` in a lower environment first, then update dashboards, recording rules, and alerts before enabling it in production.

Important migration changes:

- Legacy task labels use `type`; canonical task labels use `taskType`.
- Legacy success and failure timings are split across different meter names, such as `poll_success` and `poll_failure`; canonical timings use one metric name with `status=SUCCESS` or `status=FAILURE`.
- Legacy execution timings use `task_execution_completed` and `task_execution_failure`; canonical mode uses `task_execute_time_seconds` with `status`.
- Canonical error counters add an `exception` label that contains the exception type name.
- Canonical mode adds metrics that legacy mode never emitted, including task update latency, task result size, workflow input size, workflow start errors, active worker counts, and HTTP API client request latency.
- Canonical and legacy collectors are mutually exclusive. During a migration, compare scrape output by running separate worker instances or environments with and without `WORKER_CANONICAL_METRICS=true`.

Common legacy-to-canonical replacements:

| Legacy meter | Canonical replacement |
|---|---|
| `poll_started{type}` | `task_poll_total{taskType}` |
| `poll_success{type}` | `task_poll_time_seconds{taskType,status="SUCCESS"}` |
| `poll_failure{type}` | `task_poll_time_seconds{taskType,status="FAILURE"}` and `task_poll_error_total{taskType,exception}` |
| `task_execution_started{type}` | `task_execution_started_total{taskType}` |
| `task_execution_completed{type}` | `task_execute_time_seconds{taskType,status="SUCCESS"}` |
| `task_execution_failure{type}` | `task_execute_time_seconds{taskType,status="FAILURE"}` and `task_execute_error_total{taskType,exception}` |

## Troubleshooting

### Metrics Are Empty

- Verify that the collector is wired into the client. The simplest check: was `withMetricsCollector` called on `ConductorClient.Builder`, or was `MetricsCollectorFactory.create()` called and registered manually?
- Verify workers have polled or executed tasks. Metrics are created lazily when the relevant event occurs.
- Confirm the scrape endpoint is reachable at the expected host and port.

### Missing HTTP or Workflow Metrics

- `http_api_client_request_seconds` requires the HTTP interceptor, which is installed automatically when `withMetricsCollector` is called on the builder. In canonical mode the interceptor records request latency; in legacy mode it is skipped because `getApiClientMetrics()` returns `ApiClientMetrics.NOOP`.
- `task_result_size_bytes` and `workflow_input_size_bytes` likewise require the HTTP interceptor — they are recorded at wire time from `RequestBody.contentLength()` for requests tagged with a `PayloadKind`. If the `ConductorClient` is built without `withMetricsCollector`, those histograms will be empty even when canonical mode is enabled. (`workflow_start_error_total` and workflow-side `external_payload_used_total` continue to flow through `workflowClient.registerListener(metricsCollector)`.)
- `task_ack_failed_total` and `task_ack_error_total` require `taskClient.registerTaskRunnerListener(metricsCollector)`. This is automatic when using `withMetricsCollector` on the builder.

### High Cardinality

- The `uri` label on `http_api_client_request_seconds` uses the path template, so it is bounded by the number of distinct API endpoints (not by request volume or unique IDs). The interceptor falls back to the resolved path for requests that are not tagged with a template, which may be unbounded.
- Prefer canonical mode for bounded `exception` labels. Legacy mode does not emit exception-labeled error counters.
- Avoid embedding user identifiers or unbounded values in task type, workflow type, or external payload labels.

## Detailed Technical Notes — Unreleased

Implementation details, internal design decisions, and migration notes for the
unreleased metrics harmonization work. For a summary, see the project
[CHANGELOG](../CHANGELOG.md).

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
  - Removed `WorkflowInputPayloadSizeEvent` publish from `checkAndUploadToExternalStorage` and `TaskResultPayloadSizeEvent` publish from `evaluateAndUploadLargePayload`. These events never reached any listener on `main`: the `PrometheusMetricsCollector.consume()` handlers were unimplemented `//TODO` stubs, and neither `workflowClient.registerListener()` nor `taskClient.registerListener()` was called in any SDK wiring path. Payload-size observability is now provided unconditionally by the `PayloadKind` tag on outbound requests, recorded at wire time by the `ApiClientMetrics` OkHttp interceptor as `task_result_size_bytes{taskType}` and `workflow_input_size_bytes{workflowType,version}`, without double-serialization or manual listener registration.
  - **Transitive dependency change**: `conductor-client-metrics` now exposes `micrometer-registry-prometheus` as an `api` (transitive) dependency instead of `implementation`. Consumers with their own Micrometer version on the classpath should verify compatibility with 1.15.1.
  - **Graceful shutdown improvement**: `TaskRunner` now catches `RejectedExecutionException` when submitting a polled task to the executor during shutdown, logging a descriptive message and releasing the semaphore permit. Previously, the same exception was caught by a broad `catch (Throwable t)` that only logged the message string and did not release the permit. The task outcome is unchanged in both cases (polled but never executed; times out server-side).
  - **OkHttp request tagging**: Outbound requests are tagged with `ConductorClient.PathTemplateTag` (not `String.class`) so the metrics interceptor can read the URI template without conflicting with user-installed interceptors.
  - **ListenerRegister thread safety**: Registration is now idempotent. `EventDispatcher` uses `ConcurrentHashMap` with `putIfAbsent` keyed by the listener instance, so registering the same listener twice is a no-op without external synchronization.
  - **Diagnostic events gating**: Per-poll-cycle events (`TaskPaused`, `TaskExecutionQueueFull`) and ack diagnostic events (`TaskAckFailure`, `TaskAckError`) are now gated behind `MetricsCollector.isDiagnosticEventsEnabled()` (canonical: `true`, legacy: `false`). When disabled, no event objects are allocated on the hot path.

### Deprecated

- `TaskClient.ack(String taskId, String workerId)` is deprecated in favor of `ack(String taskType, String taskId, String workerId)`. The 2-arg form lacks the `taskType` parameter, so canonical ack metrics (`task_ack_failed_total{taskType}`, `task_ack_error_total{taskType}`) cannot be labeled. The deprecated overload delegates with `taskType=null`, preserving identical runtime behavior — no metrics are emitted and no try-catch wrapping takes effect.

- `com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector` is deprecated and is now a thin alias for `LegacyPrometheusMetricsCollector`. Existing 4.0.x callers that wrote `new PrometheusMetricsCollector()` continue to compile and emit the same six legacy meter names (`poll_started{type}`, `poll_success{type}`, `poll_failure{type}`, `task_execution_started{type}`, `task_execution_completed{type}`, `task_execution_failure{type}`) byte-for-byte. The shim deliberately pins to the legacy collector rather than `MetricsCollectorFactory.create()`, so upgraders with `WORKER_CANONICAL_METRICS=true` already set in their environment are not silently flipped to the canonical surface. New code should use `MetricsCollectorFactory.create()` or `MetricsBundle.create()` to opt into env-var-driven selection.