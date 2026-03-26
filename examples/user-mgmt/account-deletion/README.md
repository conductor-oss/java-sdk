# Account Deletion

Orchestrates account deletion through a multi-stage Conductor workflow.

**Input:** `userId`, `reason` | **Timeout:** 60s

## Pipeline

```
acd_verify
    │
acd_backup
    │
acd_delete
    │
acd_confirm
```

## Workers

**BackupWorker** (`acd_backup`)

Outputs `backupId`, `sizeBytes`, `retainDays`.

**ConfirmDeletionWorker** (`acd_confirm`)

Outputs `confirmationSent`, `gdprCompliant`.

**DeleteAccountWorker** (`acd_delete`)

Reads `userId`. Outputs `deleted`, `tablesCleared`, `deletedAt`.

**VerifyDeletionWorker** (`acd_verify`)

Reads `reason`, `userId`. Outputs `verified`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
