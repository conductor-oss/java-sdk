# Hr Onboarding

HR onboarding workflow: create profile, provision systems, assign mentor, training.

**Input:** `employeeName`, `department`, `startDate` | **Timeout:** 60s

## Pipeline

```
hro_create_profile
    │
hro_provision
    │
hro_assign_mentor
    │
hro_training
```

## Workers

**AssignMentorWorker** (`hro_assign_mentor`): Assigns a mentor from the same department to the new employee.

Reads `department`. Outputs `mentorId`, `mentorName`.

**CreateProfileWorker** (`hro_create_profile`): Creates an employee profile during onboarding.

Reads `department`, `employeeName`. Outputs `employeeId`, `email`.

**ProvisionWorker** (`hro_provision`): Provisions systems (laptop, email, Slack, Jira) for a new employee.

Reads `employeeId`. Outputs `systems`, `laptop`.

**TrainingWorker** (`hro_training`): Creates a training plan for the new employee.

Reads `employeeId`. Outputs `planId`, `courses`, `durationWeeks`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
