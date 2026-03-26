# Analyst-Strategist-Executor Collaboration for Business Planning

A business context goes in; an execution plan comes out. The analyst identifies insights (e.g., `INS-001: "Customer churn concentrated in 90-day post-acquisition window"`), the strategist formulates a strategy (`"Stabilize & Retain"` thesis), the executor creates action items (`ACT-001: "Redesign onboarding email sequence with personalized milestones"`), and the compiler assembles the final plan.

## Workflow

```
businessContext -> ac_analyst -> ac_strategist -> ac_executor -> ac_compile_plan
```

## Workers

**AnalystWorker** (`ac_analyst`) -- Produces insights like `{id: "INS-001", finding: "Customer churn concentrated in 90-day post-acquisition window"}`.

**StrategistWorker** (`ac_strategist`) -- Formulates strategy: `name: "Stabilize & Retain"`, `thesis: "Address churn by improving post-acquisition experience..."`.

**ExecutorWorker** (`ac_executor`) -- Creates actions: `{id: "ACT-001", task: "Redesign onboarding email sequence with personalized milestones"}`.

**CompilePlanWorker** (`ac_compile_plan`) -- Assembles `title: "Stabilize & Retain -- Execution Plan"` from strategy pillars.

## Tests

36 tests cover all four collaboration stages.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
