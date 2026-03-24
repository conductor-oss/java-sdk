# Escalation Chain

Multi-level escalation chain -- submit, WAIT for approval (Analyst -> Manager -> VP), then finalize.

**Input:** `requestId` | **Timeout:** 300s

**Output:** `decision`, `respondedAt`

## Pipeline

```
esc_submit
    │
esc_approval [WAIT]
    │
esc_finalize
```

## Workers

**EscFinalizeWorker** (`esc_finalize`): Finalizes the escalation decision. Records outcome with audit trail.

Reads `decision`, `level`. Outputs `done`, `decision`, `resolvedAt`, `approved`, `finalizedAt`.

**EscSubmitWorker** (`esc_submit`): Submits a request for escalation-chain approval.

- `amount > 100000` &rarr; `"VP"`
- `amount > 10000` &rarr; `"Manager"`

```java
boolean validRequest = !requestId.equals("unknown") && requestId.length() > 0;
```

Reads `amount`, `requestId`. Outputs `submitted`, `requestId`, `firstLevel`, `submittedAt`.

## Workflow Output

- `decision`: `${wait_ref.output.decision}`
- `respondedAt`: `${wait_ref.output.respondedAt}`

## Data Flow

**esc_submit**: `requestId` = `${workflow.input.requestId}`
**esc_finalize**: `decision` = `${wait_ref.output.decision}`, `level` = `${wait_ref.output.respondedAt}`

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
