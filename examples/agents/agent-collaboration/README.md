# Agent Collaboration in Java Using Conductor: Sequential Analyst, Strategist, Executor, and Plan Compiler

Four specialized AI agents chained in sequence sounds clean on a whiteboard, but in practice Agent 2 sits idle while Agent 1 thinks, Agent 3 has no way to flag that Agent 2's output is garbage, and when Agent 1's LLM call times out you end up re-running the entire pipeline because nothing preserved the intermediate state. This example uses [Conductor](https://github.com/conductor-oss/conductor) to wire an analyst, strategist, executor, and plan compiler into a durable sequential pipeline, each agent is an independent worker with its own retries and timeout, Conductor persists every intermediate output, and you can inspect exactly which agent produced which piece of the final business plan.

## Business Planning Requires Multiple Perspectives in Sequence

A useful business plan requires analysis, strategy, execution planning, and synthesis. Four distinct cognitive tasks that build on each other. The analyst identifies problems (customer churn concentrated in the 90-day post-acquisition window, repeat purchase rate declining 22% QoQ). The strategist takes those findings and develops responses (retention program, pricing restructure). The executor turns strategies into implementation tasks with deadlines and resource requirements. The compiler weaves all three outputs into a coherent plan.

Each agent's output is the next agent's input, the strategist can't develop recommendations without the analyst's findings, and the executor can't create tasks without the strategist's priorities. If the strategist's LLM call times out, you need to retry it with the analyst's findings still available, not re-run the entire analysis. And you need to see each agent's individual contribution to understand how the final plan was assembled.

## The Solution

**You write each agent's analysis and planning logic. Conductor handles sequencing, data passing between agents, and durability.**

`AnalystWorker` examines the business context and produces structured findings with severity ratings (critical, high, medium) and a metrics summary. `StrategistWorker` takes those findings and develops strategic recommendations with priority rankings and expected impact. `ExecutorWorker` converts the strategy into concrete implementation tasks with owners, timelines, and resource estimates. `CompilePlanWorker` assembles the analysis, strategy, and execution plan into a final deliverable. Conductor sequences them strictly, retries any failed agent call, and records each agent's output so you can trace how every recommendation in the final plan connects back to a specific finding.

### What You Write: Workers

Four agents collaborate sequentially, the analyst produces findings, the strategist develops recommendations, the executor creates tasks, and the compiler assembles the plan.

| Worker | Task | What It Does |
|---|---|---|
| **AnalystWorker** | `ac_analyst` | Examines the business context and produces 4 structured insights with severity ratings (critical, high, medium) across retention, revenue, operations, and engagement categories. Returns a metrics summary with churn rate (15%), repeat purchase decline (22%), support response time (48h), and loyalty engagement (12%). |
| **StrategistWorker** | `ac_strategist` | Takes the analyst's insights and metrics, then formulates the "Stabilize & Retain" strategy with a thesis, 3 strategic pillars (Customer Experience Overhaul, Support Response Acceleration, Loyalty Program Revitalization), and 4 ranked priorities with effort/impact assessments. |
| **ExecutorWorker** | `ac_executor` | Translates the strategy into 6 concrete action items with owners (Marketing, Customer Support, Product, Data Science, Operations), deadlines (Week 2-8), and priority levels. Creates a 3-phase, 8-week timeline (Quick Wins, Foundation, Scale & Sustain). |
| **CompilePlanWorker** | `ac_compile_plan` | Assembles insights, strategy, action items, and timeline into a consolidated plan summary. Reports counts (insights used, strategy pillars, action items) and timeline duration. Sets status to "ready_for_review". |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
ac_analyst
 |
 v
ac_strategist
 |
 v
ac_executor
 |
 v
ac_compile_plan

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
