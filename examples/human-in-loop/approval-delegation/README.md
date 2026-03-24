# Approval Delegation

Approval delegation — reassign approval to another person via WAIT + SWITCH

**Output:** `done`

## Pipeline

```
ad_prepare
    │
initial_approval_wait [WAIT]
    │
delegation_switch [SWITCH]
  └─ delegate: delegated_approval_wait
    │
ad_finalize
```

## Workers

**FinalizeWorker** (`ad_finalize`): Worker for ad_finalize — finalizes the approval after it has been.

Outputs `done`.

**PrepareWorker** (`ad_prepare`): Worker for ad_prepare — prepares approval request data.

Outputs `ready`.

## Workflow Output

- `done`: `${finalize_ref.output.done}`

## Data Flow

**delegation_switch** [SWITCH]: `switchCaseValue` = `${initial_approval_ref.output.action}`

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
