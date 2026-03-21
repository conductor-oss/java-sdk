# Payroll Workflow in Java with Conductor

Payroll processing: collect hours, calculate gross, apply deductions, process, distribute stubs. ## The Problem

You need to process payroll for a department. This means collecting timesheets and hours worked for the pay period, calculating gross pay (salary, overtime, bonuses), applying deductions (taxes, benefits, retirement contributions), processing the net payments, and distributing pay stubs to employees. Incorrect gross calculations result in wage violations; missed deductions create tax liability.

Without orchestration, you'd build a batch payroll job that queries the timesheet system, computes pay inline, applies tax tables, triggers ACH payments, and generates stubs. manually handling employees who clock in late, retrying failed payment submissions, and logging everything to satisfy Department of Labor audits.

## The Solution

**You just write the payroll workers. Timesheet collection, gross pay calculation, deduction application, payment processing, and stub distribution. Conductor handles sequential processing, automatic retries when the payment processor is unavailable, and detailed payroll run tracking for Department of Labor compliance.**

Each payroll concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect hours, calculate gross, apply deductions, process payments, distribute stubs), retrying if the payment processor is unavailable, tracking every payroll run with full calculation details, and resuming from the last step if the process crashes. ### What You Write: Workers

Five workers manage the payroll cycle: CollectHoursWorker gathers timesheets, CalculateGrossWorker computes earnings, ApplyDeductionsWorker handles taxes and benefits, ProcessPayrollWorker submits the payment batch, and DistributeStubsWorker delivers pay statements.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyDeductionsWorker** | `prl_apply_deductions` | Apply Deductions. Computes and returns net payroll, total deductions, federal tax, state tax |
| **CalculateGrossWorker** | `prl_calculate_gross` | Calculate Gross. Computes and returns gross payroll, employee count |
| **CollectHoursWorker** | `prl_collect_hours` | Collecting hours for period |
| **DistributeStubsWorker** | `prl_distribute_stubs` | Distribute Stubs. Computes and returns distributed count, method |
| **ProcessPayrollWorker** | `prl_process_payroll` | Process Payroll. Computes and returns batch id, bank reference |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
prl_collect_hours
 │
 ▼
prl_calculate_gross
 │
 ▼
prl_apply_deductions
 │
 ▼
prl_process_payroll
 │
 ▼
prl_distribute_stubs

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
