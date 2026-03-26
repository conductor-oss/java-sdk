# Database Integration

Orchestrates database integration through a multi-stage Conductor workflow.

**Input:** `sourceDb`, `targetDb`, `query`, `transformRules` | **Timeout:** 60s

## Pipeline

```
dbi_connect
    │
dbi_query
    │
dbi_transform
    │
dbi_write
    │
dbi_verify
```

## Workers

**ConnectWorker** (`dbi_connect`): Connects to source and target databases.

```java
String srcId = "conn-src-" + System.currentTimeMillis();
```

Reads `sourceDb`, `targetDb`. Outputs `sourceConnectionId`, `targetConnectionId`.

**QueryWorker** (`dbi_query`): Queries data from source database.

Reads `query`. Outputs `rows`, `rowCount`.

**TransformWorker** (`dbi_transform`): Transforms queried rows.

Reads `rows`. Outputs `transformedRows`, `transformedCount`.

**VerifyWorker** (`dbi_verify`): Verifies source and target row counts match.

```java
boolean verified = String.valueOf(sourceCount).equals(String.valueOf(writtenCount));
```

Reads `sourceCount`, `writtenCount`. Outputs `verified`.

**WriteWorker** (`dbi_write`): Writes transformed rows to target database.

Reads `transformedRows`. Outputs `writtenCount`, `writtenAt`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
