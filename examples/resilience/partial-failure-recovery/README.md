# Partial Failure Recovery

A three-step pipeline (validate, process, finalize) fails at step 2 due to a transient issue. Re-running the entire pipeline from scratch would duplicate step 1's side effects. The system must resume from exactly the failed step, skipping already-completed work.

## Workflow

```
pfr_step1 ──> pfr_step2 ──> pfr_step3
                  │
                  └── (fails on attempt 1, succeeds on retry via POST /workflow/{id}/retry)
```

Workflow `partial_failure_recovery_demo` accepts `data` as input. Each step passes its result to the next via `${s1_ref.output.result}` and `${s2_ref.output.result}`.

## Workers

**Step1Worker** (`pfr_step1`) -- reads `data` from input. Returns `result` = `"s1-" + data`. Always completes.

**Step2Worker** (`pfr_step2`) -- reads `prev` from input. Maintains an `AtomicInteger` called `attemptCounter`. On attempt 1 (`attemptCounter` == 1), returns `FAILED` with `error` = `"Intentional transient failure on attempt 1"`. On attempt 2+, returns `COMPLETED` with `result` = `"s2-" + prev`. Exposes a `reset()` method for testing.

**Step3Worker** (`pfr_step3`) -- reads `prev` from input. Returns `result` = `"s3-" + prev`. Always completes.

The final output for input `"abc"` is `finalResult` = `"s3-s2-s1-abc"`, confirming all three steps chained correctly.

## Workflow Output

The workflow produces `finalResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `partial_failure_recovery_demo` defines 3 tasks with input parameters `data` and a timeout of `120` seconds.

## Tests

7 tests verify the happy path, transient failure at step 2, retry resumption from step 2 without re-executing step 1, and the complete data chain across all three steps.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
