# Sequential Tasks in Java with Conductor

Sequential ETL pipeline. extract, transform, load. Three workers process data in order. ## The Problem

You need to run an ETL pipeline where each phase strictly depends on the previous one: extract raw records from a data source, transform them by adding computed fields (grade classification, normalized scores) in a specified format, then load the transformed records into a destination system. The transform step cannot start until extraction is complete because it needs the raw data. The load step cannot start until transformation is complete because it needs the enriched records. If the load step fails after transforming 1,000 records, you need to resume from the load step. not re-extract and re-transform.

Without orchestration, you'd chain method calls in a single process. `extract()` feeds into `transform()` feeds into `load()`. If `load()` throws an exception after `transform()` completed successfully, you re-run the entire pipeline because the intermediate transformed data was only in memory. Adding retry logic to each step means nested try/catch blocks, and there is no record of what each step produced.

## The Solution

**You just write the extract, transform, and load workers. Conductor handles the sequencing, data passing, and per-step durability.**

This example demonstrates the simplest Conductor pattern. three tasks running in strict sequence. ExtractWorker takes a data source name and returns raw records. TransformWorker receives the raw data (wired via `${seq_extract_ref.output.rawData}`) and a format specification, then adds grade classifications (A/B/C) and normalized scores to each record. LoadWorker receives the transformed records (wired via `${seq_transform_ref.output.transformedData}`) and loads them into the destination, returning a count of loaded records. Conductor persists the output of each step, so if the load fails, you restart from the load step with the already-transformed data intact.

### What You Write: Workers

Three workers form the ETL sequence: ExtractWorker reads raw records from a data source, TransformWorker adds grade classifications and normalized scores, and LoadWorker writes the transformed records to the destination.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractWorker** | `seq_extract` | Extract phase of the ETL pipeline. Takes a data source name and returns hardcoded raw records. |
| **LoadWorker** | `seq_load` | Load phase of the ETL pipeline. Takes transformed data, prints each record, and returns load summary. |
| **TransformWorker** | `seq_transform` | Transform phase of the ETL pipeline. Takes raw data and a format, adds grade (A/B/C) and normalized score. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
seq_extract
 │
 ▼
seq_transform
 │
 ▼
seq_load

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
