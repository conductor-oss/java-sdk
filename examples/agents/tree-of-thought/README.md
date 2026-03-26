# Tree of Thought: Three Parallel Solution Paths, Evaluate, Select Best

Instead of one chain of thought, the agent defines the problem, then explores three solution paths (A, B, C) in parallel via FORK_JOIN. Each path produces a `solution` with an `approach` and confidence score. The evaluator compares all three scores, and the selector picks the best path and solution.

## Workflow

```
problem -> tt_define_problem
  -> FORK_JOIN(tt_path_a | tt_path_b | tt_path_c)
  -> tt_evaluate_paths -> tt_select_best
```

## Workers

**PathA/B/CWorkers** -- Each produces a `solution`, `approach`, and confidence `score`.

**EvaluatePathsWorker** (`tt_evaluate_paths`) -- Compares `scoreA`, `scoreB`, `scoreC`.

**SelectBestWorker** (`tt_select_best`) -- Returns `selectedPath` and `solution`.

## Tests

54 tests cover problem definition, all three paths, evaluation, and selection.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
