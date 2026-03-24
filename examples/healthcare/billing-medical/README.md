# Billing Medical

Medical Billing: code procedures, verify coverage, submit claim, track payment

**Input:** `encounterId`, `patientId`, `providerId`, `procedures` | **Timeout:** 60s

## Pipeline

```
mbl_code_procedures
    │
mbl_verify_coverage
    │
mbl_submit_claim
    │
mbl_track_payment
```

## Workers

**CodeProceduresWorker** (`mbl_code_procedures`)

Reads `encounterId`. Outputs `cptCodes`, `icdCodes`, `totalCharge`.

**SubmitClaimWorker** (`mbl_submit_claim`)

```java
double expected = Math.round(total * (coverage / 100) * 100.0) / 100.0;
```

Reads `coverage`, `totalCharge`. Outputs `claimId`, `expectedPayment`, `submittedAt`, `clearinghouse`.

**TrackPaymentWorker** (`mbl_track_payment`)

Reads `claimId`. Outputs `paymentStatus`, `expectedPaymentDate`, `eraReceived`.

**VerifyCoverageWorker** (`mbl_verify_coverage`)

Reads `cptCodes`. Outputs `coveragePercent`, `copay`, `deductibleApplied`, `allCovered`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
