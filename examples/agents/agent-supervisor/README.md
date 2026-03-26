# Supervisor Agent: Plan, Fan Out to Coder/Tester/Documenter, Review

A supervisor plans a feature implementation, then fans out to three specialist agents in parallel via FORK_JOIN: a coder (creates files, counts lines), a tester (uses regex patterns for test generation), and a documenter (also uses regex). The supervisor reviews all outputs and produces action items with metrics.

## Workflow

```
feature, requirements
       │
       ▼
┌──────────────┐
│ sup_plan     │
└──────┬───────┘
       ▼
┌─── FORK_JOIN ──────────────────────────────────┐
│ ┌──────────────────┐ ┌────────────┐ ┌─────────┐│
│ │sup_coder_agent   │ │sup_tester_ │ │sup_doc_ ││
│ │(files, LOC)      │ │  agent     │ │ agent   ││
│ └──────────────────┘ └────────────┘ └─────────┘│
└──────────────────────────┬─────────────────────┘
                           ▼
                   ┌──────────────┐
                   │ sup_review   │  Compile review + action items
                   └──────────────┘
```

## Workers

**PlanWorker** (`sup_plan`) -- Creates a plan with feature name and priority.

**CoderAgentWorker** (`sup_coder_agent`) -- Generates `filesCreated` (controller, service, repository paths) and counts `linesOfCode`.

**TesterAgentWorker** (`sup_tester_agent`) -- Uses `Pattern`/`Matcher` for test generation.

**DocumenterAgentWorker** (`sup_documenter_agent`) -- Uses `Pattern`/`Matcher` for documentation generation.

**ReviewWorker** (`sup_review`) -- Compiles action items and metrics including `linesOfCode`.

## Tests

58 tests cover planning, all three agent types, and comprehensive review logic.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
