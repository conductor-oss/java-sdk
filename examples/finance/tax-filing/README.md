# Tax Filing in Java with Conductor

Tax filing: collect data, calculate tax, validate, file return, confirm.

## The Problem

You need to prepare and file a tax return. The workflow collects income, deduction, and credit data from source systems, calculates the tax liability using current tax tables and rules, validates the return for accuracy and completeness, files it electronically with the tax authority, and confirms acceptance. Incorrect calculations result in penalties; late filing incurs interest charges and potential audits.

Without orchestration, you'd build a tax preparation service that aggregates data from W-2s, 1099s, and other sources, applies tax logic inline, validates against IRS rules, transmits via MeF, and checks for acknowledgment. manually handling data discrepancies, retrying failed transmissions, and meeting the April 15 deadline.

## The Solution

**You just write the tax workers. Income data collection, liability calculation, return validation, electronic filing, and acceptance confirmation. Conductor handles step sequencing, automatic retries when the e-filing system is unavailable, and complete return preparation tracking for audit defense.**

Each tax-filing concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect data, calculate tax, validate, file, confirm), retrying if the e-filing system is unavailable, tracking every return's preparation lifecycle, and resuming from the last step if the process crashes.

### What You Write: Workers

Five workers manage the filing lifecycle: CollectDataWorker aggregates income and deduction data, CalculateTaxWorker computes liability, ValidateFilingWorker checks accuracy, FileReturnWorker transmits to the tax authority, and ConfirmSubmissionWorker verifies acceptance.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateTaxWorker** | `txf_calculate_tax` | Calculate Tax. Computes and returns tax liability, taxable income, effective rate |
| **CollectDataWorker** | `txf_collect_data` | Collect Data. Computes and returns gross income, deductions, credits, w2 count |
| **ConfirmSubmissionWorker** | `txf_confirm_submission` | Confirms the submission |
| **FileReturnWorker** | `txf_file_return` | File Return. Computes and returns filing id, confirmation number, filed at |
| **ValidateFilingWorker** | `txf_validate_filing` | Validate Filing. Computes and returns validated, validation checks, warnings |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
txf_collect_data
 │
 ▼
txf_calculate_tax
 │
 ▼
txf_validate_filing
 │
 ▼
txf_file_return
 │
 ▼
txf_confirm_submission

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
