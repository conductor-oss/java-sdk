# User Migration

Orchestrates user migration through a multi-stage Conductor workflow.

**Input:** `sourceDb`, `targetDb`, `batchSize` | **Timeout:** 60s

## Pipeline

```
umg_extract
    │
umg_transform
    │
umg_load
    │
umg_verify
    │
umg_notify
```

## Workers

**ExtractWorker** (`umg_extract`)

Reads `sourceDb`. Outputs `users`, `extractedCount`.

**LoadWorker** (`umg_load`)

Reads `targetDb`. Outputs `loadedCount`, `failedCount`.

**NotifyMigrationWorker** (`umg_notify`)

Outputs `notified`, `channel`.

**TransformWorker** (`umg_transform`)

Outputs `transformed`, `transformedCount`, `fieldsAdded`.

**VerifyMigrationWorker** (`umg_verify`)

```java
boolean match = loaded != null && loaded.equals(expected);
```

Reads `expected`, `loaded`. Outputs `allMatch`, `verificationResult`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
