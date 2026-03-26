# Human Group Claim

Group Assignment with Claim — intake, wait for human group claim, resolve

**Output:** `ticketId`, `queued`, `closed`

## Pipeline

```
hgc_intake
    │
group_assigned_wait [WAIT]
    │
hgc_resolve
```

## Workers

**IntakeWorker** (`hgc_intake`): Worker for hgc_intake — processes intake and returns queued=true.

Outputs `queued`.

**ResolveWorker** (`hgc_resolve`): Worker for hgc_resolve — resolves the ticket and returns closed=true.

Outputs `closed`.

## Workflow Output

- `ticketId`: `${workflow.input.ticketId}`
- `queued`: `${intake_ref.output.queued}`
- `closed`: `${resolve_ref.output.closed}`

## Data Flow

**hgc_intake**: `ticketId` = `${workflow.input.ticketId}`, `assignedGroup` = `${workflow.input.assignedGroup}`
**group_assigned_wait** [WAIT]: `assignedGroup` = `${workflow.input.assignedGroup}`, `ticketId` = `${workflow.input.ticketId}`, `instructions` = `${workflow.input.instructions}`
**hgc_resolve**: `ticketId` = `${workflow.input.ticketId}`, `claimedBy` = `${group_assigned_ref.output.claimedBy}`

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
