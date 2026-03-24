# Document Review

Orchestrates document review through a multi-stage Conductor workflow.

**Input:** `matterId`, `documentBatch` | **Timeout:** 60s

## Pipeline

```
drv_ingest
    │
drv_classify
    │
drv_review
    │
drv_privilege
    │
drv_produce
```

## Workers

**ClassifyWorker** (`drv_classify`)

Outputs `classified`, `privilegedCount`, `documents`, `reviewed`.

**IngestWorker** (`drv_ingest`)

Outputs `documents`, `privilegedCount`, `classified`, `reviewed`.

**PrivilegeWorker** (`drv_privilege`)

Outputs `producible`, `privilegedCount`, `documents`, `classified`, `reviewed`.

**ProduceWorker** (`drv_produce`)

Outputs `producedCount`, `privilegedCount`, `documents`, `classified`, `reviewed`.

**ReviewWorker** (`drv_review`)

Outputs `reviewed`, `privilegedCount`, `documents`, `classified`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
