# Discharge Planning

Orchestrates discharge planning through a multi-stage Conductor workflow.

**Input:** `patientId`, `admissionId`, `diagnosis` | **Timeout:** 60s

## Pipeline

```
dsc_assess_readiness
    │
dsc_create_plan
    │
dsc_coordinate
    │
dsc_educate
    │
dsc_schedule_followup
```

## Workers

**AssessReadinessWorker** (`dsc_assess_readiness`): Evaluates discharge readiness for a patient.

Reads `patientId`. Outputs `readiness`, `needs`, `lengthOfStay`.

**CoordinateWorker** (`dsc_coordinate`): Coordinates discharge services.

Reads `services`. Outputs `servicesArranged`, `confirmedServices`.

**CreateDischargePlanWorker** (`dsc_create_plan`): Creates a discharge plan based on readiness assessment.

Reads `needs`. Outputs `services`, `medications`, `followUpNeeds`.

**EducateWorker** (`dsc_educate`): Provides patient education for discharge.

Reads `diagnosis`, `medications`. Outputs `educated`, `topics`.

**ScheduleFollowupWorker** (`dsc_schedule_followup`): Schedules follow-up appointments after discharge.

Reads `followUpNeeds`. Outputs `appointmentDate`, `appointments`.

## Tests

**24 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
