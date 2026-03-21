# Conductor UI Explorer in Java with Conductor: A 3-Step Workflow for Learning the Dashboard

## Seeing Orchestration in Action

Reading about workflow orchestration is abstract. Seeing it in a UI makes it concrete. This workflow runs three sequential tasks that pass data between them, giving you a real execution to explore in Conductor's built-in dashboard. You can click on each task to see its inputs, outputs, and timing. You can see how data flows from one step to the next via JSONPath expressions. And you can use the workflow diagram view to visualize the execution path.

This is a learning tool, the tasks themselves are simple, but the UI exploration skills you build here apply to any Conductor workflow.

## The Solution

**You just write the step workers that produce inspectable outputs. Conductor handles sequencing, data flow, and the dashboard visualization.**

Three simple workers produce outputs that flow between tasks. The value is in exploring the Conductor UI. task detail panels, workflow diagrams, execution timelines, and search/filtering, using a real, running workflow as your sandbox.

### What You Write: Workers

Three simple workers. StepOneWorker, StepTwoWorker, and StepThreeWorker. Give you a multi-step workflow to explore in the Conductor UI dashboard.

| Worker | Task | What It Does |
|---|---|---|
| **StepOneWorker** | `ui_step_one` | Step One. Processes user action input. |
| **StepThreeWorker** | `ui_step_three` | Step Three. Summarizes results from steps one and two. |
| **StepTwoWorker** | `ui_step_two` | Step Two. Enriches data from step one with metadata. |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the demo logic for your real service calls, the worker contract stays the same.

### The Workflow

```
ui_step_one
 │
 ▼
ui_step_two
 │
 ▼
ui_step_three

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
