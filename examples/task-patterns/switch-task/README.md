# Switch Task

A support ticket routing system uses a SWITCH task to route tickets by `priority`. High priority escalates to a manager, medium assigns to a team, low auto-resolves, and unknown priorities go to a default classification handler. A log task records the action taken.

## Workflow

```
SWITCH(priority)
  ├── "high" ──> escalate to manager
  ├── "medium" ──> assign to team
  ├── "low" ──> auto-resolve
  └── default ──> classify
                    │
             sw_log_action
```

Workflow `switch_demo` accepts `ticketId`, `priority`, and `description`. Times out after `60` seconds.

## Workers

**EscalateWorker** (`manager`) -- escalates to `manager@example.com`.

**TeamReviewWorker** (`team`) -- assigns to `support-team-1`.

**AutoHandleWorker** (`auto`) -- auto-resolves the ticket.

**UnknownPriorityWorker** (`default`) -- flags for classification.

**LogActionWorker** (`sw_log_action`) -- logs the action taken for the ticket.

## Workflow Output

The workflow produces `ticketId`, `priority`, `logged` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `sw_auto_handle`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sw_team_review`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sw_escalate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sw_unknown_priority`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sw_log_action`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `switch_demo` defines 2 tasks with input parameters `ticketId`, `priority`, `description` and a timeout of `60` seconds.

## Tests

7 tests verify routing for each priority level, default case handling, and action logging.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
