# Student Progress

Orchestrates student progress through a multi-stage Conductor workflow.

**Input:** `studentId`, `semester` | **Timeout:** 60s

## Pipeline

```
spr_collect_grades
    │
spr_analyze
    │
spr_generate_report
    │
spr_notify
```

## Workers

**AnalyzeWorker** (`spr_analyze`)

```java
String standing = gpa >= 3.5 ? "Dean's List" : gpa >= 2.0 ? "Good Standing" : "Academic Probation";
```

Outputs `gpa`, `standing`, `analysis`.

**CollectGradesWorker** (`spr_collect_grades`)

Reads `semester`, `studentId`. Outputs `grades`, `courseCount`.

**GenerateReportWorker** (`spr_generate_report`)

Reads `studentId`. Outputs `generated`, `format`.

**NotifyWorker** (`spr_notify`)

Reads `gpa`, `studentId`. Outputs `notified`, `recipients`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
