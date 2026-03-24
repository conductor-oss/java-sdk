# Autonomous Agent: Goal-Directed Planning with Iterative Execution

Given a mission, this agent sets a goal with constraints, creates a multi-step plan, then executes steps in a DO_WHILE loop with progress evaluation. One step example: `"Alerts configured: 8 rules for critical thresholds, Slack + PagerDuty notifications enabled"`. After all steps complete, it produces a final report.

## Workflow

```
mission -> aa_set_goal -> aa_create_plan -> DO_WHILE(aa_execute_step -> aa_evaluate_progress) -> aa_final_report
```

## Workers

**SetGoalWorker** (`aa_set_goal`) -- Parses the mission, sets constraints as `List.of(...)`.

**CreatePlanWorker** (`aa_create_plan`) -- Creates a step-by-step plan.

**ExecuteStepWorker** (`aa_execute_step`) -- Executes the current plan step with detailed output.

**EvaluateProgressWorker** (`aa_evaluate_progress`) -- Reports `progress` percentage and `onTrack: true`.

**FinalReportWorker** (`aa_final_report`) -- Produces `report` and `success: true`.

## Tests

45 tests cover goal setting, planning, step execution, progress evaluation, and reporting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
