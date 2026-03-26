# Volunteer Coordination

Orchestrates volunteer coordination through a multi-stage Conductor workflow.

**Input:** `volunteerName`, `skills`, `availability` | **Timeout:** 60s

## Pipeline

```
vol_register
    │
vol_match
    │
vol_schedule
    │
vol_track
    │
vol_thank
```

## Workers

**MatchWorker** (`vol_match`)

Reads `volunteerId`. Outputs `opportunity`.

**RegisterWorker** (`vol_register`)

Reads `volunteerName`. Outputs `volunteerId`, `registered`.

**ScheduleWorker** (`vol_schedule`)

```java
r.addOutputData("scheduled", true); r.addOutputData("date", "2026-03-15"); r.addOutputData("shift", "9:00 AM - 1:00 PM"); return r;
```

Reads `volunteerId`. Outputs `scheduled`, `date`, `shift`.

**ThankWorker** (`vol_thank`)

Reads `hours`, `volunteerName`. Outputs `volunteer`.

**TrackWorker** (`vol_track`)

Reads `volunteerId`. Outputs `hoursLogged`, `totalHours`, `eventsAttended`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
