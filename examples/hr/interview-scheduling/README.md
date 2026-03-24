# Interview Scheduling

Interview scheduling: availability, schedule, invite, confirm, remind.

**Input:** `candidateName`, `interviewers`, `role` | **Timeout:** 60s

## Pipeline

```
ivs_availability
    │
ivs_schedule
    │
ivs_invite
    │
ivs_confirm
    │
ivs_remind
```

## Workers

**AvailabilityWorker** (`ivs_availability`)

Reads `interviewers`. Outputs `availableSlots`.

**ConfirmWorker** (`ivs_confirm`)

Reads `interviewId`. Outputs `confirmed`, `confirmations`.

**InviteWorker** (`ivs_invite`)

Reads `candidateName`. Outputs `invited`, `calendarEvent`.

**RemindWorker** (`ivs_remind`)

Reads `candidateName`. Outputs `reminded`.

**ScheduleWorker** (`ivs_schedule`)

Outputs `interviewId`, `scheduledTime`, `room`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
