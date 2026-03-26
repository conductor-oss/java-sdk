# Vendor Onboarding

Vendor onboarding: apply, verify credentials, evaluate, approve, and activate.

**Input:** `vendorName`, `category`, `country` | **Timeout:** 60s

## Pipeline

```
von_apply
    │
von_verify
    │
von_evaluate
    │
von_approve
    │
von_activate
```

## Workers

**ActivateWorker** (`von_activate`): Activates the vendor in the system.

Reads `vendorId`. Outputs `active`, `activatedAt`.

**ApplyWorker** (`von_apply`): Submits a vendor application.

Reads `category`, `vendorName`. Outputs `vendorId`.

**ApproveWorker** (`von_approve`): Approves or rejects a vendor based on score.

```java
boolean approved = score >= 70;
```

Reads `score`. Outputs `approved`.

**EvaluateWorker** (`von_evaluate`): Evaluates the vendor and assigns a score.

Outputs `score`, `tier`.

**VerifyWorker** (`von_verify`): Verifies vendor credentials.

Reads `vendorId`. Outputs `verified`, `checks`.

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
