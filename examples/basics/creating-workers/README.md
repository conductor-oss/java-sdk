# Creating Workers

Demonstrates three worker patterns: sync transform, async fetch, and error-handling process

**Input:** `text`, `source` | **Timeout:** 120s

## Pipeline

```
simple_transform
    │
fetch_data
    │
safe_process
```

## Workers

**FetchDataWorker** (`fetch_data`): Worker that performs an async data fetch operation.

Reads `source`. Outputs `records`, `source`, `recordCount`.

**SafeProcessWorker** (`safe_process`): Worker that processes data records with error handling.

```java
int baseScore = Math.abs(record.hashCode()) % 100;
int adjustedScore = (baseScore + (i * 17)) % 100;
```

Reads `records`. Outputs `processedCount`, `results`, `summary`, `passCount`, `failCount`.
Returns `FAILED` on validation errors.

**SimpleTransformWorker** (`simple_transform`): Pure synchronous worker that transforms text input.

Reads `text`. Outputs `upper`, `lower`, `length`, `original`.

## Tests

**18 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
