# Conductor Client Metrics

The `conductor-client-metrics` module records Micrometer metrics for Java SDK clients and workers. It helps operators monitor worker polling, task execution, task result updates, payload sizes, workflow starts, and HTTP client latency.

The SDK **should not start a metrics web server**. Metrics are published into a Micrometer `MeterRegistry` that you supply, and exposing them for scraping is the responsibility of the embedding application:

- **Spring Boot apps** hand in the application's `MeterRegistry` (the one backing `/actuator/prometheus`), so SDK metrics appear alongside the app's own metrics. This module ships a Spring Boot auto-configuration that does this automatically.
- **Plain Java apps** create a `PrometheusMeterRegistry` (or any other registry), pass it to the collector, and expose it however they like (e.g. serve `registry.scrape()` from an HTTP endpoint they own).

> **Deprecated:** the no-argument `new PrometheusMetricsCollector()` plus `startServer()` still spin up a minimal embedded scrape server for backward compatibility, but both are deprecated and will be removed in a future major release. Prefer supplying a `MeterRegistry`.

## Installation

Add the metrics module to the worker application:

Use the published version from [Maven Central](https://search.maven.org/artifact/org.conductoross/conductor-client-metrics).

```groovy
dependencies {
    implementation 'org.conductoross:conductor-client-metrics:<VERSION>'
}
```

## Usage

### Spring Boot (recommended)

Add this module and Spring Boot Actuator with the Prometheus registry to a Spring Boot worker app:

```groovy
dependencies {
    implementation 'org.conductoross:conductor-client-metrics:5.1.0'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

That's it. When a `MeterRegistry` bean is present, `ConductorMetricsAutoConfiguration` creates a `MetricsCollector` bound to it, and the Conductor Spring auto-configuration wires that collector into the client and task runner. SDK metrics then show up on the app's existing scrape endpoint (by default `/actuator/prometheus`).

To serve them at a custom path/port, use the standard Actuator properties, e.g.:

```yaml
server:
  port: 9991
management:
  endpoints:
    web:
      base-path: /
      exposure:
        include: health,prometheus
      path-mapping:
        prometheus: metrics   # scrape endpoint at /metrics
```

To provide your own collector (or a non-Prometheus registry), just declare a `MetricsCollector` bean; the auto-configuration backs off (`@ConditionalOnMissingBean`).

### Plain Java

Create a registry, pass it to the collector, and expose the registry yourself. Passing the collector to `ConductorClient.Builder` auto-registers all downstream clients and the task runner as listeners.

```java
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
PrometheusMetricsCollector metricsCollector = new PrometheusMetricsCollector(registry);

ConductorClient client = ConductorClient.builder()
        .basePath("http://conductor-server:8080/api")
        .withMetricsCollector(metricsCollector)
        .build();

TaskClient taskClient = new TaskClient(client);
WorkflowClient workflowClient = new WorkflowClient(client);

TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
        .withThreadCount(10)
        .build();
configurer.init();

// Expose the metrics from an HTTP endpoint you own, e.g.:
// var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(9991), 0);
// server.createContext("/metrics", exchange -> {
//     byte[] body = registry.scrape().getBytes();
//     exchange.sendResponseHeaders(200, body.length);
//     try (var os = exchange.getResponseBody()) { os.write(body); }
// });
// server.start();
```

The client builder accepts the usual timeouts, SSL, authentication, and other options alongside `withMetricsCollector` -- none of them change how metrics wiring works.

### How Auto-Registration Works

When a `MetricsCollector` is passed to `ConductorClient.Builder.withMetricsCollector()`:

1. The `ConductorClient` installs an OkHttp interceptor that records `http_api_client_request_seconds`, `task_result_size_bytes`, and `workflow_input_size_bytes`.
2. `TaskClient` detects the collector from the `ConductorClient` it receives and calls `registerListener` and `registerTaskRunnerListener` on itself.
3. `WorkflowClient` detects the collector from the `ConductorClient` it receives and calls `registerListener` on itself.
4. `TaskRunnerConfigurer.Builder.build()` detects the collector from the `TaskClient`'s `ConductorClient` and registers task-runner events automatically, unless `withMetricsCollector` was called explicitly on the builder.

All registrations are idempotent. If you call both `withMetricsCollector` on the builder and `registerListener` manually with the same collector, events are not duplicated.

The collector exposes Prometheus text format from the embedded HTTP server. Metrics are created lazily, so a metric family appears after the corresponding worker or client event has occurred.

### Manual Wiring

For advanced use cases where you need fine-grained control over which listeners are registered where, or you want to mix the metrics collector with custom event listeners, create the `ConductorClient` without `withMetricsCollector` and register listeners explicitly:

```java
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
PrometheusMetricsCollector metricsCollector = new PrometheusMetricsCollector(registry);

ConductorClient client = ConductorClient.builder()
        .basePath("http://conductor-server:8080/api")
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

Note that manual wiring does not install the OkHttp interceptor for `http_api_client_request_seconds`, `task_result_size_bytes`, or `workflow_input_size_bytes`. Use `withMetricsCollector` on the builder for those metrics.

### Event Dispatch Threading

Events are dispatched asynchronously on a single shared daemon thread (`conductor-event-dispatch`). This avoids contention with the application's `ForkJoinPool.commonPool()`. Metrics collector listeners (counter increments, timer recordings) are lock-free and sub-microsecond, so the single thread keeps up under normal load. Custom listeners registered via `EventDispatcher.register()` must be non-blocking; a slow listener will delay delivery of all events across the process.

## Metrics Catalog

Time metrics use seconds and standard bucket boundaries. Size metrics use bytes and standard size bucket boundaries. Exception labels use bounded exception type names, not exception messages or stack traces.

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

### Micrometer `_max` Sidecars

Micrometer publishes a `*_max` Gauge alongside every Timer and DistributionSummary. These appear in scrape output as e.g. `task_poll_time_seconds_max`, `task_result_size_bytes_max`. The `_max` tracks the maximum observed value within the current reporting interval. This is a Micrometer artifact, not part of the metric catalog; it is harmless and can be ignored by dashboards that don't use it.

## Labels

| Label | Used by | Values |
|---|---|---|
| `taskType` | Worker metrics | Task definition name. |
| `workflowType` | Workflow metrics | Workflow definition name. |
| `version` | `workflow_input_size_bytes` | Workflow version as a string. Empty string when the version is absent. |
| `status` | Task time metrics | `SUCCESS` or `FAILURE`. For `http_api_client_request_seconds`, the HTTP status code as a string, or `0` when no response status is available. |
| `exception` | Error counters | Exception type name, such as `SocketTimeoutException`. |
| `entityName` | `external_payload_used_total` | Task type or workflow name associated with the external payload. |
| `operation` | `external_payload_used_total` | External payload operation, such as `READ` or `WRITE`. |
| `payloadType` | `external_payload_used_total` | Payload type, such as `TASK_INPUT`, `TASK_OUTPUT`, `WORKFLOW_INPUT`, or `WORKFLOW_OUTPUT`. |
| `method` | HTTP metrics | HTTP verb. |
| `uri` | HTTP metrics | Path template from the Java HTTP client (e.g. `/workflow/{workflowId}`). Resolved identifiers are not included, keeping cardinality bounded. |

## Troubleshooting

### Metrics Are Empty

- Verify that the collector is wired into the client. In Spring Boot, confirm a `MeterRegistry` bean exists (add `spring-boot-starter-actuator` + `micrometer-registry-prometheus`). In plain Java, confirm `withMetricsCollector` was called on `ConductorClient.Builder`.
- Verify workers have polled or executed tasks. Metrics are created lazily when the relevant event occurs.
- Confirm the scrape endpoint is reachable at the expected host and port (Actuator's `/actuator/prometheus` by default, or whatever your app exposes).

### Missing HTTP or Size Metrics

- `http_api_client_request_seconds` requires the HTTP interceptor, which is installed automatically when `withMetricsCollector` is called on the builder.
- `task_result_size_bytes` and `workflow_input_size_bytes` likewise require the HTTP interceptor -- they are recorded at wire time from `RequestBody.contentLength()` for requests tagged with a `PayloadKind`.
- `task_ack_failed_total` and `task_ack_error_total` require `taskClient.registerTaskRunnerListener(metricsCollector)`. This is automatic when using `withMetricsCollector` on the builder.

### High Cardinality

- The `uri` label on `http_api_client_request_seconds` uses the path template, so it is bounded by the number of distinct API endpoints (not by request volume or unique IDs). The interceptor falls back to the resolved path for requests that are not tagged with a template, which may be unbounded.
- Avoid embedding user identifiers or unbounded values in task type, workflow type, or external payload labels.
