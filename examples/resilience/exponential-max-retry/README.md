# Exponential Backoff with Max Retries

An unreliable external API fails intermittently. The system must retry with exponential backoff up to a configured maximum, then route permanently failed calls to a dead-letter log rather than retrying forever.

## Workflow

```
emr_unreliable_api ──(retries with EXPONENTIAL_BACKOFF, retryCount=3)──> success
       │
       └── (all retries exhausted) ──> failureWorkflow: emr_dead_letter_handler
                                                │
                                        emr_dead_letter_log
```

Workflow `emr_exponential_max_retry` accepts `shouldSucceed` as input. The task definition for `emr_unreliable_api` is configured with `retryCount` = `3`, `retryLogic` = `EXPONENTIAL_BACKOFF`, and `retryDelaySeconds` = `1`. On final failure, the `failureWorkflow` named `"emr_dead_letter_handler"` fires.

## Workers

**UnreliableApiWorker** (`emr_unreliable_api`) -- reads `shouldSucceed` from task input. When `true`, returns `COMPLETED` with `status` = `"API call successful"` and `data` = `"processed"`. When `false`, returns `FAILED` with `error` = `"API call failed"` and `status` = `"FAILED"`.

**DeadLetterLogWorker** (`emr_dead_letter_log`) -- reads `failedWorkflowId` and `reason` from task input (both default to `"unknown"` if absent). Logs both fields to stdout and returns `logged` = `true` along with the original details.

## Workflow Output

The workflow produces `status`, `data` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `emr_exponential_max_retry` defines 1 task with input parameters `shouldSucceed` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Exponential backoff with max retries -- calls emr_unreliable_api with exponential backoff retries. On final failure, triggers dead letter handler.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

The `failureWorkflow` is set to `"emr_dead_letter_handler"`, which triggers automatically when the main workflow fails.

## Tests

8 tests cover the successful API call, forced failure, exponential retry configuration, dead-letter logging after exhaustion, and end-to-end failure-to-log round trips.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
