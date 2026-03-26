# Task Input Templates

A three-step pipeline demonstrates Conductor's input template system. The lookup worker fetches user details, the build-context worker constructs an execution context from user data, and the execute worker performs the action. Each step's input template wires specific output fields from upstream tasks.

## Workflow

```
tpl_lookup_user ──> tpl_build_context ──> tpl_execute_action
```

Workflow `input_templates_demo` accepts `userId`, `action`, and `metadata`. Times out after `60` seconds.

## Workers

**LookupUserWorker** (`tpl_lookup_user`) -- looks up user details by ID. Returns `name` and `role`.

**BuildContextWorker** (`tpl_build_context`) -- constructs the execution context from user data.

**ExecuteActionWorker** (`tpl_execute_action`) -- reads `userName` and `userRole` from the context. Executes the requested action.

## Workflow Output

The workflow produces `result`, `auditLog` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `tpl_lookup_user`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `tpl_build_context`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `tpl_execute_action`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `input_templates_demo` defines 3 tasks with input parameters `userId`, `action`, `metadata` and a timeout of `60` seconds.

## Tests

5 tests verify user lookup, context building, action execution, and correct input template wiring between tasks.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
