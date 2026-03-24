# Uptime Monitoring

An endpoint needs SLA tracking. The pipeline checks the endpoint status, logs the result, calculates the SLA percentage, and generates a report.

## Workflow

```
um_check_endpoint ──> um_log_result ──> um_calculate_sla ──> um_report
```

Workflow `uptime_monitoring_420` accepts `endpoint`, `expectedStatus`, and `slaTarget`. Times out after `60` seconds.

## Workers

**CheckEndpointWorker** (`um_check_endpoint`) -- checks the specified endpoint and returns the status.

**LogResultWorker** (`um_log_result`) -- logs the endpoint status result.

**CalculateSlaWorker** (`um_calculate_sla`) -- calculates the SLA percentage from check results.

**UmReportWorker** (`um_report`) -- generates the SLA report.

## Workflow Output

The workflow produces `isUp`, `responseTimeMs`, `currentSla`, `slaMet` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `uptime_monitoring_420` defines 4 tasks with input parameters `endpoint`, `expectedStatus`, `slaTarget` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify endpoint checking, result logging, SLA calculation, and report generation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
