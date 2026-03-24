# Leave Management in Java with Conductor : Request, Balance Check, Manager Approval, Accrual Update, and Notification

## The Problem

You need to manage employee leave requests from submission through approval. An employee submits a leave request specifying the type (vacation, sick, personal, FMLA), start date, and number of days. The system must check the employee's available balance for that leave type to ensure they have sufficient hours. The request is then routed to the manager for approval, taking team coverage into account. Once approved, the leave balance must be debited and the payroll calendar updated. Finally, the employee receives confirmation, the team calendar is updated, and the manager is notified. If the balance check or approval is skipped, employees may overdraw their PTO or take leave without coverage arranged.

Without orchestration, you'd build a monolithic leave system that queries the accrual database, sends the manager an approval email, waits for a response, updates the balance, and notifies everyone. If the payroll system is temporarily unavailable for the balance update, you'd need retry logic. If the system crashes after approval but before updating the balance, the employee has approved leave but incorrect accruals. HR needs a complete audit trail of every leave request for labor law compliance.

## The Solution

**You just write the leave request, balance checking, manager approval, accrual update, and notification logic. Conductor handles approval routing, balance updates, and leave request audit trails.**

Each stage of the leave request is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of checking balances before approval, updating accruals only after approval, sending notifications as the final step, retrying if the payroll or HRIS system is temporarily unavailable, and maintaining a complete audit trail for labor law compliance.

### What You Write: Workers

Request intake, balance checking, approval routing, and calendar update workers handle leave requests through independent validation steps.

| Worker | Task | What It Does |
|---|---|---|
| **RequestWorker** | `lvm_request` | Registers the leave request with employee ID, leave type, start date, and duration |
| **CheckBalanceWorker** | `lvm_check_balance` | Queries the employee's available leave balance for the requested type and verifies sufficient hours |
| **ApproveWorker** | `lvm_approve` | Routes the request to the manager for approval, considering team coverage and blackout dates |
| **UpdateWorker** | `lvm_update` | Debits the leave balance, updates the payroll calendar, and records the approved absence |
| **NotifyWorker** | `lvm_notify` | Sends confirmation to the employee and updates the team calendar with the approved absence |

### The Workflow

```
lvm_request
 │
 ▼
lvm_check_balance
 │
 ▼
lvm_approve
 │
 ▼
lvm_update
 │
 ▼
lvm_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
