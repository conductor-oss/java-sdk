# ETL Basics in Java Using Conductor: Extract, Transform, Validate, Load, and Confirm

Your company's data lives in 3 databases, 2 third-party APIs, and a shared Google Drive folder that someone updates manually on Fridays. The analyst's "ETL pipeline" is a Python script on their laptop that connects to each source, munges the data with pandas, and writes to the warehouse. It works great: until the VPN disconnects at row 50,000, or the analyst goes on vacation and nobody else can run it, or the Google Drive CSV changes column order and the script silently loads first names into the email field. There's no retry, no validation, and no record of what was loaded when. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a durable ETL pipeline, extract, transform, validate, load, and confirm, as independent workers with automatic retries and per-stage tracking.

## The Problem

You need to move data from a source system to a destination, but the data needs cleaning along the way. Names have extra whitespace. Emails are inconsistently cased. Amounts are stored as strings that need parsing to numbers. Some records are incomplete and shouldn't be loaded at all. You need a pipeline that extracts, transforms, validates, loads, and confirms, with each step depending on the previous one's output. If the load fails after transforming 10,000 records, you shouldn't have to re-extract and re-transform from scratch.

Without orchestration, you'd write a single ETL method that connects to the source, reads records, transforms inline, validates inline, writes to the destination, and hopes nothing crashes. If the destination database is briefly unavailable, the entire pipeline fails with no retry. There's no visibility into how many records were extracted vs: transformed vs: validated vs: loaded, and debugging a data quality issue means adding println statements throughout coupled code.

## The Solution

**You just write the extract, transform, validate, load, and confirm workers. Conductor handles the extract-transform-validate-load-confirm sequence, retries when the destination is unavailable, and per-stage record count tracking.**

Each stage of the ETL pipeline is a simple, independent worker. The extractor reads records from the source. The transformer cleans and normalizes fields (trimming, lowercasing, type conversion). The validator filters out incomplete or invalid records. The loader writes clean records to the destination. The confirmer verifies the load completed successfully. Conductor executes them in sequence, passes records between stages, retries if the destination is unavailable, and tracks exactly how many records survived each stage.

### What You Write: Workers

Five workers form the classic ETL pipeline: extracting records from a source, transforming them (trimming names, lowercasing emails, parsing amounts), validating output quality, loading into the destination, and confirming successful completion.

| Worker | Task | What It Does |
|---|---|---|
| `ExtractDataWorker` | `el_extract_data` | Reads 4 customer records from the source, each with untrimmed names, uppercase emails, and string amounts |
| `TransformDataWorker` | `el_transform_data` | Cleans records: trims whitespace from names, lowercases emails, parses amount strings to doubles |
| `ValidateOutputWorker` | `el_validate_output` | Filters out records with empty name, empty email, or amount <= 0 (e.g., Charlie with amount 0.00) |
| `LoadDataWorker` | `el_load_data` | Writes the validated records to the destination and returns a loaded count |
| `ConfirmLoadWorker` | `el_confirm_load` | Confirms the load completed by returning status `ETL_COMPLETE` with the final loaded count |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
el_extract_data
 │
 ▼
el_transform_data
 │
 ▼
el_validate_output
 │
 ▼
el_load_data
 │
 ▼
el_confirm_load

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
