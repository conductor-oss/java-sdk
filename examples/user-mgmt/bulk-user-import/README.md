# Bulk User Import

Orchestrates bulk user import through a multi-stage Conductor workflow.

**Input:** `fileUrl`, `format` | **Timeout:** 60s

## Pipeline

```
bui_parse_file
    │
bui_validate
    │
bui_batch_insert
    │
bui_report
```

## Workers

**BatchInsertWorker** (`bui_batch_insert`)

Outputs `insertedCount`, `batches`, `avgBatchTime`.

**ParseFileWorker** (`bui_parse_file`)

Reads `fileUrl`, `format`. Outputs `records`, `totalParsed`.

**ReportWorker** (`bui_report`)

Reads `inserted`, `parsed`, `valid`. Outputs `reportUrl`, `summary`.

**ValidateRecordsWorker** (`bui_validate`)

Reads `totalParsed`. Outputs `validRecords`, `validCount`, `invalidCount`, `errors`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
