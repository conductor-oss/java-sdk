# SLA Scheduling

Support tickets need processing in SLA priority order. The pipeline prioritizes tickets based on the SLA policy, executes them in priority order, and tracks SLA compliance metrics.

## Workflow

```
sla_prioritize ──> sla_execute_tasks ──> sla_track_compliance
```

Workflow `sla_scheduling_409` accepts `tickets` and `slaPolicy`. Times out after `60` seconds.

## Workers

**PrioritizeWorker** (`sla_prioritize`) -- prioritizes tickets by the SLA policy.

**ExecuteTasksWorker** (`sla_execute_tasks`) -- processes tickets in SLA priority order. Reports the total ticket count.

**TrackComplianceWorker** (`sla_track_compliance`) -- tracks SLA compliance for the processed tickets.

## Workflow Output

The workflow produces `totalTickets`, `completedOnTime`, `complianceRate` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `sla_scheduling_409` defines 3 tasks with input parameters `tickets`, `slaPolicy` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify ticket prioritization, execution in priority order, and SLA compliance tracking.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
