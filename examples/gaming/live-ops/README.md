# Live Ops

Orchestrates live ops through a multi-stage Conductor workflow.

**Input:** `eventName`, `startDate`, `endDate` | **Timeout:** 60s

## Pipeline

```
lop_schedule_event
    │
lop_configure
    │
lop_deploy
    │
lop_monitor
    │
lop_close
```

## Workers

**CloseWorker** (`lop_close`)

```java
r.addOutputData("event", Map.of("eventId", eventId != null ? eventId : "EVT-748", "participants", 15000, "rewards_distributed", 12800, "status", "CLOSED"));
```

Reads `eventId`. Outputs `event`.

**ConfigureWorker** (`lop_configure`)

Reads `eventId`. Outputs `config`.

**DeployWorker** (`lop_deploy`)

Reads `eventId`. Outputs `deployed`, `servers`, `regions`.

**MonitorWorker** (`lop_monitor`)

Reads `eventId`. Outputs `participants`, `engagement`, `issues`.

**ScheduleEventWorker** (`lop_schedule_event`)

Reads `eventName`. Outputs `eventId`, `scheduled`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
