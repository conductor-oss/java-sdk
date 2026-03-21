# Time Tracking in Java with Conductor : Timesheet Submission, Validation, Manager Approval, and Payroll Processing

## The Problem

You need to process employee timesheets from submission through payroll every week. An employee submits their timesheet for the week ending date, logging hours against project codes and cost centers. The entries must be validated. checking that total hours are within policy limits, overtime is flagged for FLSA compliance, and hours are charged to active project codes. The validated timesheet routes to the employee's manager for approval, where the manager reviews hours against expected allocations and overtime justifications. Once approved, the timesheet feeds into payroll processing, converting hours into gross pay calculations, applying overtime rates (1.5x for hours over 40), and posting to the general ledger by cost center. If validation is skipped, employees can charge hours to closed projects or exceed overtime limits without authorization.

Without orchestration, you'd build a monolithic timesheet system that collects entries, runs validation rules, sends the manager an approval email, polls for a response, and pushes approved hours to payroll. If the payroll system is down on Friday night when timesheets are due, you'd need retry logic. If the system crashes after approval but before payroll processing, the employee has an approved timesheet but no paycheck. Finance and labor law auditors need a complete trail showing every timesheet's path from submission through payment.

## The Solution

**You just write the timesheet submission, overtime validation, manager approval, and payroll processing logic. Conductor handles clock-in retries, overtime calculations, and timesheet audit trails.**

Each stage of timesheet processing is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of validating entries before they reach the manager, processing payroll only after manager approval, retrying if the payroll or ERP system is temporarily unavailable, and maintaining a complete audit trail from submission through payment for FLSA and labor law compliance. ### What You Write: Workers

Clock-in recording, break management, overtime calculation, and payroll export workers each handle one aspect of workforce time management.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `ttk_submit` | Records the employee's timesheet submission for the week ending date, assigns a timesheet ID, and locks the entries for validation |
| **ValidateWorker** | `ttk_validate` | Checks entries against project codes, calculates total hours and overtime, and flags policy violations (over 40 hours, inactive projects) |
| **ApproveWorker** | `ttk_approve` | Routes the validated timesheet to the employee's manager for approval with overtime justification if applicable |
| **ProcessWorker** | `ttk_process` | Converts approved hours into payroll. applies regular and overtime rates, posts labor costs to cost centers in the general ledger |

### The Workflow

```
ttk_submit
 │
 ▼
ttk_validate
 │
 ▼
ttk_approve
 │
 ▼
ttk_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
