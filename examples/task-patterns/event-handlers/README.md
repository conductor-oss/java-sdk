# Event Handlers

An external system publishes events that trigger workflows. The workflow processes events based on their type and payload, demonstrating how Conductor's event handler mechanism connects external event sources to workflow execution.

## Workflow

```
eh_process_event
```

Workflow `event_triggered_workflow` accepts `eventType` and `payload`. Times out after `60` seconds.

## Workers

**ProcessEventWorker** (`eh_process_event`) -- reads `eventType` and `payload` from input. Reports processing the event type with its payload.

## Workflow Output

The workflow produces `result`, `eventType`, `payload` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `eh_process_event`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `event_triggered_workflow` defines 1 task with input parameters `eventType`, `payload` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Workflow triggered by external events. Processes the event type and payload.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

7 tests verify event processing for different event types, payload handling, and the event-to-workflow triggering mechanism.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
