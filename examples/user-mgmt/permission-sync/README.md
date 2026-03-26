# Permission Sync

Orchestrates permission sync through a multi-stage Conductor workflow.

**Input:** `sourceSystem`, `targetSystems` | **Timeout:** 60s

## Pipeline

```
pms_scan_systems
    │
pms_diff
    │
pms_sync
    │
pms_verify
```

## Workers

**DiffWorker** (`pms_diff`)

Outputs `diffs`, `totalDiffs`.

**ScanSystemsWorker** (`pms_scan_systems`)

Reads `source`, `targets`. Outputs `sourcePermissions`, `targetPermissions`, `scannedAt`.

**SyncWorker** (`pms_sync`)

Outputs `syncedCount`, `syncResults`.

**VerifyWorker** (`pms_verify`)

Outputs `allVerified`, `verifiedAt`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
