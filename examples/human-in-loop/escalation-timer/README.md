# Escalation Timer

Escalation timer demo -- auto-approve after timeout using a WAIT task and external escalation checker.

**Input:** `requestId`, `autoApproveAfterMs` | **Timeout:** 300s

**Output:** `decision`, `method`

## Pipeline

```
et_submit
    │
approval_wait [WAIT]
    │
et_process
```

## Workers

**ProcessWorker** (`et_process`): Worker for et_process -- processes the approval decision.

Reads `decision`, `method`. Outputs `processed`.

**SubmitWorker** (`et_submit`): Worker for et_submit -- submits a request for approval.

Reads `requestId`. Outputs `submitted`.

## Workflow Output

- `decision`: `${wait_ref.output.decision}`
- `method`: `${wait_ref.output.method}`

## Data Flow

**et_submit**: `requestId` = `${workflow.input.requestId}`
**et_process**: `decision` = `${wait_ref.output.decision}`, `method` = `${wait_ref.output.method}`

## Tests

**17 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
