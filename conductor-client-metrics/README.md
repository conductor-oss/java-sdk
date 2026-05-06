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

Create a Prometheus collector, start its scrape endpoint, and wire the collector into the clients and task runner:

```java
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.metrics.ApiClientMetrics;
import com.netflix.conductor.client.metrics.prometheus.AbstractPrometheusMetricsCollector;
import com.netflix.conductor.client.metrics.prometheus.ApiClientMetricsInterceptor;
import com.netflix.conductor.client.metrics.prometheus.MetricsCollectorFactory;

AbstractPrometheusMetricsCollector metricsCollector = MetricsCollectorFactory.create();
metricsCollector.startServer(); // http://localhost:9991/metrics

ApiClientMetrics apiClientMetrics = metricsCollector.getApiClientMetrics();
ConductorClient.Builder<?> clientBuilder = ConductorClient.builder()
        .basePath("http://conductor-server:8080/api");
if (apiClientMetrics != ApiClientMetrics.NOOP) {
    clientBuilder.configureOkHttp(builder ->
            builder.addInterceptor(new ApiClientMetricsInterceptor(apiClientMetrics)));
}
ConductorClient client = clientBuilder.build();

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

To expose metrics on a custom port or path:

```java
AbstractPrometheusMetricsCollector metricsCollector = MetricsCollectorFactory.create();
metricsCollector.startServer(8080, "/custom-metrics");
```

The collector exposes Prometheus text format from the embedded HTTP server. Metrics are created lazily, so a metric family appears after the corresponding worker or client event has occurred.

`withMetricsCollector(metricsCollector)` registers task-runner events for worker polling, task execution, task result update, queue saturation, paused workers, uncaught worker thread exceptions, and active worker counts.

The additional registrations complete the canonical metrics surface:

- `ApiClientMetricsInterceptor` records every HTTP request made through the `ConductorClient` as `http_api_client_request_seconds`. Canonical mode returns a Prometheus-backed `ApiClientMetrics`; legacy mode returns `ApiClientMetrics.NOOP`, so the interceptor is skipped when there is nothing to record.
- `taskClient.registerListener(metricsCollector)` registers task-client events for task payload metrics, including `task_result_size_bytes` and task-side `external_payload_used_total`.
- `taskClient.registerTaskRunnerListener(metricsCollector)` registers task-runner events emitted by `TaskClient` itself, currently explicit task ack failure/error events for `task_ack_failed_total` and `task_ack_error_total` when a task type is known.
- `workflowClient.registerListener(metricsCollector)` registers workflow-client events for `workflow_input_size_bytes`, workflow-side `external_payload_used_total`, and `workflow_start_error_total`.

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
| `task_execution_queue_full_total` | `taskType` | Incremented when the worker execution queue is saturated. |
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

The `uri` label for `http_api_client_request_seconds` currently uses the request path available to the Java HTTP client. If the path contains interpolated identifiers, those values may appear in the label.

### Size Metrics

| Meter | Labels | Meaning |
|---|---|---|
| `task_result_size_bytes` | `taskType` | Serialized task result output size. |
| `workflow_input_size_bytes` | `workflowType`, `version` | Serialized workflow input size. `version` is an empty string when the workflow version is absent. |

Size metrics use these service-level objective buckets, in bytes:

```text
100, 1000, 10000, 100000, 1000000, 10000000
```

### Gauges

| Meter | Labels | Meaning |
|---|---|---|
| `active_workers` | `taskType` | Current number of worker threads actively executing tasks. |

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
| `uri` | HTTP metrics | Request path from the Java HTTP client. May contain interpolated identifiers. |

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

- Verify that `MetricsCollectorFactory.create()` is called and the collector is wired into `TaskRunnerConfigurer`, `TaskClient`, and `WorkflowClient` as shown in the Usage section.
- Verify workers have polled or executed tasks. Metrics are created lazily when the relevant event occurs.
- Confirm the scrape endpoint is reachable at the expected host and port.

### Missing HTTP or Workflow Metrics

- `http_api_client_request_seconds` requires adding `ApiClientMetricsInterceptor` to the `ConductorClient` OkHttp builder. Legacy mode returns `ApiClientMetrics.NOOP`, so the interceptor is skipped and no HTTP metrics are emitted.
- `workflow_input_size_bytes`, `workflow_start_error_total`, and workflow-side `external_payload_used_total` require `workflowClient.registerListener(metricsCollector)`.
- `task_ack_failed_total` and `task_ack_error_total` require `taskClient.registerTaskRunnerListener(metricsCollector)`.

### High Cardinality

- Watch the `uri` label on `http_api_client_request_seconds`. The Java HTTP client may include interpolated path identifiers in the request path.
- Prefer canonical mode for bounded `exception` labels. Legacy mode does not emit exception-labeled error counters.
- Avoid embedding user identifiers or unbounded values in task type, workflow type, or external payload labels.
