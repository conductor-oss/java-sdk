# Expense Management in Java with Conductor

Expense management: submit, validate receipts, categorize, approve, reimburse.

## The Problem

You need to process employee expense reports from submission to reimbursement. An employee submits an expense with receipt, the receipt is validated for authenticity and policy compliance (amount limits, eligible categories), the expense is categorized for accounting, a manager approves it, and the employee is reimbursed. Processing expenses without receipt validation invites fraud; reimbursing without approval violates spending controls.

Without orchestration, you'd build a single expense service that uploads receipts, validates amounts, categorizes line items, emails managers for approval, and triggers payroll reimbursement. manually tracking approval status through email threads, retrying failed receipt OCR, and logging everything for tax audit.

## The Solution

**You just write the expense workers. Report submission, receipt validation, GL categorization, manager approval, and reimbursement. Conductor handles sequential processing, automatic retries when the receipt validation service times out, and complete expense tracking from submission to reimbursement.**

Each expense concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, validate, categorize, approve, reimburse), retrying if the receipt validation service times out, tracking every expense from submission to reimbursement, and resuming from the last step if the process crashes.

### What You Write: Workers

Five workers manage the expense lifecycle: SubmitExpenseWorker captures the report, ValidateReceiptsWorker checks receipt authenticity, CategorizeWorker assigns GL codes, ApproveExpenseWorker routes for manager sign-off, and ReimburseWorker triggers payment.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveExpenseWorker** | `exp_approve_expense` | Approves the expense |
| **CategorizeWorker** | `exp_categorize` | Categorizes the input and computes final category, gl code, tax deductible |
| **ReimburseWorker** | `exp_reimburse` | Reimburse the data and computes reimbursement status, payment id, expected date |
| **SubmitExpenseWorker** | `exp_submit_expense` | Handles submit expense |
| **ValidateReceiptsWorker** | `exp_validate_receipts` | Checking receipt for expense |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
exp_submit_expense
 │
 ▼
exp_validate_receipts
 │
 ▼
exp_categorize
 │
 ▼
exp_approve_expense
 │
 ▼
exp_reimburse

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
