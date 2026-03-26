# Retrospective

Retrospective: collect feedback, categorize, prioritize, create action items.

**Input:** `sprintId`, `teamName`, `facilitator` | **Timeout:** 60s

## Pipeline

```
rsp_collect_feedback
    │
rsp_categorize
    │
rsp_prioritize
    │
rsp_action_items
```

## Workers

**ActionItemsWorker** (`rsp_action_items`)

Reads `sprintId`. Outputs `actionItems`, `totalItems`.

**CategorizeWorker** (`rsp_categorize`)

Reads `sprintId`. Outputs `categories`.

**CollectFeedbackWorker** (`rsp_collect_feedback`)

Reads `sprintId`. Outputs `feedback`, `responseCount`.

**PrioritizeWorker** (`rsp_prioritize`)

Reads `sprintId`. Outputs `priorities`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
