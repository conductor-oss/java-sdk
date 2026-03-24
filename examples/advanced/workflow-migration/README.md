# Workflow Migration

A platform is migrating from workflow definition v1 to v2. In-flight v1 executions must complete, new executions must use v2, and the migration pipeline must validate that v2 produces equivalent results for a test suite of historical inputs.

## Pipeline

```
[wm_export_old]
     |
     v
[wm_transform]
     |
     v
[wm_import_new]
     |
     v
[wm_verify]
```

**Workflow inputs:** `sourceSystem`, `targetSystem`, `workflowName`

## Workers

**WmExportOldWorker** (task: `wm_export_old`)

- Sets `format` = `"legacy_json"`
- Writes `exported`, `definition`, `format`, `taskCount`

**WmImportNewWorker** (task: `wm_import_new`)

- Writes `imported`, `importedTaskCount`, `newWorkflowId`

**WmTransformWorker** (task: `wm_transform`)

- Writes `transformed`, `transformSuccess`

**WmVerifyWorker** (task: `wm_verify`)

- Reads `originalTasks`, `importedTasks`. Writes `verified`, `taskDelta`

---

**16 tests** | Workflow: `workflow_migration_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
