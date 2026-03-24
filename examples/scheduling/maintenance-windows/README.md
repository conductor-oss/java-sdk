# Maintenance Windows

A system needs maintenance but can only be serviced during approved time windows. The pipeline checks whether the current time falls within a maintenance window. If yes, it executes the maintenance. If not, it defers to the next available window.

## Workflow

```
mnw_check_window ──> SWITCH
                       ├── in_window ──> mnw_execute_maintenance
                       └── outside ──> mnw_defer_maintenance
```

Workflow `maintenance_windows_408` accepts `system`, `maintenanceType`, and `currentTime`. Times out after `60` seconds.

## Workers

**CheckWindowWorker** (`mnw_check_window`) -- checks whether the current time falls within the maintenance window for the specified system.

**ExecuteMaintenanceWorker** (`mnw_execute_maintenance`) -- runs the maintenance operation on the system.

**DeferMaintenanceWorker** (`mnw_defer_maintenance`) -- defers maintenance to the next available window.

## Workflow Output

The workflow produces `windowStatus`, `windowEnd`, `action` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `maintenance_windows_408` defines 2 tasks with input parameters `system`, `maintenanceType`, `currentTime` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify window checking, maintenance execution, and deferral routing.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
