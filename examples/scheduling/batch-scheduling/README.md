# Batch Scheduling

A batch of jobs needs to be prioritized, have resources allocated, and then executed with a configurable concurrency limit. The pipeline handles the full batch lifecycle from job ordering through execution.

## Workflow

```
bs_prioritize_jobs ──> bs_allocate_resources ──> bs_execute_batch
```

Workflow `batch_scheduling_404` accepts `batchId`, `jobs`, and `maxConcurrency`. Times out after `60` seconds.

## Workers

**PrioritizeJobsWorker** (`bs_prioritize_jobs`) -- prioritizes the jobs within the batch by their attributes.

**AllocateResourcesWorker** (`bs_allocate_resources`) -- allocates compute resources for the batch.

**ExecuteBatchWorker** (`bs_execute_batch`) -- executes the batch with the allocated resources.

## Workflow Output

The workflow produces `totalJobs`, `resourcesAllocated`, `jobsCompleted`, `totalDurationMs` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `batch_scheduling_404` defines 3 tasks with input parameters `batchId`, `jobs`, `maxConcurrency` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Batch scheduling workflow that prioritizes jobs, allocates resources, and executes the batch.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

1 test verifies the end-to-end batch scheduling pipeline.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
