# Data Validation in Java Using Conductor : Required Fields, Type Checking, Range Validation, and Reporting

## The Problem

You receive data from external partners, user submissions, or upstream systems, and you need to validate it before it enters your production database. Validation is layered: first check that required fields exist (a record without an email is immediately invalid), then verify data types (age must be a number, not a string), then validate value ranges (age must be 0-150, email must contain "@"). Each layer filters out invalid records, so type checking only runs on records that passed required field checks. You need a report showing exactly how many records failed at each stage and why.

Without orchestration, you'd write a single validation method with nested if/else blocks. There's no visibility into which validation layer rejected a record. If you want to add a new layer (uniqueness check, cross-field validation), you'd modify deeply coupled code. If the process crashes after two of three checks, you'd restart from scratch.

## The Solution

**You just write the required-field, type-checking, range-validation, and reporting workers. Conductor handles sequential validation layer execution, per-layer retry on failure, and precise tracking of how many records were rejected at each stage.**

Each validation layer is a simple, independent worker. The required fields checker filters out records missing mandatory fields. The type checker verifies data types on the surviving records. The range validator checks value constraints. The report generator tallies errors per layer and counts the records that passed all checks. Conductor executes them in sequence, passes only valid records from one layer to the next, retries if a check fails, and tracks exactly how many records were rejected at each stage. ### What You Write: Workers

Five workers implement layered validation: loading records, checking required fields are present, verifying data types (name is String, age is Number), validating value ranges (age 0-150, email contains @), and generating an error report by category.

| Worker | Task | What It Does |
|---|---|---|
| **CheckRangesWorker** | `vd_check_ranges` | Checks value ranges: age must be 0-150, email must contain "@". |
| **CheckRequiredWorker** | `vd_check_required` | Checks that required fields (name, email, age) are present and non-empty. |
| **CheckTypesWorker** | `vd_check_types` | Checks that field types are correct: name must be String, age must be Number. |
| **GenerateReportWorker** | `vd_generate_report` | Generates a validation summary report. |
| **LoadRecordsWorker** | `vd_load_records` | Loads records for validation. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
vd_load_records
 │
 ▼
vd_check_required
 │
 ▼
vd_check_types
 │
 ▼
vd_check_ranges
 │
 ▼
vd_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
