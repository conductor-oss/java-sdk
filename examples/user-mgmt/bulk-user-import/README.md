# Bulk User Import in Java Using Conductor

## The Problem

You need to import thousands of users from a CSV or JSON file. Parsing the file into individual records, validating each record for required fields, email format, and duplicate detection, batch-inserting the valid records into your user database, and generating a summary report showing how many were imported versus rejected. Each step depends on the previous one's output.

If the parser misreads a column mapping and the validator doesn't catch it, you insert users with swapped first and last names across your entire tenant. If batch insertion fails halfway through but there's no tracking of which records succeeded, you either re-import everything (creating duplicates) or manually reconcile the partial load. Without orchestration, you'd build a monolithic import script that mixes file parsing, field validation, database batch operations, and report generation. Making it impossible to support new file formats, adjust validation rules, or retry a failed batch without re-processing the entire file.

## The Solution

**You just write the file-parsing, record-validation, batch-insertion, and reporting workers. Conductor handles the import pipeline and batch data flow.**

ParseFileWorker reads the uploaded file from the provided URL, detects the format (CSV, JSON, Excel), and extracts individual user records with their field mappings. ValidateRecordsWorker checks every record for required fields, email format validity, and constraint violations: separating 1,238 valid records from 12 invalid ones and flagging the rejection reasons. BatchInsertWorker groups the valid records into batches (13 batches for 1,238 users) and inserts them into the user database, tracking how many were successfully created. ReportWorker compiles the import summary, total parsed, valid, inserted, and failed, and generates a report URL for the admin who initiated the import. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

ParseFileWorker extracts records from CSV/JSON/Excel, ValidateRecordsWorker checks fields and detects duplicates, BatchInsertWorker loads valid users into the database, and ReportWorker compiles the import summary.

| Worker | Task | What It Does |
|---|---|---|
| **BatchInsertWorker** | `bui_batch_insert` | Inserts valid user records into the database in batches, tracking inserted count and average batch time |
| **ParseFileWorker** | `bui_parse_file` | Parses the uploaded file (CSV, JSON, Excel) from the provided URL, extracting individual user records |
| **ReportWorker** | `bui_report` | Generates an import summary report with parsed, valid, and inserted counts, returning a downloadable report URL |
| **ValidateRecordsWorker** | `bui_validate` | Validates each parsed record for required fields and duplicates, separating valid records from invalid ones with rejection reasons |

Replace with real identity provider and database calls and ### The Workflow

```
bui_parse_file
 │
 ▼
bui_validate
 │
 ▼
bui_batch_insert
 │
 ▼
bui_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
