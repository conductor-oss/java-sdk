# Goal Decomposition in Java Using Conductor : Break Down Goals, Execute Subgoals in Parallel, Aggregate

Goal Decomposition. decomposes a high-level goal into subgoals, executes them in parallel via FORK/JOIN, then aggregates the results.

## Big Goals Need to Be Broken Down

"Improve customer satisfaction" is a goal, not a plan. A useful agent decomposes it into actionable subgoals: analyze current satisfaction scores and identify pain points, benchmark against competitors, and design improvement initiatives. These three subgoals are independent. they can run simultaneously; but all must complete before the results can be aggregated into a coherent improvement plan.

Decomposition quality determines execution quality. If the subgoals overlap, agents duplicate work. If they have gaps, the final aggregation misses important areas. If one subgoal's agent fails (competitor data API is down), the other two results are still valid. you just need to retry that one subgoal. Without orchestration, parallel subgoal execution means managing threads, handling partial failures, and synchronizing results manually.

## The Solution

**You write the decomposition, subgoal execution, and aggregation logic. Conductor handles parallel fan-out, independent retries per subgoal, and result synchronization.**

`DecomposeGoalWorker` breaks the high-level goal into three independent subgoals with clear scope, expected outputs, and success criteria. `FORK_JOIN` dispatches `Subgoal1Worker`, `Subgoal2Worker`, and `Subgoal3Worker` to execute each subgoal simultaneously. each returns structured results with findings and recommendations. After `JOIN` collects all three results, `AggregateWorker` synthesizes the subgoal outputs into a unified plan, resolving any conflicts and identifying cross-cutting themes. Conductor runs all three subgoals in parallel and retries any failed subgoal independently.

### What You Write: Workers

Five workers decompose goals into action. Breaking the goal into subgoals, executing three of them in parallel, and aggregating the results into a unified plan.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `gd_aggregate` | Aggregates the results from all three subgoal workers into a single summary. |
| **DecomposeGoalWorker** | `gd_decompose_goal` | Decomposes a high-level goal into three concrete subgoals. |
| **Subgoal1Worker** | `gd_subgoal_1` | Executes the first subgoal: analyzing current system performance bottlenecks. |
| **Subgoal2Worker** | `gd_subgoal_2` | Executes the second subgoal: researching caching and optimization strategies. |
| **Subgoal3Worker** | `gd_subgoal_3` | Executes the third subgoal: evaluating infrastructure scaling options. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
gd_decompose_goal
 │
 ▼
FORK_JOIN
 ├── gd_subgoal_1
 ├── gd_subgoal_2
 └── gd_subgoal_3
 │
 ▼
JOIN (wait for all branches)
gd_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
