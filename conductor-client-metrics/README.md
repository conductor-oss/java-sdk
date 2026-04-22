# Conductor Client Metrics

**Status: Incubating.**

Provides Prometheus-format metrics for the Conductor Java client. Once wired
in, the SDK exposes the cross-SDK canonical worker-telemetry set described in
[`longrunning-wfstest/sdk-metrics-harmonization.md`](../../longrunning-wfstest/sdk-metrics-harmonization.md)
alongside the legacy Java metric names (so dashboards built against either
naming scheme keep working during the deprecation window).

---

## Quick start

### 1. Register the collector

```java
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;

var metrics = new PrometheusMetricsCollector();
metrics.startServer(); // exposes /metrics on :9991 by default

var configurer = new TaskRunnerConfigurer.Builder(taskClient, List.of(myWorker))
        .withListener(metrics) // implements MetricsCollector
        .build();
configurer.init();
```

### 2. Optional: enable the HTTP API-client Histogram

To also capture `http_api_client_request_seconds` for every outbound call the
SDK makes to the Conductor server, install the bundled OkHttp3 interceptor:

```java
import com.netflix.conductor.client.metrics.prometheus.ApiClientMetricsInterceptor;
import com.netflix.conductor.client.metrics.prometheus.PrometheusApiClientMetrics;

var apiMetrics = new PrometheusApiClientMetrics(metrics.getRegistry());

ApiClient client = ApiClient.builder()
        .basePath("https://conductor.example/api")
        .credentials(keyId, secret)
        .configureOkHttp(b -> b.addInterceptor(new ApiClientMetricsInterceptor(apiMetrics)))
        .build();
```

Passing `metrics.getRegistry()` keeps everything on the same `/metrics`
scrape surface. If you want `http_api_client_request_seconds` in its own
registry, use the no-arg `new PrometheusApiClientMetrics()` constructor and
scrape `apiMetrics.getRegistry()` separately.

---

## Metrics emitted

All metrics end up on the Prometheus scrape surface started by
`PrometheusMetricsCollector#startServer()` (default `:9991/metrics`). Both
canonical and legacy names are emitted during Phase 1 of the harmonization
rollout.

### Canonical (use these going forward)

| Name | Type | Labels |
|---|---|---|
| `task_poll_total` | Counter | `taskType` |
| `task_execution_started_total` | Counter | `type, taskType` (see note below) |
| `task_poll_error_total` | Counter | `taskType, exception` |
| `task_execute_error_total` | Counter | `taskType, exception` |
| `task_update_error_total` | Counter | `taskType, exception` |
| `task_ack_error_total` | Counter | `taskType, exception` |
| `task_ack_failed_total` | Counter | `taskType` |
| `task_execution_queue_full_total` | Counter | `taskType` |
| `task_paused_total` | Counter | `taskType` |
| `thread_uncaught_exceptions_total` | Counter | `exception` |
| `external_payload_used_total` | Counter | `entityName, operation, payload_type` |
| `workflow_start_error_total` | Counter | `workflowType, exception` |
| `task_poll_time_seconds` | Histogram | `taskType, status` |
| `task_execute_time_seconds` | Histogram | `taskType, status` |
| `task_update_time_seconds` | Histogram | `taskType, status` |
| `http_api_client_request_seconds` | Histogram | `method, uri, status` |
| `task_result_size_bytes` | Gauge | `taskType` |
| `workflow_input_size_bytes` | Gauge | `workflowType, version` |

All Histograms use the shared bucket set
`(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10)` so that
cross-SDK dashboards can share a single `le` axis.

Label conventions:

- `taskType` / `workflowType` — string name of the task / workflow.
- `status` on timing histograms — `"SUCCESS"` or `"FAILURE"`.
- `exception` on error counters — `e.getClass().getSimpleName()` of the root
  cause (wrapper exceptions like `ExecutionException` /
  `CompletionException` / `InvocationTargetException` are unwrapped by one
  level). This keeps label cardinality bounded.
- `uri` on `http_api_client_request_seconds` — encoded URL path as OkHttp
  sees it. Note that the generated API client has already interpolated path
  parameters by this point, so operators who care about cardinality should
  relabel at scrape time.

### Deprecated legacy names (kept emitting during Phase 1)

| Name | Replacement |
|---|---|
| `poll_started` | `task_poll_total` |
| `poll_success` | `task_poll_time_seconds{status="SUCCESS"}` |
| `poll_failure` | `task_poll_time_seconds{status="FAILURE"}` + `task_poll_error_total` |
| `task_execution_started` | `task_execution_started_total` |
| `task_execution_completed` | `task_execute_time_seconds{status="SUCCESS"}` |
| `task_execution_failure` | `task_execute_time_seconds{status="FAILURE"}` + `task_execute_error_total` |

The legacy names use a `type` label; the canonical names use `taskType`.
Both are emitted simultaneously so existing dashboards keep working.

**Exception:** `task_execution_started` / `task_execution_started_total`
cannot be dual-registered as separate meters because Micrometer's Prometheus
exporter auto-appends `_total` to Counter names, which would cause both
meters to resolve to the same Prometheus metric family with incompatible
label sets. Instead, this SDK emits a single counter carrying **both** `type`
and `taskType` labels with identical values; legacy queries on
`{type=...}` and canonical queries on `{taskType=...}` both resolve to the
same time series. See
[`longrunning-wfstest/sdk-metrics-harmonization.md`](../../longrunning-wfstest/sdk-metrics-harmonization.md)
§3.3 and §4 for the full rationale.

See the harmonization doc for the full catalog and rollout schedule.

---

## Querying the histograms

```promql
# 95th-percentile poll latency across all worker replicas
histogram_quantile(
  0.95,
  sum by (le, taskType) (
    rate(task_poll_time_seconds_bucket[5m])
  )
)

# error rate per task type
sum by (taskType, exception) (
  rate(task_execute_error_total[5m])
)

# outbound HTTP call latency to the Conductor server
histogram_quantile(
  0.99,
  sum by (le, method) (
    rate(http_api_client_request_seconds_bucket[5m])
  )
)
```

---

## Plugging in a custom backend

If you don't want to use Prometheus, implement
[`MetricsCollector`](../conductor-client/src/main/java/com/netflix/conductor/client/metrics/MetricsCollector.java)
(which extends `TaskRunnerEventsListener`, `WorkflowClientListener`, and
`TaskClientListener`) and register your implementation with the
`TaskRunnerConfigurer` via `withListener(...)`. All harmonization events
have default no-op methods, so older listener implementations compile
unchanged.

For HTTP-level metrics outside Prometheus, implement
[`ApiClientMetrics`](../conductor-client/src/main/java/com/netflix/conductor/client/metrics/ApiClientMetrics.java)
and wrap it in `ApiClientMetricsInterceptor`.
