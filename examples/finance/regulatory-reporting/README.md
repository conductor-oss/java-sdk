# Regulatory Reporting in Java with Conductor

Regulatory reporting workflow: collect data, validate, format, submit, and confirm.

## The Problem

You need to submit a regulatory report to a governing body. This involves collecting the required data from source systems, validating it against regulatory rules and completeness checks, formatting it according to the regulator's specifications, submitting the report electronically, and confirming acceptance. Late or inaccurate filings result in fines, enforcement actions, or loss of operating licenses.

Without orchestration, you'd build a reporting pipeline that queries multiple databases, applies validation rules, formats XML/XBRL/CSV output, uploads to the regulator's portal, and checks for acceptance. manually handling data quality issues, retrying failed submissions, and meeting tight filing deadlines.

## The Solution

**You just write the reporting workers. Data collection, validation, regulatory formatting, submission, and acceptance confirmation. Conductor handles pipeline ordering, automatic retries when the regulatory portal is down, and complete filing lifecycle tracking for audit evidence.**

Each reporting concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect, validate, format, submit, confirm), retrying if the regulatory portal is down, tracking every filing with complete audit trail, and resuming from the last step if the process crashes.

### What You Write: Workers

Five workers cover the reporting pipeline: CollectDataWorker aggregates data from source systems, ValidateWorker applies regulatory rules, FormatWorker generates the required output format, SubmitWorker uploads to the regulatory portal, and ConfirmWorker checks for acceptance.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `reg_collect_data` | Collects data |
| **ConfirmWorker** | `reg_confirm` | Confirm. Computes and returns accepted, receipt number, confirmed at |
| **FormatWorker** | `reg_format` | Formatting report as |
| **SubmitWorker** | `reg_submit` | Submit. Computes and returns submission id, submitted at, deadline, submitted before |
| **ValidateWorker** | `reg_validate` | Validate. Computes and returns validated data, error count, passed |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
reg_collect_data
 │
 ▼
reg_validate
 │
 ▼
reg_format
 │
 ▼
reg_submit
 │
 ▼
reg_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
