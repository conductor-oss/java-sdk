# Competitive Agents in Java Using Conductor : Parallel Solvers, Judge, and Winner Selection

Competitive Agents. three solvers propose solutions in parallel, a judge scores them, and a winner is selected.

## Better Solutions Through Competition

A single AI agent solving a problem gives you one perspective. Three agents solving the same problem independently give you three perspectives, and the best one is almost always better than a random single attempt. Agent 1 might take an analytical approach (data-driven, quantitative). Agent 2 might take a creative approach (novel angles, unconventional solutions). Agent 3 might take a practical approach (implementation-focused, risk-aware).

The challenge is running all three simultaneously (sequential execution triples the latency), evaluating their solutions objectively against the same criteria, and selecting the winner with justification. Without orchestration, parallel agent execution means managing thread pools, handling partial failures (agent 2 timed out but agents 1 and 3 produced solutions), and synchronizing results for the judge.

## The Solution

**You write the solver strategies and judging criteria. Conductor handles parallel execution, result collection, and comparative scoring.**

`FORK_JOIN` dispatches `Solver1Worker`, `Solver2Worker`, and `Solver3Worker` to tackle the problem simultaneously. each takes a different approach (analytical, creative, practical) and produces a solution with rationale. After `JOIN` collects all three solutions, `JudgeAgentWorker` evaluates each against the specified criteria (accuracy, creativity, feasibility, clarity) and assigns scores. `SelectWinnerWorker` picks the highest-scoring solution and produces the final output with the winning solution, scores for all three, and the judge's reasoning. Conductor runs all three solvers in parallel, so total time equals the slowest solver, not the sum of all three.

### What You Write: Workers

Five workers run the competition. Three solvers propose solutions in parallel using different strategies, a judge scores them, and a winner is selected.

| Worker | Task | What It Does |
|---|---|---|
| **JudgeAgentWorker** | `comp_judge_agent` | Judge agent. evaluates all three solver solutions, scores each on cost, innovation, and risk, then ranks them to det.. |
| **SelectWinnerWorker** | `comp_select_winner` | Select winner. takes the judge's judgment and all solutions, produces a winner map and a ranked list of all solutions. |
| **Solver1Worker** | `comp_solver_1` | Creative solver. proposes an innovative, AI-powered adaptive solution. Uses a "creative" approach with higher innova.. |
| **Solver2Worker** | `comp_solver_2` | Analytical solver. proposes a data-driven optimization framework. Uses an "analytical" approach with balanced innova.. |
| **Solver3Worker** | `comp_solver_3` | Practical solver. proposes an incremental process improvement. Uses a "practical" approach with low cost, low risk.. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
FORK_JOIN
 ├── comp_solver_1
 ├── comp_solver_2
 └── comp_solver_3
 │
 ▼
JOIN (wait for all branches)
comp_judge_agent
 │
 ▼
comp_select_winner

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
