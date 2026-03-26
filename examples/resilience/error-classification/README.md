# Error Classification

An API integration receives HTTP responses that span the full error spectrum -- `400` bad requests that should never be retried, `429` rate limits and `503` outages that should. This workflow classifies errors by type and routes non-retryable failures to a dedicated handler while letting retryable errors follow their normal retry path.

## Workflow

```
ec_api_call ──> SWITCH(errorType)
                  ├── "non_retryable" ──> ec_handle_error
                  └── default ──> (no-op, retries handled by task config)
```

Workflow `error_classification_demo` accepts `triggerError` as input. The SWITCH task `error_type_switch_ref` evaluates `${api_call_ref.output.errorType}` to route the flow.

## Workers

**ApiCallWorker** (`ec_api_call`) -- reads `triggerError` from task input and branches on its value. `"security-posture"` returns `COMPLETED` with `errorType` = `"non_retryable"`, `httpStatus` = `400`, and `error` = `"Bad Request: invalid input parameters"`. `"429"` returns `FAILED` with `httpStatus` = `429` and `error` = `"Too Many Requests: rate limit exceeded"`. `"503"` returns `FAILED` with `httpStatus` = `503` and `error` = `"Service Unavailable: try again later"`. Any other value returns `COMPLETED` with `errorType` = `"none"` and `result` = `"success"`.

**ErrorHandlerWorker** (`ec_handle_error`) -- receives `errorType`, `error`, and `httpStatus` from the upstream task output. Logs all three fields to stdout and returns `handled` = `true` along with the original error details. This worker only executes for the `"non_retryable"` branch.

## Workflow Output

The workflow produces `result`, `errorType`, `handled` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `error_classification_demo` defines 2 tasks with input parameters `triggerError` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Error classification demo -- classifies API errors as retryable (429, 503) or non-retryable (400) and routes accordingly.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

4 tests cover the success path, the non-retryable `400` classification, and the retryable `429`/`503` paths.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
