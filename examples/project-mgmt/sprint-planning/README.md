# Sprint Planning

Sprint planning: select stories, estimate, assign, create sprint.

**Input:** `sprintNumber`, `teamCapacity` | **Timeout:** 60s

## Pipeline

```
spn_select_stories
    │
spn_estimate
    │
spn_assign
    │
spn_create_sprint
```

## Workers

**AssignWorker** (`spn_assign`)

Outputs `assignments`.

**CreateSprintWorker** (`spn_create_sprint`)

Reads `sprintNumber`. Outputs `sprint`.

**EstimateWorker** (`spn_estimate`)

Outputs `estimatedStories`, `totalPoints`.

**SelectStoriesWorker** (`spn_select_stories`)

Reads `teamCapacity`. Outputs `stories`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
