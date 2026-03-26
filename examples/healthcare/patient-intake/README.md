# Patient Intake

Patient intake: registration, insurance verification, triage, provider assignment

**Input:** `patientId`, `name`, `chiefComplaint`, `insuranceId` | **Timeout:** 1800s

## Pipeline

```
pit_register
    │
pit_verify_insurance
    │
pit_triage
    │
pit_assign
```

## Workers

**AssignWorker** (`pit_assign`): Assigns patient to care provider based on department and triage level.

```java
case "Emergency" -> "ER Room " + (1 + Math.abs(dept.hashCode() % 10));
case "Fast Track" -> "FT Room " + (1 + Math.abs(dept.hashCode() % 5));
```

Reads `department`, `triageLevel`. Outputs `provider`, `room`, `estimatedWait`.

**RegisterWorker** (`pit_register`): Registers a patient and captures vitals. Real vitals generation within normal ranges.

```java
double temp = 97.5 + RNG.nextDouble() * 2.5;
temp = Math.round(temp * 10.0) / 10.0;
```

Reads `name`, `patientId`. Outputs `registeredAt`, `vitals`, `mrn`.

**TriageWorker** (`pit_triage`): Performs real triage assessment based on chief complaint and vitals.

Reads `chiefComplaint`, `vitals`. Outputs `triageLevel`, `department`, `assessedAt`.

**VerifyInsuranceWorker** (`pit_verify_insurance`): Verifies insurance coverage. Real verification with plan lookup and copay calculation.

```java
deductibleRemaining = 200 + Math.abs(insuranceId.hashCode() % 800);
```

Reads `insuranceId`. Outputs `verified`, `plan`, `copay`, `deductibleRemaining`.

## Tests

**19 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
