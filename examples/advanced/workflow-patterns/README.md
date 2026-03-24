# Workflow Patterns

A workflow engine needs to demonstrate fundamental control-flow patterns: sequence, parallel split, synchronization, exclusive choice, and merge. Each pattern is a building block that more complex workflows combine. Understanding them individually clarifies how the engine executes branching and joining logic.

## Pipeline

```
[wp_chain_step]
     |
     v
     +─────────────────────────────+
     | [wp_split_a] | [wp_split_b] |
     +─────────────────────────────+
     [join]
     |
     v
[wp_merge_results]
     |
     v
     +── loop ──────────────+
     |  [wp_loop_iteration]
     +───────────────────────+
```

**Workflow inputs:** `inputData`, `iterations`

## Workers

**WpChainStepWorker** (task: `wp_chain_step`)

Chain step: sequential processing in a chain pattern.

- Sets `result` = `"chain_processed"`
- Reads `data`. Writes `result`

**WpLoopIterationWorker** (task: `wp_loop_iteration`)

Loop iteration: processes one iteration in a DO_WHILE loop.

- Reads `iteration`. Writes `iteration`, `processed`

**WpMergeResultsWorker** (task: `wp_merge_results`)

Merge results from fork branches A and B.

- Reads `resultA`, `resultB`. Writes `combined`, `total`

**WpSplitAWorker** (task: `wp_split_a`)

Split branch A: parallel fork processing.

- Sets `result` = `"branch_a_done"`
- Reads `chainOutput`. Writes `result`, `value`

**WpSplitBWorker** (task: `wp_split_b`)

Split branch B: parallel fork processing.

- Sets `result` = `"branch_b_done"`
- Reads `chainOutput`. Writes `result`, `value`

---

**38 tests** | Workflow: `workflow_patterns_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
