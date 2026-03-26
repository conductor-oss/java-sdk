# Contract Analysis

Orchestrates contract analysis through a multi-stage Conductor workflow.

**Input:** `contractId`, `contractType` | **Timeout:** 60s

## Pipeline

```
cna_parse
    │
cna_extract
    │
cna_analyze
    │
cna_summarize
```

## Workers

**AnalyzeWorker** (`cna_analyze`)

Outputs `overallRisk`, `parsed`, `clauses`, `risks`.

**ExtractWorker** (`cna_extract`)

Outputs `clauses`, `parsed`, `risks`.

**ParseWorker** (`cna_parse`)

Outputs `parsed`, `clauses`, `risks`.

**SummarizeWorker** (`cna_summarize`)

Outputs `summaryId`, `parsed`, `clauses`, `risks`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
