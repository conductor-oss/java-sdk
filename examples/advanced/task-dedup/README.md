# Task Deduplication in Java Using Conductor : Hash Input, Check Cache, Execute or Return Cached

## Identical Inputs Should Produce Cached Results, Not Redundant Computation

A report generation request comes in with the same parameters as yesterday's run. same date range, same filters, same output format. Regenerating the report takes 15 minutes and produces identical output. If you could detect that the input hasn't changed, you'd return yesterday's result in milliseconds instead of burning compute.

Task deduplication means hashing the input to create a deterministic fingerprint, looking up that hash in a cache, and short-circuiting execution if a cached result exists. The `SWITCH` task makes the decision explicit. new inputs go to the execute path, seen inputs go to the cache-return path.

## The Solution

**You write the hashing and cache-check logic. Conductor handles the hit-or-miss routing, retries, and execution tracking.**

`HashInputWorker` computes a deterministic hash (SHA-256) of the task payload to create a fingerprint. `CheckSeenWorker` looks up the hash in the deduplication cache to determine if this exact input has been processed before. A `SWITCH` task routes based on the result: new inputs go to `ExecuteNewWorker` for full processing, while previously-seen inputs go to `ReturnCachedWorker` which returns the stored result immediately. Conductor records whether each execution was a cache hit or miss, and the hash used for deduplication.

### What You Write: Workers

Four workers implement the cache-or-compute pattern: input hashing, cache lookup, new-task execution, and cached-result retrieval, routing each request through a hit-or-miss decision.

| Worker | Task | What It Does |
|---|---|---|
| **CheckSeenWorker** | `tdd_check_seen` | Checks if a hash has been seen before to determine duplicate status. |
| **ExecuteNewWorker** | `tdd_execute_new` | Processes a new (non-duplicate) task and caches the result for future dedup. |
| **HashInputWorker** | `tdd_hash_input` | Hashes the input payload to produce a deterministic deduplication key. |
| **ReturnCachedWorker** | `tdd_return_cached` | Returns a previously cached result for a duplicate task.

### The Workflow

```
tdd_hash_input
 │
 ▼
tdd_check_seen
 │
 ▼
SWITCH (tdd_switch_ref)
 ├── new: tdd_execute_new
 ├── dup: tdd_return_cached
 └── default: tdd_execute_new

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
