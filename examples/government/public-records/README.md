# Public Records

Orchestrates public records through a multi-stage Conductor workflow.

**Input:** `requesterId`, `recordType`, `query` | **Timeout:** 60s

## Pipeline

```
pbr_request
    │
pbr_search
    │
pbr_verify
    │
pbr_redact
    │
pbr_release
```

## Workers

**RedactWorker** (`pbr_redact`)

Outputs `redactedDocs`, `redactionsApplied`.

**ReleaseWorker** (`pbr_release`)

Reads `requesterId`. Outputs `released`, `count`.

**RequestWorker** (`pbr_request`)

Reads `requesterId`. Outputs `requestId`.

**SearchWorker** (`pbr_search`)

Reads `query`. Outputs `documents`, `count`.

**VerifyWorker** (`pbr_verify`)

Reads `documents`. Outputs `verifiedDocs`, `allVerified`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
