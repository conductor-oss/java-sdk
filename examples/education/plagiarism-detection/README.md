# Plagiarism Detection

Orchestrates plagiarism detection through a multi-stage Conductor workflow.

**Input:** `studentId`, `assignmentId`, `documentText` | **Timeout:** 60s

## Pipeline

```
plg_submit
    │
plg_scan
    │
plg_compare
    │
result_switch [SWITCH]
  ├─ clean: plg_handle_clean
  └─ default: plg_handle_flagged
    │
plg_report
```

## Workers

**CompareWorker** (`plg_compare`)

```java
String verdict = similarity < 20 ? "clean" : "flagged";
```

Outputs `similarityScore`, `verdict`.

**HandleCleanWorker** (`plg_handle_clean`)

Reads `assignmentId`. Outputs `action`.

**HandleFlaggedWorker** (`plg_handle_flagged`)

Reads `similarityScore`, `studentId`. Outputs `action`, `notified`.

**ReportWorker** (`plg_report`)

Reads `studentId`, `verdict`. Outputs `generated`.

**ScanWorker** (`plg_scan`)

Reads `documentText`. Outputs `scanResults`.

**SubmitWorker** (`plg_submit`)

Reads `assignmentId`, `studentId`. Outputs `submissionId`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
