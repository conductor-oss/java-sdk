# Do-While Loop in Java with Conductor

DO_WHILE loop demo: processes items in a batch one at a time, iterating until the batch is complete, then summarizes all results. Demonstrates Conductor's declarative loop construct where the orchestrator manages iteration state, progress tracking, and per-iteration retry. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to process items in a batch one at a time, where each item requires a separate task execution with its own retry policy and timeout. The batch size is determined at runtime (could be 5 items or 500), and if processing fails on item 47 of 100, you need to retry just item 47. . Not restart from item 1. After all items are processed, you need a summary of what happened.

Without orchestration, you'd write a for-loop in a single long-running process, managing iteration state manually, adding checkpointing logic to survive crashes, and building retry handling for individual item failures. If the process dies mid-batch, you either restart from scratch (wasting work on items 1-46) or build complex resume logic with database-backed state that is hard to test and fragile to maintain.

## The Solution

**You just write the per-item processing and summarization workers. Conductor handles the iteration, state tracking, and per-iteration retry.**

This example demonstrates Conductor's DO_WHILE loop task, a declarative iteration construct where the loop condition (`iteration < batchSize`) is evaluated by Conductor after each pass. ProcessItemWorker handles one item per iteration: it receives the current iteration number, processes the item, and returns the incremented iteration counter. If the worker fails on any iteration, Conductor retries that specific iteration without re-processing already-completed items. After the loop exits (when iteration >= batchSize), SummarizeWorker aggregates the results into a completion message with the total count. Conductor tracks every iteration with its own inputs and outputs, giving you full visibility into batch progress without any custom logging or state management.

### What You Write: Workers

Two workers handle the iterative batch: ProcessItemWorker processes one item per loop iteration and increments the counter, while SummarizeWorker aggregates results after all iterations complete.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessItemWorker** | `dw_process_item` | Processes a single item within the loop. Takes the current iteration number, increments it, and returns itemProcessed=true, the new iteration count, and a result string like "Item-3 processed". Defaults iteration to 0 if missing. |
| **SummarizeWorker** | `dw_summarize` | Runs after the loop completes. Takes totalIterations and returns totalProcessed and a summary string like "Processed 5 items successfully". Defaults to 0 if totalIterations is missing. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
DO_WHILE (loop condition: iteration < batchSize)
 └── dw_process_item (runs once per iteration)
 │
 ▼
dw_summarize (runs once after loop exits)

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
