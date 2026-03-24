# Content Archival

Orchestrates content archival through a multi-stage Conductor workflow.

**Input:** `archiveJobId`, `ageThresholdDays`, `contentCategory` | **Timeout:** 60s

## Pipeline

```
car_identify_content
    │
car_compress
    │
car_store_cold
    │
car_index_archive
    │
car_verify_integrity
```

## Workers

**CompressWorker** (`car_compress`)

Reads `compressedPath`. Outputs `compressedPath`, `compressedSizeMb`, `compressionRatio`, `checksum`.

**IdentifyContentWorker** (`car_identify_content`)

Reads `itemCount`. Outputs `itemCount`, `totalSizeMb`, `oldestItem`, `newestItem`.

**IndexArchiveWorker** (`car_index_archive`)

Reads `indexId`. Outputs `indexId`, `searchable`, `indexedAt`.

**StoreColdWorker** (`car_store_cold`)

Reads `coldStoragePath`. Outputs `coldStoragePath`, `storageClass`, `retrievalTime`, `storedAt`.

**VerifyIntegrityWorker** (`car_verify_integrity`)

Reads `verified`. Outputs `verified`, `checksumMatch`, `verifiedAt`, `retentionPolicy`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
