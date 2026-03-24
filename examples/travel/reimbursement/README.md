# Reimbursement in Java with Conductor

Reimbursement: submit, validate, approve, process, notify.

## The Problem

You need to process a travel reimbursement claim from submission through payment, the employee submits a claim with receipts and amounts, the system validates receipts and checks policy compliance, a manager approves or rejects the claim, the finance team processes the approved payment, and the employee receives notification of the reimbursement status. Each step depends on the previous one's outcome.

If validation flags a receipt as invalid but approval proceeds anyway, the company reimburses an unverified expense. If payment processing fails after approval, the employee sees "approved" but never receives the money. Without orchestration, you'd build a monolithic reimbursement handler that mixes receipt validation, approval routing, payment processing, and notifications. Making it impossible to change validation rules, add a second-level approval for large amounts, or audit which receipts were verified for which claims.

## The Solution

**You just write the claim submission, receipt validation, manager approval, payment processing, and notification logic. Conductor handles approval routing retries, payment sequencing, and reimbursement audit trails.**

SubmitWorker captures the reimbursement claim with receipt details, amounts, and expense category. ValidateWorker checks each receipt for validity (date, vendor, amount), verifies policy compliance, and confirms the total matches submitted receipts. ApproveWorker routes the validated claim to the employee's manager for approval, recording the approver and decision. ProcessWorker initiates the payment through payroll or direct deposit for the approved amount. NotifyWorker sends the employee a notification with the reimbursement status and expected payment date. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Expense validation, approval routing, payment processing, and notification workers each handle one stage of returning travel costs to the employee.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `rmb_approve` | Evaluates approval criteria and computes approved, approved by |
| **NotifyWorker** | `rmb_notify` | Notify. Computes and returns notified |
| **ProcessWorker** | `rmb_process` | Processes the reimbursement payment to the employee's account |
| **SubmitWorker** | `rmb_submit` | Submit. Computes and returns claim id, submitted |
| **ValidateWorker** | `rmb_validate` | Validate. Computes and returns valid, receipts verified |

### The Workflow

```
rmb_submit
 │
 ▼
rmb_validate
 │
 ▼
rmb_approve
 │
 ▼
rmb_process
 │
 ▼
rmb_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
