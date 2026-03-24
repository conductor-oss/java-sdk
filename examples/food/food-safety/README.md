# Food Safety

Orchestrates food safety through a multi-stage Conductor workflow.

**Input:** `restaurantId`, `inspectorId` | **Timeout:** 60s

## Pipeline

```
fsf_inspect
    │
fsf_check_temps
    │
fsf_verify_hygiene
    │
fsf_certify
    │
fsf_record
```

## Workers

**CertifyWorker** (`fsf_certify`)

Outputs `certification`.

**CheckTempsWorker** (`fsf_check_temps`)

Reads `restaurantId`. Outputs `temps`.

**InspectWorker** (`fsf_inspect`)

Reads `inspectorId`, `restaurantId`. Outputs `findings`.

**RecordWorker** (`fsf_record`)

```java
result.addOutputData("report", Map.of("restaurantId", restaurantId != null ? restaurantId : "REST-10", "grade", "A", "score", 98, "status", "CERTIFIED"));
```

Reads `restaurantId`. Outputs `report`.

**VerifyHygieneWorker** (`fsf_verify_hygiene`)

Reads `restaurantId`. Outputs `hygiene`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
