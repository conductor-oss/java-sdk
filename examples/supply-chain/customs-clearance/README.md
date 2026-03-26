# Customs Clearance

Customs clearance: declare, validate, calculate duty, clear, and release.

**Input:** `shipmentId`, `origin`, `destination`, `goods` | **Timeout:** 60s

## Pipeline

```
cst_declare
    │
cst_validate
    │
cst_calculate_duty
    │
cst_clear
    │
cst_release
```

## Workers

**CalculateDutyWorker** (`cst_calculate_duty`)

```java
double dutyAmount = Math.round(totalValue * dutyRate * 100.0) / 100.0;
```

Reads `goods`. Outputs `dutyAmount`, `dutyRate`.

**ClearWorker** (`cst_clear`)

Reads `declarationId`, `dutyAmount`. Outputs `clearanceId`, `cleared`.

**DeclareWorker** (`cst_declare`)

Reads `origin`, `shipmentId`. Outputs `declarationId`.

**ReleaseWorker** (`cst_release`)

Reads `shipmentId`. Outputs `released`.

**ValidateWorker** (`cst_validate`)

Reads `declarationId`. Outputs `valid`, `documents`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
