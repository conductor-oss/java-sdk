# Changelog

All notable changes to this project will be documented in this file.

## [5.1.0]

### Added

- Standardized Prometheus metrics: `PrometheusMetricsCollector` now emits the harmonized cross-SDK metric surface — [details](conductor-client-metrics/README.md)
- Automatic metrics wiring: `ConductorClient.Builder.withMetricsCollector(...)` installs the HTTP interceptor and auto-registers listeners on `TaskClient`, `WorkflowClient`, and `TaskRunnerConfigurer`
- HTTP API client metrics via OkHttp interceptor (`http_api_client_request_seconds`, `task_result_size_bytes`, `workflow_input_size_bytes`)
- Event-driven metrics architecture with `EventDispatcher` and typed event POJOs

### Changed

- `PrometheusMetricsCollector` metric names updated to the harmonized cross-SDK catalog (e.g. `task_poll_total`, `task_execute_time_seconds`)
- `micrometer-registry-prometheus` is now a transitive (`api`) dependency

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
