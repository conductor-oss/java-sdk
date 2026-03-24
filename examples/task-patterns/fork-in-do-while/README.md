# Fork In Do While in Java with Conductor

FORK inside DO_WHILE demo. iterative parallel processing. Each iteration forks parallel batch-processing tasks, then a summary task reports after the loop.

## The Problem

You need to process multiple batches iteratively, where each batch contains tasks that can run in parallel. For example, processing 5 batches of data imports where each batch involves parallel validation, transformation, and loading steps. The loop runs until all batches are complete (iteration >= totalBatches), and within each iteration, the parallel tasks must all finish before the next iteration begins. After all batches complete, a summary step aggregates the results across every iteration.

Without orchestration, you'd write a for-loop around a thread pool, submitting parallel tasks per iteration, waiting on a barrier for each batch, and incrementing the counter manually. If the process crashes on batch 3 of 5, you restart from batch 1 because there is no checkpoint. There is no way to see which batches completed, which parallel tasks within a batch succeeded, or how long each iteration took.

## The Solution

**You just write the per-batch processing and summary workers. Conductor handles the loop iteration, parallel forking within each iteration, and checkpointing.**

This example demonstrates combining Conductor's DO_WHILE loop with FORK_JOIN inside the loop body. iterative parallel processing. Each iteration of the loop forks parallel batch-processing tasks via FORK_JOIN, and a JOIN task waits for all parallel branches to complete before the loop condition is evaluated. The ProcessBatchWorker handles each parallel task within an iteration, returning a batchId and processing status. After the loop exits (when iteration >= totalBatches), the SummaryWorker aggregates results from all iterations into a final report. Conductor checkpoints every iteration, so if the process crashes on batch 3 of 5, it resumes from batch 3, batches 1 and 2 are not re-processed.

### What You Write: Workers

Two workers handle iterative parallel processing: ProcessBatchWorker runs within each loop iteration's FORK_JOIN branches to process batch segments, and SummaryWorker aggregates results from all iterations after the loop completes.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessBatchWorker** | `fl_process_batch` | Processes a batch within the DO_WHILE loop. Takes the current iteration number and returns a batchId along with a pro... |
| **SummaryWorker** | `fl_summary` | Summarizes the results after the DO_WHILE loop completes. Takes the iterations count and produces a summary string. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
DO_WHILE
 └── fork_batch
 └── join_batch
 │
 ▼
fl_summary

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
