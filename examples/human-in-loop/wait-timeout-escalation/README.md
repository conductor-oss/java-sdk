# Wait Timeout Escalation

WAIT with Timeout and Escalation -- prepare, WAIT for human input, then process the response.

**Input:** `requestId` | **Timeout:** 120s

## Pipeline

```
wte_prepare
    │
wait_for_response [WAIT]
    │
wte_process
```

## Workers

**WteEscalateWorker** (`wte_escalate`): Worker for wte_escalate — handles escalation when the WAIT task times out.

Outputs `escalated`, `escalatedTo`.

**WtePrepareWorker** (`wte_prepare`): Worker for wte_prepare — prepares the request before the WAIT task.

Outputs `ready`.

**WteProcessWorker** (`wte_process`): Worker for wte_process — processes the response received after the WAIT task completes.

Reads `response`. Outputs `result`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
