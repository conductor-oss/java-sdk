# Multi Level Approval

Multi-level approval chain: Manager -> Director -> VP. Each level uses a WAIT task for human input, followed by a SWITCH to check approval. Rejection at any level terminates the workflow.

**Input:** `requestId`, `requestor`

**Output:** `requestId`, `submitted`, `finalized`

## Pipeline

```
mla_submit
    │
wait_manager_approval [WAIT]
    │
check_manager_decision [SWITCH]
  ├─ false: terminate_manager_rejected
  └─ default: wait_director_approval → check_director_decision
```

## Workers

**FinalizeWorker** (`mla_finalize`): Worker for mla_finalize task -- finalizes a fully-approved request.

Outputs `finalized`.

**SubmitWorker** (`mla_submit`): Worker for mla_submit task -- submits a request for multi-level approval.

Outputs `submitted`.

## Workflow Output

- `requestId`: `${workflow.input.requestId}`
- `submitted`: `${submit_request.output.submitted}`
- `finalized`: `${finalize_request.output.finalized}`

## Data Flow

**mla_submit**: `requestId` = `${workflow.input.requestId}`, `requestor` = `${workflow.input.requestor}`
**wait_manager_approval** [WAIT]: `requestId` = `${workflow.input.requestId}`
**check_manager_decision** [SWITCH]: `approved` = `${wait_manager_approval.output.approved}`

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
