# Assessment Creation

Orchestrates assessment creation through a multi-stage Conductor workflow.

**Input:** `courseId`, `assessmentType`, `topics` | **Timeout:** 60s

## Pipeline

```
asc_define_criteria
    │
asc_create_questions
    │
asc_review
    │
asc_publish
```

## Workers

**CreateQuestionsWorker** (`asc_create_questions`)

```java
int totalPoints = questions.stream().mapToInt(q -> (int) q.get("points")).sum();
```

Outputs `questions`, `questionCount`, `assessmentId`.

**DefineCriteriaWorker** (`asc_define_criteria`)

Reads `assessmentType`, `courseId`, `topics`. Outputs `criteria`, `criteriaCount`.

**PublishWorker** (`asc_publish`)

Reads `assessmentId`, `courseId`. Outputs `published`, `availableDate`.

**ReviewWorker** (`asc_review`)

Reads `questions`. Outputs `status`, `reviewer`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
