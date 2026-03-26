# Trouble Ticket

Orchestrates trouble ticket through a multi-stage Conductor workflow.

**Input:** `customerId`, `issueType`, `description` | **Timeout:** 60s

## Pipeline

```
tbt_open
    │
tbt_diagnose
    │
tbt_assign
    │
tbt_resolve
    │
tbt_close
```

## Workers

**AssignWorker** (`tbt_assign`)

Reads `ticketId`. Outputs `assignee`, `eta`.

**CloseWorker** (`tbt_close`)

Reads `ticketId`. Outputs `closedAt`, `satisfactionSurvey`.

**DiagnoseWorker** (`tbt_diagnose`)

Reads `ticketId`. Outputs `category`, `rootCause`.

**OpenWorker** (`tbt_open`)

Reads `customerId`. Outputs `ticketId`, `priority`.

**ResolveWorker** (`tbt_resolve`)

Reads `assignee`. Outputs `resolution`, `downtime`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
