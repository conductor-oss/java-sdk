# Plan-Execute: Create Plan, Execute Three Steps Sequentially, Compile

An objective goes in and the agent creates a multi-step plan, executes each step reporting `status: "complete"` with the result, then compiles all step results into a final report.

## Workflow

```
objective
    |
    v
+-----------------+
| pe_create_plan  |  Create step-by-step plan
+--------+--------+
         v
+-----------------+
| pe_execute_step_1|  First step -> status: "complete"
+--------+--------+
         v
+-----------------+
| pe_execute_step_2|  Second step -> status: "complete"
+--------+--------+
         v
+-----------------+
| pe_execute_step_3|  Third step -> status: "complete"
+--------+--------+
         v
+------------------+
| pe_compile_results|  Compile into report
+------------------+
```

## Workers

**CreatePlanWorker** (`pe_create_plan`) -- Produces `steps` as a `List.of(...)` from the objective.

**ExecuteStep1-3Workers** -- Each outputs a `result` string and `status: "complete"`.

**CompileResultsWorker** (`pe_compile_results`) -- Aggregates all step results into a `report`.

## Tests

45 tests cover plan creation, all three execution steps, and result compilation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
