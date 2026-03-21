# Parallel Chunk Processing in Java Using Conductor : Split, Process in Parallel, Merge

## Processing Large Datasets Sequentially Wastes Time

A 10 GB CSV file needs to be processed. parsing rows, applying transformations, computing aggregates. Doing it sequentially takes an hour. Splitting it into three chunks and processing them in parallel takes 20 minutes, but you need to manage the chunking logic, wait for all three to finish, handle a chunk that fails while the others succeed, and merge the partial results into a coherent whole.

Building parallel processing with raw threads means managing a thread pool, implementing a barrier to wait for all chunks, retrying failed chunks without redoing successful ones, and merging heterogeneous partial results. Each concern is simple alone, but combining them reliably is where the complexity lives.

## The Solution

**You write the chunking and per-partition logic. Conductor handles parallel execution, per-chunk retries, and result merging.**

`PprSplitWorkWorker` divides the dataset into chunks based on the configured chunk size. A `FORK_JOIN` processes all three chunks in parallel. `PprChunk1Worker`, `PprChunk2Worker`, and `PprChunk3Worker` each handle their assigned partition independently. The `JOIN` waits for all three to complete. `PprMergeWorker` combines the per-chunk outputs into a single merged result. Conductor handles the parallel fan-out, retries any failed chunk independently, and tracks per-chunk timing so you can identify slow partitions.

### What You Write: Workers

Five workers implement the split-process-merge pattern: data chunking, three parallel chunk processors, and a result merger, each handling one partition independently.

| Worker | Task | What It Does |
|---|---|---|
| **PprChunk1Worker** | `ppr_chunk_1` | Processes the first data chunk in parallel (e.g.### The Workflow

```
ppr_split_work
 │
 ▼
FORK_JOIN
 ├── ppr_chunk_1
 ├── ppr_chunk_2
 └── ppr_chunk_3
 │
 ▼
JOIN (wait for all branches)
ppr_merge

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
