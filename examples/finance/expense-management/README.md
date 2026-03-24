# Expense Management

Expense management: submit, validate receipts, categorize, approve, reimburse.

**Input:** `expenseId`, `employeeId`, `amount`, `category`, `receiptUrl` | **Timeout:** 60s

## Pipeline

```
exp_submit_expense
    │
exp_validate_receipts
    │
exp_categorize
    │
exp_approve_expense
    │
exp_reimburse
```

## Workers

**ApproveExpenseWorker** (`exp_approve_expense`)

Reads `amount`, `employeeId`. Outputs `approved`, `approvedAmount`, `approver`.

**CategorizeWorker** (`exp_categorize`)

Reads `category`, `expenseId`. Outputs `finalCategory`, `glCode`, `taxDeductible`.

**ReimburseWorker** (`exp_reimburse`)

Reads `approvedAmount`, `employeeId`. Outputs `reimbursementStatus`, `paymentId`, `expectedDate`.

**SubmitExpenseWorker** (`exp_submit_expense`)

Reads `amount`, `employeeId`, `expenseId`. Outputs `submittedAt`, `referenceNumber`.

**ValidateReceiptsWorker** (`exp_validate_receipts`)

Reads `expenseId`. Outputs `valid`, `receiptDate`, `merchantName`, `ocrConfidence`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
