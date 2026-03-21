# Worker Pool Management in Java Using Conductor : Categorize, Assign Pool, Execute, Return

## Specialized Tasks Need Specialized Workers

A video transcoding task needs workers with high-CPU instances and FFmpeg installed. An image recognition task needs GPU workers with CUDA drivers. A PDF generation task just needs a basic worker with LibreOffice. Sending all tasks to a single general-purpose pool wastes expensive GPU time on PDF generation and leaves transcoding tasks waiting behind image recognition jobs.

Worker pool management means categorizing each task to determine what kind of worker it needs, selecting the right pool (GPU pool, high-CPU pool, general pool), executing the task on a worker from that pool, and returning the worker for reuse when the task finishes.

## The Solution

**You write the categorization and pool assignment logic. Conductor handles task dispatch, retries, and pool utilization tracking.**

`WplCategorizeTaskWorker` examines the task payload and category to determine what resources it needs. `WplAssignPoolWorker` maps the task category to the appropriate worker pool. `WplExecuteTaskWorker` runs the task on a worker from the assigned pool. `WplReturnToPoolWorker` releases the worker back to the pool for reuse. Conductor sequences these steps, retries if execution fails, and records which pool handled each task. enabling pool utilization analysis.

### What You Write: Workers

Four workers handle pool-based dispatch. Task categorization, pool assignment by resource profile, execution on the assigned worker, and pool return for reuse.### The Workflow

```
wpl_categorize_task
 │
 ▼
wpl_assign_pool
 │
 ▼
wpl_execute_task
 │
 ▼
wpl_return_to_pool

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
