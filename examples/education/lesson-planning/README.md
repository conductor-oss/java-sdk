# Lesson Planning

Orchestrates lesson planning through a multi-stage Conductor workflow.

**Input:** `courseId`, `lessonTitle`, `week` | **Timeout:** 60s

## Pipeline

```
lpl_define_objectives
    │
lpl_create_content
    │
lpl_review
    │
lpl_publish
```

## Workers

**CreateContentWorker** (`lpl_create_content`)

Reads `lessonTitle`. Outputs `lessonPlan`, `sectionCount`.

**DefineObjectivesWorker** (`lpl_define_objectives`)

Reads `lessonTitle`. Outputs `objectives`, `objectiveCount`.

**PublishWorker** (`lpl_publish`)

Reads `courseId`, `week`. Outputs `published`, `visibleToStudents`.

**ReviewWorker** (`lpl_review`)

Outputs `status`, `feedback`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
