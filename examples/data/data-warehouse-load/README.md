# Data Warehouse Load

A nightly ETL job needs to load cleansed records into a data warehouse. The load is not a simple INSERT -- it requires pre-load validation, staging to a temporary table, merge-on-conflict semantics, post-load row count verification, and a rollback path if the counts don't match.

## Pipeline

```
[wh_stage_data]
     |
     v
[wh_pre_load_checks]
     |
     v
[wh_upsert_target]
     |
     v
[wh_post_load_validation]
     |
     v
[wh_update_metadata]
```

**Workflow inputs:** `records`, `targetTable`, `schema`

## Workers

**PostLoadValidationWorker** (task: `wh_post_load_validation`)

Validates records in the target table after loading.

- Reads `expectedCount`. Writes `passed`, `rowCountMatch`, `expectedCount`

**PreLoadChecksWorker** (task: `wh_pre_load_checks`)

Runs pre-load quality checks on staged records.

- Reads `recordCount`. Writes `passed`, `validCount`, `checks`

**StageDataWorker** (task: `wh_stage_data`)

Stages incoming records into a temporary staging table.

- Records wall-clock milliseconds
- Reads `records`. Writes `stagingTable`, `stagedCount`

**UpdateMetadataWorker** (task: `wh_update_metadata`)

Updates warehouse metadata after a successful load.

- Captures `instant.now()` timestamps
- Reads `recordsLoaded`, `validationPassed`. Writes `summary`, `lastLoadTime`

**UpsertTargetWorker** (task: `wh_upsert_target`)

Upserts records from staging into the target table.

- Applies `math.floor()`
- Reads `recordCount`. Writes `upsertedCount`, `inserted`, `updated`, `targetTable`

---

**30 tests** | Workflow: `data_warehouse_load` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
