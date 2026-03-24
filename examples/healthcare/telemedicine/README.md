# Telemedicine

Telemedicine: schedule, connect, consult, prescribe, follow-up

**Input:** `visitId`, `patientId`, `providerId`, `reason` | **Timeout:** 60s

## Pipeline

```
tlm_schedule
    │
tlm_connect
    │
tlm_consult
    │
tlm_prescribe
    │
tlm_followup
```

## Workers

**ConnectWorker** (`tlm_connect`)

Reads `visitId`. Outputs `connectionId`, `quality`, `latency`.

**ConsultWorker** (`tlm_consult`)

Reads `reason`. Outputs `diagnosis`, `notes`, `followUpNeeded`, `durationMinutes`.

**FollowUpWorker** (`tlm_followup`)

```java
String followUpDate = needed ? "2024-03-25" : null;
```

Reads `followUpNeeded`. Outputs `followUpDate`, `followUpScheduled`.

**PrescribeWorker** (`tlm_prescribe`)

Reads `diagnosis`. Outputs `prescription`, `sentToPharmacy`.

**ScheduleWorker** (`tlm_schedule`)

Reads `reason`, `visitId`. Outputs `sessionUrl`, `scheduledAt`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
