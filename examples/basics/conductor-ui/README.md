# Conductor Ui

A 3-step workflow designed for exploring the Conductor UI

**Input:** `userId`, `action` | **Timeout:** 60s

## Pipeline

```
ui_step_one
    │
ui_step_two
    │
ui_step_three
```

## Workers

**StepOneWorker** (`ui_step_one`): Step One — Processes user action input.

Reads `action`, `userId`. Outputs `result`, `timestamp`.

**StepThreeWorker** (`ui_step_three`): Step Three — Summarizes results from steps one and two.

```java
String path = "ui_step_one -> ui_step_two -> ui_step_three";
```

Reads `allData`. Outputs `summary`, `path`.

**StepTwoWorker** (`ui_step_two`): Step Two — Enriches data from step one with metadata.

```java
double score = Math.min(1.0, wordCount * 0.1);
score = Math.round(score * 100.0) / 100.0;
```

Reads `previousResult`. Outputs `result`, `enriched`, `score`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
