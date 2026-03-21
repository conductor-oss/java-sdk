# Batch Processing in Java Using Conductor: Chunked Record Processing with DO_WHILE Loops

Your nightly ETL job processes 10 million rows from the transactions database. At row 8.7 million, the destination warehouse hiccups, a brief network partition, maybe 3 seconds. The job crashes. There's no checkpoint, so it restarts from row 1. Every night. Some nights it finishes by 6 AM; other nights the same transient failure at row 6M or 9M means the warehouse team arrives to stale data and a failed job notification. Nobody trusts the "last updated" timestamp anymore. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate batch processing with DO_WHILE loops that checkpoint progress, so a failure at batch 47 retries batch 47, not the entire job.

## The Problem

You have thousands (or millions) of records to process. Database rows to transform, files to convert, API calls to make, and you can't do them all at once. You need to split the work into manageable batches, process each batch sequentially, track progress across iterations, and produce a final summary. If batch 47 out of 200 fails, you need to retry just that batch, not start over from scratch.

Without orchestration, you'd write a `for` loop with manual batch slicing, wrap it in try/catch for retries, build your own progress tracking, and hope the process doesn't crash at batch 199 of 200. There's no visibility into which batch is running, no automatic recovery if the JVM dies mid-processing, and changing the batch size or adding a validation step means rewriting the loop logic.

## The Solution

**You just write the batch preparation, per-batch processing, and summarization workers. Conductor handles DO_WHILE iteration, per-batch retry on failure, and crash recovery that resumes from the exact batch where processing stopped.**

Each concern is a simple, independent worker. The batch preparation worker splits records into chunks based on the configured batch size. The processing worker handles a single batch. It receives a batch index, computes the record range, and processes those records. The summarizer aggregates results across all batches. Conductor's `DO_WHILE` loop drives the iteration, automatically advancing the batch index, retrying any batch that fails, and resuming from the exact batch where it left off if the process crashes. You get all of that, without writing a single line of loop management code.

### What You Write: Workers

Three workers divide batch processing into distinct responsibilities: splitting records into chunks, processing one chunk per loop iteration, and summarizing the results across all batches.

| Worker | Task | What It Does |
|---|---|---|
| `PrepareBatchesWorker` | `bp_prepare_batches` | Splits input records into chunks based on `batchSize`, computing total records (10) and total batches (4) |
| `ProcessBatchWorker` | `bp_process_batch` | Processes one batch per DO_WHILE iteration. Computes record range (e.g., records 1-3) and returns count |
| `SummarizeWorker` | `bp_summarize` | Aggregates results across all batches into a summary like "10 records in 4 batches" |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
bp_prepare_batches
 │
 ▼
DO_WHILE
 └── bp_process_batch
 │
 ▼
bp_summarize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
