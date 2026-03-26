# Wait Task Basics

WAIT Task basics -- pause a workflow at a WAIT task and resume it by completing the task externally.

**Input:** `requestId` | **Timeout:** 120s

**Output:** `prepared`, `result`

## Pipeline

```
wait_before
    │
wait_for_approval [WAIT]
    │
wait_after
```

## Workers

**WaitAfterWorker** (`wait_after`): Worker for wait_after — processes data after the WAIT task completes.

Reads `approval`, `requestId`. Outputs `result`.

**WaitBeforeWorker** (`wait_before`): Worker for wait_before — prepares data before the WAIT task.

Outputs `prepared`.

## Workflow Output

- `prepared`: `${wait_before_ref.output.prepared}`
- `result`: `${wait_after_ref.output.result}`

## Data Flow

**wait_before**: `requestId` = `${workflow.input.requestId}`
**wait_after**: `requestId` = `${workflow.input.requestId}`, `approval` = `${wait_for_approval_ref.output.approval}`

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
