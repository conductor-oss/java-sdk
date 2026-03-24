# Research Swarm: Decompose Topic into 4 Parallel Specialist Investigations

A research topic is decomposed into subtasks (e.g., `{id: "subtask-1", area: "Market Analysis"}`), then 4 swarm agents investigate in parallel via FORK_JOIN. Each agent produces findings and reports with its `agentId`. The merge worker combines all results.

## Workflow

```
researchTopic
       │
       ▼
┌──────────────────┐
│ as_decompose     │  Break into 4 subtasks
└──────┬───────────┘
       ▼
┌─── FORK_JOIN ──────────────────────────────────────────┐
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌───────┐│
│ │as_swarm_1  │ │as_swarm_2  │ │as_swarm_3  │ │as_    ││
│ │            │ │            │ │            │ │swarm_4││
│ └────────────┘ └────────────┘ └────────────┘ └───────┘│
└──────────────────────────┬─────────────────────────────┘
                           ▼
                   ┌──────────────┐
                   │ as_merge     │  Combine all findings
                   └──────────────┘
```

## Workers

**DecomposeWorker** (`as_decompose`) -- Splits topic into subtasks with IDs and areas.

**Swarm1-4Workers** (`as_swarm_1` through `as_swarm_4`) -- Each produces findings as `List.of(...)` and reports `agentId`.

**MergeWorker** (`as_merge`) -- Combines results from all 4 agents (null-safe via ternary).

## Tests

51 tests cover decomposition, all 4 swarm agents, and result merging.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
