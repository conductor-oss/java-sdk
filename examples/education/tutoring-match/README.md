# Tutoring Match

Orchestrates tutoring match through a multi-stage Conductor workflow.

**Input:** `studentId`, `subject`, `preferredTime` | **Timeout:** 60s

## Pipeline

```
tut_student_request
    │
tut_match_tutor
    │
tut_schedule
    │
tut_confirm
```

## Workers

**ConfirmWorker** (`tut_confirm`)

Reads `sessionId`. Outputs `confirmed`, `reminderSet`.

**MatchTutorWorker** (`tut_match_tutor`)

Reads `subject`. Outputs `tutorId`, `tutorName`, `rating`.

**ScheduleWorker** (`tut_schedule`)

Reads `preferredTime`, `studentId`, `tutorId`. Outputs `sessionId`, `sessionTime`, `location`.

**StudentRequestWorker** (`tut_student_request`)

Reads `studentId`, `subject`. Outputs `requestId`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
