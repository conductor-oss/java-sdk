# Citizen Request

Orchestrates citizen request through a multi-stage Conductor workflow.

**Input:** `citizenId`, `requestType`, `description` | **Timeout:** 60s

## Pipeline

```
ctz_submit
    │
ctz_classify
    │
ctz_route
    │
ctz_resolve
    │
ctz_notify
```

## Workers

**ClassifyWorker** (`ctz_classify`)

Reads `requestId`. Outputs `category`, `priority`.

**NotifyWorker** (`ctz_notify`)

Reads `citizenId`, `resolution`. Outputs `notified`.

**ResolveWorker** (`ctz_resolve`)

Reads `department`. Outputs `resolution`.

**RouteWorker** (`ctz_route`)

Reads `category`. Outputs `department`, `assignee`.

**SubmitWorker** (`ctz_submit`)

Reads `citizenId`. Outputs `requestId`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
