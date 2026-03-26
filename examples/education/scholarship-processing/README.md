# Scholarship Processing

Orchestrates scholarship processing through a multi-stage Conductor workflow.

**Input:** `studentId`, `scholarshipId`, `gpa`, `financialNeed` | **Timeout:** 60s

## Pipeline

```
scp_apply
    │
scp_evaluate
    │
scp_rank
    │
scp_award
    │
scp_notify
```

## Workers

**ApplyWorker** (`scp_apply`)

Reads `scholarshipId`, `studentId`. Outputs `applicationId`.

**AwardWorker** (`scp_award`)

```java
boolean awarded = rank <= 2;
```

Reads `applicationId`, `rank`. Outputs `awarded`, `amount`.

**EvaluateWorker** (`scp_evaluate`)

```java
int score = (int) Math.round(gpa * 17.5 + needScore);
```

Reads `applicationId`, `financialNeed`, `gpa`. Outputs `score`, `eligible`.

**NotifyWorker** (`scp_notify`)

Reads `amount`, `awarded`, `studentId`. Outputs `notified`, `message`.

**RankWorker** (`scp_rank`)

```java
int rank = score >= 90 ? 1 : score >= 80 ? 2 : 3;
```

Reads `applicationId`, `score`. Outputs `rank`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
