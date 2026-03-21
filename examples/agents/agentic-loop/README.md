# Agentic Loop in Java Using Conductor: Think-Act-Observe Iteration Until Goal Completion

You tell the agent "research distributed consensus algorithms." It searches, finds three papers, and searches again. And again. And again. Forty minutes and $50 in API calls later, it's still searching because nobody told it when to stop. The agent has no concept of "done".; no iteration cap, no goal-completion check, no kill switch. This example builds a think-act-observe loop with Conductor's `DO_WHILE` that gives the agent autonomy within guardrails: it reasons about what to do next, executes the plan, evaluates the result, and terminates when the goal is met or the iteration limit is hit. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Complex Goals Require Iterative Reasoning

Some tasks can't be solved in a single step. "Research distributed consensus algorithms and produce recommendations" requires gathering information, analyzing patterns, and synthesizing findings, and the agent might need multiple cycles to converge. In the first iteration, it gathers sources. In the second, it identifies patterns (consistency, partition tolerance, replication strategies, consensus protocols, failure recovery). In the third, it synthesizes recommendations.

The think-act-observe loop gives the agent autonomy within guardrails: it decides what to do next (think), executes that plan (act), evaluates the result (observe), and decides whether the goal is met or another iteration is needed. The loop must terminate. Either the agent achieves the goal or hits a maximum iteration count. Without orchestration, implementing this loop with proper state management between iterations, retry logic for failed actions, and observability into each iteration's reasoning is error-prone.

## The Solution

**You write the thinking, acting, and observing logic. Conductor handles the iteration loop, state persistence, and termination control.**

`SetGoalWorker` initializes the agent's mission and success criteria. A `DO_WHILE` loop then iterates: `ThinkWorker` examines the current state and plans the next action ("Research and gather information", then "Analyze gathered data", then "Synthesize findings"). `ActWorker` executes the planned action and returns results. `ObserveWorker` evaluates the results, updates progress tracking, and determines whether the goal is met. After the loop exits, `SummarizeWorker` compiles the findings from all iterations into a final report. Conductor manages the loop state between iterations, retries failed actions without losing prior iteration results, and records every think-act-observe cycle for debugging.

### What You Write: Workers

Five workers drive the iterative loop. Setting the goal, then cycling through think-act-observe until completion, and summarizing the findings.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `al_act` | Executes the action described by the plan and returns a result. Results are deterministically mapped from plan strings. |
| **ObserveWorker** | `al_observe` | Observes the outcome of the action and assesses goal progress. |
| **SetGoalWorker** | `al_set_goal` | Initializes the agentic loop by accepting a goal and marking it as active. |
| **SummarizeWorker** | `al_summarize` | Summarizes the agentic loop execution after all iterations are complete. |
| **ThinkWorker** | `al_think` | Plans the next action based on the goal and current iteration. Cycles through 3 fixed plan strings based on iteration |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
al_set_goal
 │
 ▼
DO_WHILE
 └── al_think
 └── al_act
 └── al_observe
 │
 ▼
al_summarize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
