# Medical Imaging

Medical Imaging: acquire, process, analyze, report, store to PACS

**Input:** `studyId`, `patientId`, `modality`, `bodyPart` | **Timeout:** 60s

## Pipeline

```
img_acquire
    │
img_process
    │
img_analyze
    │
img_report
    │
img_store
```

## Workers

**AcquireWorker** (`img_acquire`)

Reads `bodyPart`, `modality`, `patientId`. Outputs `imageId`, `format`, `slices`, `resolution`.

**AnalyzeImageWorker** (`img_analyze`)

Reads `bodyPart`. Outputs `findings`, `modelVersion`.

**ProcessImageWorker** (`img_process`)

Reads `imageId`. Outputs `processedImageId`, `enhancements`.

**ReportWorker** (`img_report`)

Outputs `reportId`, `status`, `generatedAt`.

**StoreWorker** (`img_store`)

Reads `studyId`. Outputs `archived`, `pacsId`, `archivedAt`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
