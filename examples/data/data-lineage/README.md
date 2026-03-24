# Data Lineage

An analytics team discovers that a KPI dashboard is showing wrong numbers, but nobody can tell which upstream transformation introduced the error. They need automatic lineage tracking that records every transformation step, its inputs and outputs, and a dependency graph so they can trace any output field back to its source.

## Pipeline

```
[ln_register_source]
     |
     v
[ln_apply_transform_1]
     |
     v
[ln_apply_transform_2]
     |
     v
[ln_record_destination]
     |
     v
[ln_build_lineage_graph]
```

**Workflow inputs:** `records`, `sourceName`, `destName`

## Workers

**ApplyTransform1Worker** (task: `ln_apply_transform_1`)

Applies transform 1: uppercase names and tracks lineage.

- Uppercases strings, captures `instant.now()` timestamps
- Reads `records`, `lineage`. Writes `records`, `lineage`

**ApplyTransform2Worker** (task: `ln_apply_transform_2`)

Applies transform 2: lowercase emails and tracks lineage.

- Lowercases strings, captures `instant.now()` timestamps
- Reads `records`, `lineage`. Writes `records`, `lineage`

**BuildLineageGraphWorker** (task: `ln_build_lineage_graph`)

Builds a lineage graph summary from collected lineage entries.

- Uses java streams
- Reads `lineage`, `recordCount`. Writes `transformSteps`, `depth`, `summary`, `lineageChain`

**RecordDestinationWorker** (task: `ln_record_destination`)

Records the destination in the lineage chain.

- Captures `instant.now()` timestamps
- Reads `records`, `lineage`. Writes `records`, `lineage`

**RegisterSourceWorker** (task: `ln_register_source`)

Registers the data source and initializes lineage tracking.

- Captures `instant.now()` timestamps
- Reads `records`. Writes `records`, `lineage`, `recordCount`

---

**30 tests** | Workflow: `data_lineage` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
