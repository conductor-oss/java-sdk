# Wait Sdk

Demonstrates completing a WAIT task via the Conductor SDK (TaskClient)

**Output:** `ticketId`, `initStatus`, `finalResult`

## Pipeline

```
wsdk_init
    │
WAIT [WAIT]
    │
wsdk_finalize
```

## Workers

**FinalizeWorker** (`wsdk_finalize`): Worker for wsdk_finalize — closes a ticket after the WAIT task is resolved.

Reads `ticketId`. Outputs `result`.

**InitWorker** (`wsdk_init`): Worker for wsdk_init — initializes a ticket with status "open".

Reads `ticketId`. Outputs `ticketId`, `status`.

## Workflow Output

- `ticketId`: `${init.output.ticketId}`
- `initStatus`: `${init.output.status}`
- `finalResult`: `${finalize.output.result}`

## Data Flow

**wsdk_init**: `ticketId` = `${workflow.input.ticketId}`
**WAIT** [WAIT]: `ticketId` = `${init.output.ticketId}`
**wsdk_finalize**: `ticketId` = `${init.output.ticketId}`

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
