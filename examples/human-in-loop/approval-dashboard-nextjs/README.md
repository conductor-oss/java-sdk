# Approval Dashboard Nextjs

Next.js Full-Stack Approval Dashboard -- SIMPLE task processes the request, then WAIT task pauses for human approval.

**Input:** `type`, `title`, `amount`, `requester` | **Timeout:** 120s

**Output:** `processed`, `approved`, `approver`

## Pipeline

```
nxt_process
    │
nxt_approval [WAIT]
```

## Workers

**NxtProcessWorker** (`nxt_process`): Worker for nxt_process -- processes an approval request before.

Reads `amount`, `requester`, `title`, `type`. Outputs `processed`, `type`, `title`, `amount`, `requester`.

## Workflow Output

- `processed`: `${nxt_process_ref.output.processed}`
- `approved`: `${nxt_approval_ref.output.approved}`
- `approver`: `${nxt_approval_ref.output.approver}`

## Data Flow

**nxt_process**: `type` = `${workflow.input.type}`, `title` = `${workflow.input.title}`, `amount` = `${workflow.input.amount}`, `requester` = `${workflow.input.requester}`
**nxt_approval** [WAIT]: `type` = `${workflow.input.type}`, `title` = `${workflow.input.title}`, `amount` = `${workflow.input.amount}`, `requester` = `${workflow.input.requester}`

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
