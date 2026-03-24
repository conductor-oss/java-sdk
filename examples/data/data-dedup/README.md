# Data Dedup

A CRM system receives customer records from three different acquisition channels. The same person often appears with slight name variations, different email casing, or transposed phone digits. Before merging into the master list, duplicates need detection via fingerprinting, grouping, and resolution into a single canonical record per person.

## Pipeline

```
[dp_load_records]
     |
     v
[dp_compute_keys]
     |
     v
[dp_find_duplicates]
     |
     v
[dp_merge_groups]
     |
     v
[dp_emit_deduped]
```

**Workflow inputs:** `records`, `matchFields`

## Workers

**ComputeKeysWorker** (task: `dp_compute_keys`)

Computes dedup keys from specified match fields. Each record gets a dedupKey built from lowercased/trimmed values of matchFields joined by "|".

- Lowercases strings, trims whitespace, uses java streams
- Reads `records`, `matchFields`. Writes `keyedRecords`

**EmitDedupedWorker** (task: `dp_emit_deduped`)

Emits the final deduplicated result with a summary.

- Reads `deduped`, `originalCount`, `dupCount`. Writes `dedupedCount`, `summary`

**FindDuplicatesWorker** (task: `dp_find_duplicates`)

Groups keyed records by dedupKey and identifies duplicate groups.

- Reads `keyedRecords`. Writes `groups`, `groupCount`, `duplicateCount`

**LoadRecordsWorker** (task: `dp_load_records`)

Loads records for deduplication and passes them through with a count.

- Reads `records`. Writes `records`, `count`

**MergeGroupsWorker** (task: `dp_merge_groups`)

Merges duplicate groups by picking the first record from each group and removing dedupKey.

- Reads `groups`. Writes `mergedRecords`

---

**42 tests** | Workflow: `data_dedup` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
