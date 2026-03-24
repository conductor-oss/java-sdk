# Break Down a Goal into 3 Parallel Subgoals, Then Aggregate

A high-level goal is decomposed into subgoals, which are executed in parallel via FORK_JOIN. Each subgoal worker reports `status: "complete"` with its result. The aggregator combines all results.

## Workflow

```
goal -> gd_decompose_goal -> FORK_JOIN(gd_subgoal_1 | gd_subgoal_2 | gd_subgoal_3) -> gd_aggregate
```

## Workers

**DecomposeGoalWorker** (`gd_decompose_goal`) -- Produces `subgoals` as a `List.of(...)`.

**Subgoal1-3Workers** -- Each reports a result and `status: "complete"`.

**AggregateWorker** (`gd_aggregate`) -- Combines all subgoal results into `aggregatedResult`.

## Tests

40 tests cover decomposition, parallel execution, and aggregation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
