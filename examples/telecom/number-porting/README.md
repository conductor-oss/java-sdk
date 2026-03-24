# Number Porting

Orchestrates number porting through a multi-stage Conductor workflow.

**Input:** `phoneNumber`, `fromCarrier`, `toCarrier` | **Timeout:** 60s

## Pipeline

```
npt_request
    │
npt_validate
    │
npt_coordinate
    │
npt_port
    │
npt_verify
```

## Workers

**CoordinateWorker** (`npt_coordinate`)

Reads `fromCarrier`, `toCarrier`. Outputs `portDate`, `windowStart`, `windowEnd`.

**PortWorker** (`npt_port`)

Reads `phoneNumber`. Outputs `ported`, `completedAt`.

**RequestWorker** (`npt_request`)

Reads `phoneNumber`, `toCarrier`. Outputs `portId`.

**ValidateWorker** (`npt_validate`)

Reads `fromCarrier`, `phoneNumber`. Outputs `eligible`, `accountActive`.

**VerifyWorker** (`npt_verify`)

Reads `phoneNumber`, `toCarrier`. Outputs `verified`, `testCallPassed`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
