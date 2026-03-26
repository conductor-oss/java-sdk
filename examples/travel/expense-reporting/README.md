# Expense Reporting

Expense reporting: collect, categorize, submit, approve, reimburse.

**Input:** `employeeId`, `tripId` | **Timeout:** 60s

## Pipeline

```
exr_collect
    │
exr_categorize
    │
exr_submit
    │
exr_approve
    │
exr_reimburse
```

## Workers

**ApproveWorker** (`exr_approve`)

Outputs `approved`, `approvedBy`.

**CategorizeWorker** (`exr_categorize`)

Outputs `categorized`, `total`.

**CollectWorker** (`exr_collect`)

Outputs `receipts`.

**ReimburseWorker** (`exr_reimburse`)

Outputs `reimbursed`, `paymentDate`, `method`.

**SubmitWorker** (`exr_submit`)

Outputs `reportId`, `submitted`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
