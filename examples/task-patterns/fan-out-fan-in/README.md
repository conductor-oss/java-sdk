# Fan-Out/Fan-In

A batch of images needs parallel processing. The prepare worker converts the image list into dynamic tasks, `FORK_JOIN_DYNAMIC` processes each image in parallel, and the aggregate worker combines results. This is the image-processing variant of the dynamic fork pattern.

## Workflow

```
fo_prepare ──> FORK_JOIN_DYNAMIC ──> JOIN ──> fo_aggregate
```

Workflow `fan_out_fan_in_demo` accepts `images`. Times out after `120` seconds.

## Workers

**PrepareWorker** (`fo_prepare`) -- converts the `images` list into dynamic task definitions. Reports preparing N dynamic tasks.

**ProcessImageWorker** (`fo_process_image`) -- processes a single image by index. Reports processing image #N.

**AggregateWorker** (`fo_aggregate`) -- combines all processed image results into a single output.

## Workflow Output

The workflow produces `processedCount`, `totalSize`, `results` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `fo_prepare`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `fo_process_image`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `fo_aggregate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `fan_out_fan_in_demo` defines 4 tasks with input parameters `images` and a timeout of `120` seconds.

## Tests

8 tests verify dynamic task preparation, parallel image processing, result aggregation, and correct handling of varying image list sizes.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
