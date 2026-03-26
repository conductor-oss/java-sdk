# Parallel Processing

A report generator needs to fetch data from three independent sources (sales, inventory, customers), each taking 5-10 seconds. Running them sequentially takes 15-30 seconds. The parallel pipeline runs all three fetches concurrently and merges the results when all complete, cutting wall-clock time to the slowest source.

## Pipeline

```
[ppr_split_work]
     |
     v
     +───────────────────────────────────────────────+
     | [ppr_chunk_1] | [ppr_chunk_2] | [ppr_chunk_3] |
     +───────────────────────────────────────────────+
     [join]
     |
     v
[ppr_merge]
```

**Workflow inputs:** `dataset`, `chunkSize`

## Workers

**PprChunk1Worker** (task: `ppr_chunk_1`)

- Writes `result`, `processed`

**PprChunk2Worker** (task: `ppr_chunk_2`)

- Writes `result`, `processed`

**PprChunk3Worker** (task: `ppr_chunk_3`)

- Writes `result`, `processed`

**PprMergeWorker** (task: `ppr_merge`)

- Writes `mergedResult`, `totalProcessed`

**PprSplitWorkWorker** (task: `ppr_split_work`)

- Writes `chunks`, `totalChunks`

---

**20 tests** | Workflow: `ppr_parallel_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
