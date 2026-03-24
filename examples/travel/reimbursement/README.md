# Reimbursement

Reimbursement: submit, validate, approve, process, notify.

**Input:** `employeeId`, `amount`, `category`, `receiptCount` | **Timeout:** 60s

## Pipeline

```
rmb_submit
    │
rmb_validate
    │
rmb_approve
    │
rmb_process
    │
rmb_notify
```

## Workers

**ApproveWorker** (`rmb_approve`)

Reads `amount`, `claimId`. Outputs `approved`, `approvedBy`.

**NotifyWorker** (`rmb_notify`)

Reads `amount`, `employeeId`. Outputs `notified`.

**ProcessWorker** (`rmb_process`)

Reads `employeeId`. Outputs `paymentId`, `status`.

**SubmitWorker** (`rmb_submit`)

Reads `amount`, `employeeId`. Outputs `claimId`, `submitted`.

**ValidateWorker** (`rmb_validate`)

Reads `claimId`, `receiptCount`. Outputs `valid`, `receiptsVerified`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
