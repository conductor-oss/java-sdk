# Chaining HTTP Tasks

A workflow registers a task definition via Conductor's REST API, verifies the registration by fetching it back, and formats the HTTP response using an INLINE task. This demonstrates chaining HTTP system tasks with custom workers.

## Workflow

```
register_task_via_http (HTTP) ──> verify_task_via_http (HTTP) ──> format_http_result (INLINE)
```

Workflow `http_chain_demo` accepts `taskName` and `conductorApiUrl`. Times out after `60` seconds.

## Workers

**PrepareRequestWorker** (`http_prepare_request`) -- builds a search query from the input. Returns `searchTerm` = the query string.

**ProcessResponseWorker** (`http_process_response`) -- processes the HTTP response from the verification call.

The workflow also uses two `HTTP` tasks for REST API calls and one `INLINE` task for response formatting.

## Workflow Output

The workflow produces `registered`, `verifiedName`, `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `http_prepare_request`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `http_process_response`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `http_chain_demo` defines 3 tasks with input parameters `taskName`, `conductorApiUrl` and a timeout of `60` seconds.

## Tests

5 tests verify HTTP task chaining, response processing, and inline formatting.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
