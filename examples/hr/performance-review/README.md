# Performance Review

Performance review workflow: self-eval, manager eval, calibrate, finalize.

**Input:** `employeeId`, `reviewPeriod`, `managerId` | **Timeout:** 60s

## Pipeline

```
pfr_self_eval
    │
pfr_manager_eval
    │
pfr_calibrate
    │
pfr_finalize
```

## Workers

**CalibrateWorker** (`pfr_calibrate`)

```java
double avg = (selfRating + managerRating) / 2.0;
```

Reads `managerRating`, `selfRating`. Outputs `finalRating`, `band`.

**FinalizeWorker** (`pfr_finalize`)

Reads `calibratedRating`, `employeeId`. Outputs `reviewId`, `finalized`, `meritIncrease`.

**ManagerEvalWorker** (`pfr_manager_eval`)

Reads `managerId`. Outputs `rating`, `feedback`.

**SelfEvalWorker** (`pfr_self_eval`)

Reads `employeeId`. Outputs `rating`, `strengths`, `areas`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
