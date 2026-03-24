# Grading Workflow

Orchestrates grading workflow through a multi-stage Conductor workflow.

**Input:** `studentId`, `courseId`, `assignmentId` | **Timeout:** 60s

## Pipeline

```
grd_submit
    │
grd_grade
    │
grd_review
    │
grd_record
    │
grd_notify
```

## Workers

**GradeWorker** (`grd_grade`)

Reads `submissionId`. Outputs `score`, `rubric`, `feedback`.

**NotifyWorker** (`grd_notify`)

Reads `finalScore`, `studentId`. Outputs `notified`, `method`.

**RecordWorker** (`grd_record`)

Reads `courseId`, `finalScore`, `studentId`. Outputs `recorded`.

**ReviewWorker** (`grd_review`)

Reads `score`. Outputs `finalScore`, `reviewed`.

**SubmitWorker** (`grd_submit`)

Reads `assignmentId`, `studentId`. Outputs `submissionId`, `onTime`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
