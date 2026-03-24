# Data Warehouse Load in Java Using Conductor : Staging, Pre-Load Checks, Upsert, Post-Load Validation, and Metadata Update

## The Problem

Loading data into a warehouse is not just an INSERT statement. You need to stage records in a temporary table so the target table isn't locked during quality checks. You need to run pre-load validation (schema compliance, null checks, referential integrity) before touching production tables. You need to upsert. Inserting new records and updating existing ones based on a key. After loading, you need to verify the target table has the expected record count and the data is queryable. Finally, you need to update warehouse metadata (last load time, row counts, freshness) so downstream dashboards and dbt models know the data is current.

Without orchestration, you'd write a stored procedure or script that does staging, checking, loading, and metadata updates in a single transaction. If the upsert fails halfway, the staging table is left in an ambiguous state. If the metadata update fails after a successful load, downstream tools think the data is stale. There's no visibility into which step failed, how many records were staged vs: loaded, or whether post-load validation passed.

## The Solution

**You just write the staging, pre-load checks, upsert, post-load validation, and metadata update workers. Conductor handles strict stage-check-upsert-verify-update ordering, retries when the warehouse is temporarily unavailable, and precise record count tracking across every loading phase.**

Each stage of the warehouse load is a simple, independent worker. The stager writes records to a temporary staging table. The pre-load checker validates staged records against the target schema. The upserter performs INSERT ... ON CONFLICT UPDATE against the target table. The post-load validator confirms the target table has the expected record count. The metadata updater records the load timestamp, row count, and validation status. Conductor executes them in strict sequence, ensures the upsert only runs after pre-load checks pass, retries if the warehouse is temporarily unavailable, and tracks exactly how many records were staged, validated, upserted, and confirmed.

### What You Write: Workers

Five workers handle the warehouse loading pipeline: staging records to a temporary table, running pre-load quality checks, upserting validated records into the target, verifying post-load record counts, and updating warehouse metadata with load statistics.

| Worker | Task | What It Does |
|---|---|---|
| **PostLoadValidationWorker** | `wh_post_load_validation` | Validates records in the target table after loading. |
| **PreLoadChecksWorker** | `wh_pre_load_checks` | Runs pre-load quality checks on staged records. |
| **StageDataWorker** | `wh_stage_data` | Stages incoming records into a temporary staging table. |
| **UpdateMetadataWorker** | `wh_update_metadata` | Updates warehouse metadata after a successful load. |
| **UpsertTargetWorker** | `wh_upsert_target` | Upserts records from staging into the target table. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
wh_stage_data
 │
 ▼
wh_pre_load_checks
 │
 ▼
wh_upsert_target
 │
 ▼
wh_post_load_validation
 │
 ▼
wh_update_metadata

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
