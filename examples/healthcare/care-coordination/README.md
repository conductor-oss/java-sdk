# Care Coordination

Orchestrates care coordination through a multi-stage Conductor workflow.

**Input:** `patientId`, `condition`, `acuity` | **Timeout:** 60s

## Pipeline

```
ccr_assess_needs
    │
ccr_create_plan
    │
ccr_assign_team
    │
ccr_monitor
```

## Workers

**AssessNeedsWorker** (`ccr_assess_needs`): Assesses patient needs based on condition and acuity.

Reads `acuity`, `condition`, `patientId`. Outputs `needs`, `riskScore`, `complexCase`.

**AssignTeamWorker** (`ccr_assign_team`): Assembles a care team based on the care plan and acuity.

Reads `acuity`. Outputs `team`.

**CreatePlanWorker** (`ccr_create_plan`): Creates a care plan based on assessed needs.

Reads `needs`. Outputs `careplan`.

**MonitorWorker** (`ccr_monitor`): Activates monitoring for the patient with the care team.

Reads `team`. Outputs `active`, `checkInFrequency`, `nextCheckIn`.

## Tests

**33 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
