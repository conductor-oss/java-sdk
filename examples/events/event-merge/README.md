# Event Merge

Two upstream systems emit partial events about the same entity. System A sends pricing data; System B sends inventory data. The merge pipeline needs to match events by entity ID, combine their fields into a single enriched event, and handle the case where one side arrives before the other.

## Pipeline

```
     +───────────────────────────────────────────────────────────────────────+
     | [mg_collect_stream_a] | [mg_collect_stream_b] | [mg_collect_stream_c] |
     +───────────────────────────────────────────────────────────────────────+
     [join]
     |
     v
[mg_merge_streams]
     |
     v
[mg_process_merged]
```

**Workflow inputs:** `sourceA`, `sourceB`, `sourceC`

## Workers

**CollectStreamAWorker** (task: `mg_collect_stream_a`)

Collects events from stream A (API source).

- Reads `source`. Writes `events`, `count`

**CollectStreamBWorker** (task: `mg_collect_stream_b`)

Collects events from stream B (mobile source).

- Reads `source`. Writes `events`, `count`

**CollectStreamCWorker** (task: `mg_collect_stream_c`)

Collects events from stream C (IoT source).

- Reads `source`. Writes `events`, `count`

**MergeStreamsWorker** (task: `mg_merge_streams`)

Merges events from three streams into a single list.

- Reads `streamA`, `streamB`, `streamC`. Writes `merged`, `totalCount`

**ProcessMergedWorker** (task: `mg_process_merged`)

Processes the merged event list.

- Sets `status` = `"all_processed"`
- Reads `mergedEvents`, `totalCount`. Writes `status`, `count`

---

**40 tests** | Workflow: `event_merge_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
