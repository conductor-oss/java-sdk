# Profile Update

Orchestrates profile update through a multi-stage Conductor workflow.

**Input:** `userId`, `updates` | **Timeout:** 60s

## Pipeline

```
pfu_validate
    │
pfu_update
    │
pfu_sync
    │
pfu_notify
```

## Workers

**NotifyChangesWorker** (`pfu_notify`): Notifies the user of profile changes.

Outputs `notified`.

**SyncProfileWorker** (`pfu_sync`): Syncs profile changes to downstream services.

Outputs `synced`, `services`.

**UpdateProfileWorker** (`pfu_update`): Applies the profile updates.

Reads `validatedFields`. Outputs `updatedFields`, `updatedAt`.

**ValidateFieldsWorker** (`pfu_validate`): Validates profile update fields.

Reads `updates`, `userId`. Outputs `validatedFields`, `allValid`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
