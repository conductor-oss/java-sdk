# Rate Limiting

An API call task uses Conductor's `rateLimitPerFrequency` and `rateLimitFrequencyInSeconds` settings to throttle execution. This prevents overwhelming downstream services when many workflows run concurrently.

## Workflow

```
rl_api_call
```

Workflow `rate_limit_demo` accepts `batchId`. Times out after `60` seconds.

## Workers

**RlApiCallWorker** (`rl_api_call`) -- reads `batchId` from input. Processes the batch while respecting the rate limit configuration.

## Workflow Output

The workflow produces `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `rl_api_call`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `rate_limit_demo` defines 1 task with input parameters `batchId` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Rate limiting demo — demonstrates task-level rate limiting with concurrency and frequency constraints.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify API call execution, rate limit configuration, and batch processing under throttling constraints.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
