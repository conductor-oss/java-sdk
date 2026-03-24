# Referral Management

Referral Management: create, match specialist, schedule, track, close

**Input:** `referralId`, `patientId`, `specialty`, `reason` | **Timeout:** 60s

## Pipeline

```
ref_create
    │
ref_match_specialist
    │
ref_schedule
    │
ref_track
    │
ref_close
```

## Workers

**CloseReferralWorker** (`ref_close`)

Reads `outcome`, `referralId`. Outputs `closedStatus`, `closedAt`.

**CreateReferralWorker** (`ref_create`)

Reads `reason`, `referralId`, `specialty`. Outputs `insurancePlan`, `urgency`, `createdAt`.

**MatchSpecialistWorker** (`ref_match_specialist`)

Reads `insurancePlan`, `specialty`. Outputs `specialistId`, `specialistName`, `distance`, `nextAvailable`.

**ScheduleReferralWorker** (`ref_schedule`)

Reads `patientId`, `specialistId`. Outputs `appointmentId`, `appointmentDate`, `appointmentTime`.

**TrackReferralWorker** (`ref_track`)

Reads `appointmentId`, `referralId`. Outputs `outcome`, `visitCompleted`, `specialistNotes`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
