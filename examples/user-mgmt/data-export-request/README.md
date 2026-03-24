# Data Export Request

Orchestrates data export request through a multi-stage Conductor workflow.

**Input:** `userId`, `exportFormat`, `dataCategories` | **Timeout:** 60s

## Pipeline

```
der_validate
    │
der_collect
    │
der_package
    │
der_deliver
```

## Workers

**CollectDataWorker** (`der_collect`)

Reads `categories`. Outputs `collectedData`, `totalRecords`.

**DeliverExportWorker** (`der_deliver`)

Reads `userId`. Outputs `delivered`, `expiresAt`.

**PackageDataWorker** (`der_package`)

Reads `format`. Outputs `packageUrl`, `sizeBytes`.

**ValidateExportWorker** (`der_validate`)

Reads `exportFormat`, `userId`. Outputs `valid`, `identity`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
