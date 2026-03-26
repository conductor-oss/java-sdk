# Event Fundraising

Orchestrates event fundraising through a multi-stage Conductor workflow.

**Input:** `eventName`, `eventDate`, `ticketPrice` | **Timeout:** 60s

## Pipeline

```
efr_plan
    │
efr_promote
    │
efr_execute
    │
efr_collect
    │
efr_reconcile
```

## Workers

**CollectWorker** (`efr_collect`)

```java
int revenue = attendees * ticketPrice;
```

Reads `attendees`, `ticketPrice`. Outputs `revenue`, `donations`, `totalRaised`.

**ExecuteWorker** (`efr_execute`)

Reads `attendees`. Outputs `attendees`, `expenses`, `satisfaction`.

**PlanWorker** (`efr_plan`)

Reads `eventDate`, `eventName`. Outputs `eventId`, `venue`, `capacity`.

**PromoteWorker** (`efr_promote`)

Reads `eventName`. Outputs `registrations`, `channels`.

**ReconcileWorker** (`efr_reconcile`)

```java
int net = revenue - expenses;
```

Reads `eventId`, `expenses`, `revenue`. Outputs `fundraiser`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
