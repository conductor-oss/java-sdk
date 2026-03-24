# Switch Plus Fork in Java with Conductor

SWITCH + FORK demo. conditional parallel execution. Batch type triggers parallel lanes A and B; default runs single processing.

## The Problem

You need to process items differently based on whether they arrive as a batch or individually. Batch processing requires two parallel lanes (Lane A and Lane B) that split the work and process simultaneously, then join before continuing. Single-item processing just runs one task. The decision. parallel batch processing or sequential single-item processing, must be made at runtime based on the input type. After either path completes, a common finalization step runs.

Without orchestration, you'd write an if/else that either spawns threads for parallel processing or runs a single function. Managing the thread pool, barrier, and join logic only when the batch path is taken adds conditional complexity. There is no way to see whether a given request took the batch or single path, and if one parallel lane fails in batch mode, you need custom logic to cancel or wait for the other lane.

## The Solution

**You just write the parallel lane processing, single-item processing, and finalization workers. Conductor handles the conditional branching and parallel execution.**

This example demonstrates combining Conductor's SWITCH and FORK_JOIN in a single workflow. conditional parallel execution. A JavaScript SWITCH evaluator checks the input `type`: if it equals `batch`, the workflow enters a FORK_JOIN with two parallel lanes (ProcessAWorker and ProcessBWorker processing items simultaneously), followed by a JOIN. If the type is anything else (default case), SingleProcessWorker handles the items sequentially. After either the batch fork/join or the single-item processing completes, FinalizeWorker runs as a common finalization step. Conductor records which branch was taken and, for batch mode, the results from both parallel lanes independently.

### What You Write: Workers

Four workers support conditional parallel execution: ProcessAWorker and ProcessBWorker run in parallel lanes for batch mode, SingleProcessWorker handles sequential items in default mode, and FinalizeWorker runs after either path completes.

| Worker | Task | What It Does |
|---|---|---|
| **FinalizeWorker** | `sf_finalize` | Finalizes and computes done |
| **ProcessAWorker** | `sf_process_a` | Lane A batch processor. Processes items in parallel lane A and returns the lane identifier and item count. |
| **ProcessBWorker** | `sf_process_b` | Lane B batch processor. Processes items in parallel lane B and returns the lane identifier and item count. |
| **SingleProcessWorker** | `sf_single_process` | Single item processor for the default (non-batch) case. Returns mode: "single" to indicate single-item processing was... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_by_type_ref)
 ├── batch: fork_batch -> join_batch
 └── default: sf_single_process
 │
 ▼
sf_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
