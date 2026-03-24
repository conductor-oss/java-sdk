# Data Deduplication in Java Using Conductor : Key Computation, Duplicate Detection, and Record Merging

## The Problem

Your dataset has duplicate records, the same customer entered twice with slightly different casing, the same order submitted by two systems, the same product listed with trailing whitespace in the name. You need to find and eliminate these duplicates before the data goes into your analytics pipeline, CRM, or data warehouse. That means computing dedup keys from configurable match fields (lowercased, trimmed for consistency), grouping records that share the same key, deciding which record to keep when duplicates are found (first seen, most complete, most recent), and emitting a clean dataset with a summary of how many duplicates were removed.

Without orchestration, you'd write a single method that loads records, builds a `HashMap` for grouping, picks winners inline, and outputs results. If the key computation logic changes (adding fuzzy matching or phonetic encoding), you'd rewrite the entire pipeline. If the process crashes after finding duplicates but before merging, you'd lose that work. There's no audit trail showing how many duplicates were found, which groups were merged, or what the dedup ratio was.

## The Solution

**You just write the record loading, key computation, duplicate detection, group merging, and emission workers. Conductor handles the load-key-detect-merge-emit pipeline, per-step retries, and full tracking of how many duplicates were found and removed at each stage.**

Each stage of the deduplication pipeline is a simple, independent worker. The loader reads records from the data source. The key computer generates a dedup key for each record by normalizing and concatenating the configured match fields. The duplicate finder groups records by key and identifies groups with more than one member. The merger selects a canonical record from each duplicate group. The emitter outputs the deduplicated dataset with a summary of original count, duplicates found, and final count. Conductor executes them in sequence, passes keyed records between steps, retries if a step fails, and tracks exactly how many duplicates were found and merged.

### What You Write: Workers

Five workers form the deduplication pipeline: loading records, computing normalized dedup keys from configurable match fields, detecting duplicate groups, merging groups into canonical records, and emitting the clean dataset.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeKeysWorker** | `dp_compute_keys` | Computes keys |
| **EmitDedupedWorker** | `dp_emit_deduped` | Emits the final deduplicated result with a summary. |
| **FindDuplicatesWorker** | `dp_find_duplicates` | Groups keyed records by dedupKey and identifies duplicate groups. |
| **LoadRecordsWorker** | `dp_load_records` | Loads records for deduplication and passes them through with a count. |
| **MergeGroupsWorker** | `dp_merge_groups` | Merges duplicate groups by picking the first record from each group and removing dedupKey. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
dp_load_records
 │
 ▼
dp_compute_keys
 │
 ▼
dp_find_duplicates
 │
 ▼
dp_merge_groups
 │
 ▼
dp_emit_deduped

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
