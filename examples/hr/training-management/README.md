# Training Management

Training management: assign, track, assess, certify, record.

**Input:** `employeeId`, `courseId`, `courseName` | **Timeout:** 60s

## Pipeline

```
trm_assign
    │
trm_track
    │
trm_assess
    │
trm_certify
    │
trm_record
```

## Workers

**AssessWorker** (`trm_assess`)

Outputs `score`, `passed`, `passingScore`.

**AssignWorker** (`trm_assign`)

Reads `courseId`, `employeeId`. Outputs `enrollmentId`, `dueDate`.

**CertifyWorker** (`trm_certify`)

Reads `employeeId`, `score`. Outputs `certificationId`, `expiresIn`.

**RecordWorker** (`trm_record`)

Reads `certificationId`, `employeeId`. Outputs `recorded`.

**TrackWorker** (`trm_track`)

Reads `enrollmentId`. Outputs `progress`, `modulesCompleted`, `totalModules`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
