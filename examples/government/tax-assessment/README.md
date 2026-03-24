# Tax Assessment

Orchestrates tax assessment through a multi-stage Conductor workflow.

**Input:** `propertyId`, `taxYear`, `ownerId` | **Timeout:** 60s

## Pipeline

```
txa_collect_data
    │
txa_assess_property
    │
txa_calculate
    │
txa_notify
    │
txa_appeal
```

## Workers

**AppealWorker** (`txa_appeal`)

Outputs `appealDeadline`, `appealOpen`.

**AssessPropertyWorker** (`txa_assess_property`)

Outputs `assessedValue`, `taxRate`.

**CalculateWorker** (`txa_calculate`)

```java
double amount = 450000 * 0.012;
```

Outputs `taxAmount`.

**CollectDataWorker** (`txa_collect_data`)

Reads `propertyId`. Outputs `propertyData`.

**NotifyWorker** (`txa_notify`)

Reads `ownerId`, `taxAmount`. Outputs `notified`.

## Tests

**3 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
