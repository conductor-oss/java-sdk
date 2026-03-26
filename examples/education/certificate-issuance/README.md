# Certificate Issuance

Orchestrates certificate issuance through a multi-stage Conductor workflow.

**Input:** `studentId`, `courseId`, `courseName` | **Timeout:** 60s

## Pipeline

```
cer_verify_completion
    │
cer_generate
    │
cer_sign
    │
cer_issue
    │
cer_record
```

## Workers

**GenerateCertificateWorker** (`cer_generate`)

Reads `courseName`. Outputs `certificateId`, `format`.

**IssueCertificateWorker** (`cer_issue`)

Reads `certificateId`, `studentId`. Outputs `issued`, `deliveryMethod`.

**RecordCertificateWorker** (`cer_record`)

Reads `certificateId`. Outputs `recorded`, `blockchain`.

**SignCertificateWorker** (`cer_sign`)

Reads `certificateId`. Outputs `signed`, `signedBy`.

**VerifyCompletionWorker** (`cer_verify_completion`)

Reads `courseId`, `studentId`. Outputs `completed`, `completionDate`, `grade`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
