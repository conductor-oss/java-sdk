# Distributed Logging

A distributed trace spans three services. The pipeline collects logs from each service in parallel using a FORK, then correlates them by trace ID to reconstruct the full request flow.

## Workflow

```
FORK ──┬── collect_svc1 ──┐
       ├── collect_svc2 ──┤
       └── collect_svc3 ──┤
                           JOIN
                            │
                     dg_correlate
```

Workflow `distributed_logging_415` accepts `traceId` and `timeRange`. Times out after `60` seconds.

## Workers

**CollectSvc1Worker**, **CollectSvc2Worker**, **CollectSvc3Worker** -- each collects logs for the specified trace ID from their respective service.

**CorrelateWorker** (`dg_correlate`) -- correlates logs across all 3 services by trace ID. Reports the total log count.

## Workflow Output

The workflow produces `svc1Logs`, `svc2Logs`, `svc3Logs`, `correlatedEvents`, `totalLogs` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `distributed_logging_415` defines 3 tasks with input parameters `traceId`, `timeRange` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify parallel log collection and cross-service correlation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
