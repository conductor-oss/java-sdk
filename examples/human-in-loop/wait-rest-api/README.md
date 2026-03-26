# Wait Rest Api

Demonstrates completing a WAIT task via REST API for human-in-the-loop approval

**Output:** `decision`, `result`

## Pipeline

```
wapi_prepare
    │
WAIT [WAIT]
    │
decision_switch [SWITCH]
  ├─ rejected: wapi_handle_decision
  └─ default: wapi_handle_decision
```

## Workers

**HandleDecisionWorker** (`wapi_handle_decision`): Worker for wapi_handle_decision — handles the outcome of the approval decision.

Reads `decision`. Outputs `result`.

**PrepareWorker** (`wapi_prepare`): Worker for wapi_prepare — prepares the workflow for the WAIT approval gate.

Outputs `ready`.

## Workflow Output

- `decision`: `${wait_ref.output.decision}`
- `result`: `${decision_switch_ref.output.result}`

## Data Flow

**decision_switch** [SWITCH]: `switchCaseValue` = `${wait_ref.output.decision}`

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
