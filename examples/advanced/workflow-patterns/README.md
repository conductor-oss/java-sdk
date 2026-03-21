# Workflow Patterns Showcase in Java Using Conductor : Chain, Fork-Join, and Loop in One Workflow

## Real Workflows Combine Multiple Patterns

Real-world processes don't fit a single pattern. An ETL pipeline starts with a sequential validation step (chain), then splits data processing across multiple workers (fork-join), then iterates over remaining unprocessed records until the batch is complete (loop). Most workflow tutorials show one pattern at a time, but production workflows combine sequential steps, parallel fan-out, and iterative loops in the same definition.

This example shows how Conductor composes all three patterns. chain, fork-join, and do-while loop, in a single workflow, demonstrating that you can mix patterns freely without special glue code.

## The Solution

**You write the chain, split, and loop logic. Conductor handles pattern composition, iteration control, and parallel branch management.**

`WpChainStepWorker` handles the initial sequential processing step. A `FORK_JOIN` then splits into two parallel branches. `WpSplitAWorker` and `WpSplitBWorker` process different aspects of the data simultaneously. The `JOIN` waits for both branches. `WpMergeResultsWorker` combines the parallel outputs. Finally, a `DO_WHILE` loop runs `WpLoopIterationWorker` iteratively, processing remaining items until the iteration count reaches the configured limit. Conductor handles the sequential-to-parallel-to-iterative transitions seamlessly, tracking every iteration and branch.

### What You Write: Workers

Five workers demonstrate three patterns in one workflow: a chain step, two parallel fork branches, a result merger, and a loop iterator, showing how Conductor composes sequential, parallel, and iterative logic.

| Worker | Task | What It Does |
|---|---|---|
| **WpChainStepWorker** | `wp_chain_step` | Chain step: sequential processing in a chain pattern. |
| **WpLoopIterationWorker** | `wp_loop_iteration` | Loop iteration: processes one iteration in a DO_WHILE loop. |
| **WpMergeResultsWorker** | `wp_merge_results` | Merge results from fork branches A and B. |
| **WpSplitAWorker** | `wp_split_a` | Split branch A: parallel fork processing. |
| **WpSplitBWorker** | `wp_split_b` | Split branch B: parallel fork processing.### The Workflow

```
wp_chain_step
 │
 ▼
FORK_JOIN
 ├── wp_split_a
 └── wp_split_b
 │
 ▼
JOIN (wait for all branches)
wp_merge_results
 │
 ▼
DO_WHILE
 └── wp_loop_iteration

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
