# Benefits Enrollment

Benefits enrollment workflow: present options, select, validate, enroll, confirm.

**Input:** `employeeId`, `enrollmentPeriod` | **Timeout:** 60s

## Pipeline

```
ben_present
    │
ben_select
    │
ben_validate
    │
ben_enroll
    │
ben_confirm
```

## Workers

**ConfirmWorker** (`ben_confirm`)

Reads `enrollmentId`. Outputs `confirmed`, `cardsMailed`.

**EnrollWorker** (`ben_enroll`)

Reads `employeeId`. Outputs `enrollmentId`, `monthlyPremium`, `effectiveDate`.

**PresentWorker** (`ben_present`)

Reads `employeeId`. Outputs `options`.

**SelectWorker** (`ben_select`)

Outputs `selections`.

**ValidateWorker** (`ben_validate`)

Reads `selections`. Outputs `validSelections`, `valid`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
