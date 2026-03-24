# Batch Processing

An operations team needs to process a dataset of variable size, but the processing service chokes on payloads larger than a few hundred records. They need to split the input into manageable chunks, process each chunk independently, and produce a unified summary -- without losing track of which chunk failed if one does.

## Pipeline

```
[bp_prepare_batches]
     |
     v
     +── loop ──────────────+
     |  [bp_process_batch]
     +───────────────────────+
     |
     v
[bp_summarize]
```

**Workflow inputs:** `records`, `batchSize`

## Workers

**PrepareBatchesWorker** (task: `bp_prepare_batches`)

Prepares batches from input records by splitting them into chunks.

- Applies `math.ceil()`, clamps with `math.min()`
- Reads `records`, `batchSize`. Writes `totalRecords`, `totalBatches`, `batchSize`, `batches`

**ProcessBatchWorker** (task: `bp_process_batch`)

Processes a single batch of records: validates fields, normalizes strings, and computes per-record status. This does real per-item transformation work.

- Trims whitespace, clamps with `math.min()`, filters with predicates
- Reads `iteration`, `batchSize`, `totalRecords`, `batches`. Writes `batchIndex`, `processedCount`, `rangeStart`, `rangeEnd`, `processedItems`

**SummarizeWorker** (task: `bp_summarize`)

Summarizes batch processing results.

- Reads `totalRecords`, `iterations`. Writes `summary`

---

**30 tests** | Workflow: `batch_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
