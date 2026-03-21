# Agent Swarm in Java Using Conductor : Decompose Research into Parallel Specialist Investigations

Agent Swarm. decompose a research topic into subtasks, run 4 swarm agents in parallel, then merge findings into a unified report. ## Research at Scale Needs Parallel Specialization

Researching a topic like "edge computing adoption" thoroughly requires four distinct perspectives: market analysis (market size, growth rates, competitive landscape), technical landscape (architectures, key innovations, standards), real-world use cases (deployments, case studies, ROI data), and future trends (emerging developments, potential disruptions). A single agent doing all four sequentially takes four times as long and tends to produce shallow coverage across all areas.

Swarm research decomposes the topic, assigns each area to a specialist agent, runs all four investigations simultaneously, then merges the results. capturing cross-references between findings (a market trend explained by a technical innovation, a use case that validates a future prediction). The merge step is critical: it's not concatenation but synthesis, combining findings from different angles into a coherent narrative with citations.

## The Solution

**You write the decomposition, specialist research, and synthesis logic. Conductor handles parallel dispatch, synchronization, and per-agent traceability.**

`DecomposeWorker` breaks the research topic into four subtasks, each with a specific research area and targeted instructions. `FORK_JOIN` dispatches `Swarm1Worker` through `Swarm4Worker` to investigate market analysis, technical landscape, use cases, and future trends simultaneously. each returning structured findings with citations and confidence scores. After `JOIN` collects all four results, `MergeWorker` synthesizes the findings into a unified report with cross-references between areas, key themes, and a consensus assessment. Conductor runs all four agents in parallel (cutting research time by 4x), retries any agent that fails without re-running the others, and records each agent's findings separately for traceability.

### What You Write: Workers

Six workers run the swarm. Decomposing the topic, dispatching four parallel specialists for market, technical, use-case, and trend research, then merging findings.

| Worker | Task | What It Does |
|---|---|---|
| **DecomposeWorker** | `as_decompose` | Decompose worker. takes a research topic and breaks it into 4 subtasks, each assigned to a different swarm agent wit.. |
| **MergeWorker** | `as_merge` | Merge worker. combines results from all 4 swarm agents into a unified research report. Computes totalFindings, avgCo.. |
| **Swarm1Worker** | `as_swarm_1` | Swarm agent 1. Market Analysis specialist. Analyzes market trends, competitive landscape, and adoption rates. Return.. |
| **Swarm2Worker** | `as_swarm_2` | Swarm agent 2. Technical Landscape specialist. Surveys technical approaches, architectures, and key innovations. Ret.. |
| **Swarm3Worker** | `as_swarm_3` | Swarm agent 3. Use Cases specialist. Identifies and evaluates real-world use cases, deployments, and case studies. R.. |
| **Swarm4Worker** | `as_swarm_4` | Swarm agent 4. Future Trends specialist. Forecasts future developments, emerging trends, and potential disruptions. .. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
as_decompose
 │
 ▼
FORK_JOIN
 ├── as_swarm_1
 ├── as_swarm_2
 ├── as_swarm_3
 └── as_swarm_4
 │
 ▼
JOIN (wait for all branches)
as_merge

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
