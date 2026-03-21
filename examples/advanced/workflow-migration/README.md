# Workflow Migration in Java Using Conductor : Export from Legacy, Transform, Import to New, Verify

## Migrating from Legacy Orchestration Without Losing History

You're moving from Airflow to Conductor (or Jenkins to Conductor, or a custom cron-based system). The legacy system has 200 workflow definitions, execution history that auditors need, and active runs that can't be interrupted. Migration means exporting the old definitions and history, transforming them into the target format (different task naming, different input/output schemas), importing them into the new system, and verifying that the migrated workflows produce identical results.

Doing this manually per workflow is tedious and error-prone. Automating it as a pipeline lets you batch-migrate workflows, catch transformation errors early, and verify each migration before decommissioning the old system.

## The Solution

**You write the export and transformation logic. Conductor handles the migration pipeline, retries, and verification tracking.**

`WmExportOldWorker` extracts workflow definitions and execution history from the source system. `WmTransformWorker` converts the exported data into the target system's format. mapping task names, translating input/output schemas, and adjusting configuration. `WmImportNewWorker` loads the transformed definitions into the new orchestration platform. `WmVerifyWorker` runs test executions on the migrated workflows and compares outputs against the legacy system's results. Conductor records the export, transformation, import, and verification for each migrated workflow.

### What You Write: Workers

Four workers handle the migration pipeline: legacy system export, schema transformation, new-platform import, and output verification, each targeting one phase of the system cutover.### The Workflow

```
wm_export_old
 │
 ▼
wm_transform
 │
 ▼
wm_import_new
 │
 ▼
wm_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
