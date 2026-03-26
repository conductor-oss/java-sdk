# Wait for Event

A request-approval workflow prepares a request, then suspends execution using a `WAIT` task until an external signal is received. After the signal arrives, a processing worker completes the request. This demonstrates human-in-the-loop and external system integration patterns.

## Workflow

```
we_prepare ──> WAIT(wait_for_signal) ──> we_process_signal
```

Workflow `wait_event_demo` accepts `requestId` and `requester`. Times out after `600` seconds (10 minutes, allowing time for external approval).

## Workers

**PrepareWorker** (`we_prepare`) -- prepares the request for approval.

**ProcessSignalWorker** (`we_process_signal`) -- processes the signal after it is received, completing the request.

## Workflow Output

The workflow produces `requestId`, `decision`, `processed` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `we_prepare`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `we_process_signal`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `wait_event_demo` defines 3 tasks with input parameters `requestId`, `requester` and a timeout of `600` seconds.

## Tests

9 tests verify request preparation, wait task suspension, signal delivery via Conductor API, and post-signal processing.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
