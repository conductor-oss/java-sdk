# Mental Health

Mental Health: intake, assess, treatment plan, track progress

**Input:** `patientId`, `referralReason`, `provider` | **Timeout:** 60s

## Pipeline

```
mh_intake
    │
mh_assess
    │
mh_treatment_plan
    │
mh_track_progress
```

## Workers

**AssessWorker** (`mh_assess`)

Outputs `diagnosis`, `icdCode`, `severity`, `phq9Score`, `gad7Score`.

**IntakeWorker** (`mh_intake`)

Reads `patientId`, `referralReason`. Outputs `intakeData`.

**TrackProgressWorker** (`mh_track_progress`)

Reads `baselineScore`. Outputs `trackingActive`, `measures`, `checkInSchedule`, `baselinePhq9`.

**TreatmentPlanWorker** (`mh_treatment_plan`)

Reads `diagnosis`. Outputs `treatmentPlan`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
