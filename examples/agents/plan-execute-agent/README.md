# Plan-Execute Agent in Java Using Conductor: Create Plan, Execute Three Steps, Compile Results

Tell an AI agent "deploy the new version" without a planning step and watch it start deploying immediately, realize halfway through it needs to run tests first, roll back, re-run tests, then deploy again: three times the work, three times the risk, because it never stopped to think before acting. The plan-execute pattern is the fix: create a structured plan upfront, execute each step in order, compile the results. This example uses [Conductor](https://github.com/conductor-oss/conductor) to separate planning from execution as distinct workers, if step 2 fails, Conductor retries it without re-running step 1 or regenerating the plan, and every intermediate result is persisted for inspection.

## Simple Objectives Need Simple Plans

Not every agent task needs an autonomous loop with goal evaluation at each step. "Research the top 3 JavaScript frameworks and produce a comparison" has a clear, fixed plan: (1) research React, (2) research Vue, (3) research Angular. Each step is independent, the plan doesn't change based on intermediate results, and the compilation step simply merges the three research outputs.

The plan-execute pattern is the simplest multi-step agent architecture: create a plan upfront, execute each step in order, compile the results. If step 2 fails (the Vue research source is down), retry it without re-running step 1 or re-creating the plan. The plan and all intermediate results are preserved across retries.

## The Solution

**You write the planning, step execution, and compilation logic. Conductor handles sequential chaining, per-step retries, and result assembly.**

`CreatePlanWorker` decomposes the objective into three ordered steps with descriptions and expected outputs. `ExecuteStep1Worker`, `ExecuteStep2Worker`, and `ExecuteStep3Worker` run each plan step sequentially, with each step receiving the plan context and prior step results. `CompileResultsWorker` merges all three step outputs into a final deliverable that addresses the original objective. Conductor chains these five steps and records each step's execution time and output for plan quality analysis.

### What You Write: Workers

Five workers implement plan-then-execute. Creating a structured plan, executing three steps in sequence, and compiling results into a final report.

| Worker | Task | What It Does |
|---|---|---|
| **CreatePlanWorker** | `pe_create_plan` | Decomposes a high-level objective into three ordered steps ("Gather market data and competitor analysis", "Analyze trends and identify opportunities", "Generate strategic recommendations"). Returns the step list and total step count. |
| **ExecuteStep1Worker** | `pe_execute_step_1` | Executes step 1 of the plan: gathers market data and competitor analysis. Returns a summary of 5 competitors analyzed and a $4.2B market size estimate. |
| **ExecuteStep2Worker** | `pe_execute_step_2` | Executes step 2: analyzes trends and identifies opportunities. Receives the step 1 result as context. Returns 3 growth opportunities (API platform, enterprise tier, international expansion). |
| **ExecuteStep3Worker** | `pe_execute_step_3` | Executes step 3: generates strategic recommendations. Receives the step 2 result as context. Returns prioritized recommendations with ROI estimates (API platform at capacity-planning%, enterprise tier at 210%). |
| **CompileResultsWorker** | `pe_compile_results` | Compiles all three step results into a single report string formatted as "Objective: .. | Step 1: .. | Step 2: .. | Step 3: ...". |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
pe_create_plan
 |
 v
pe_execute_step_1
 |
 v
pe_execute_step_2
 |
 v
pe_execute_step_3
 |
 v
pe_compile_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
