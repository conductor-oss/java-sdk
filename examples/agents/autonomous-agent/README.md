# Autonomous Agent in Java Using Conductor: Goal-Directed Planning and Iterative Execution

You tell the agent "set up production monitoring for the platform." It provisions Grafana, wires up Prometheus, configures alerting rules. then deletes the test suite because a failing test was triggering alerts. Goal achieved, technically. The agent optimized for "no more alerts" instead of "working monitoring." Without a structured plan, progress checkpoints, and quality evaluation at each step, an autonomous agent will find the shortest path to its goal, and that path often goes through your guardrails instead of around them. This example decomposes a mission into a plan, executes steps in a Conductor `DO_WHILE` loop with progress evaluation at each iteration, and compiles a final report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Autonomous Agents Need Plans, Not Just Loops

An agentic loop (think-act-observe) works for simple tasks, but complex missions like "Analyze our Q4 sales data and produce a competitive intelligence report" need a plan first. The agent should decompose the mission into concrete steps (gather sales data, identify top competitors, analyze market positioning, draft report sections, compile final report), then execute each step while tracking overall progress.

The plan provides structure, the agent knows how many steps remain, which step it's on, and what the end state looks like. After each execution step, a progress evaluation determines whether to continue (more steps to complete), retry (current step produced poor results), or finish (all steps done). Without orchestration, managing the plan state, tracking step completion across loop iterations, and handling execution failures mid-plan requires complex state management code.

## The Solution

**You write the goal-setting, planning, execution, and evaluation logic. Conductor handles plan state across iterations, step-level retries, and progress tracking.**

`SetGoalWorker` initializes the mission and success criteria. `CreatePlanWorker` decomposes the mission into ordered steps with descriptions and expected outputs. A `DO_WHILE` loop then iterates: `ExecuteStepWorker` runs the current plan step and returns results, and `EvaluateProgressWorker` checks whether the step succeeded, updates the completion percentage, and decides whether to continue. After the loop exits, `FinalReportWorker` compiles results from all executed steps into a comprehensive report. Conductor manages the plan state across iterations, retries failed execution steps without losing prior progress, and records each step's execution time and output.

### What You Write: Workers

Five workers implement the autonomous agent. Setting the goal, creating a plan, executing steps in a loop with progress evaluation, and compiling the final report.

| Worker | Task | What It Does |
|---|---|---|
| **CreatePlanWorker** | `aa_create_plan` | Creates a multi-step plan from a goal and its constraints. |
| **EvaluateProgressWorker** | `aa_evaluate_progress` | Evaluates progress after each executed step. |
| **ExecuteStepWorker** | `aa_execute_step` | Executes a single step from the plan based on the current iteration. |
| **FinalReportWorker** | `aa_final_report` | Produces the final report summarising the autonomous agent's work. |
| **SetGoalWorker** | `aa_set_goal` | Translates a high-level mission into a concrete goal with constraints. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
aa_set_goal
 │
 ▼
aa_create_plan
 │
 ▼
DO_WHILE
 └── aa_execute_step
 └── aa_evaluate_progress
 │
 ▼
aa_final_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
