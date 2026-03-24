# Schema Evolution

A data platform team is migrating from schema v1 to v2. Records in the old schema need detection, field-level migration (renamed columns, split fields, new required defaults), validation against the new schema, and a compatibility report showing what percentage of records migrated cleanly.

## Pipeline

```
[sh_detect_changes]
     |
     v
[sh_generate_transform]
     |
     v
[sh_apply_transform]
     |
     v
[sh_validate_schema]
```

**Workflow inputs:** `currentSchema`, `targetSchema`, `sampleData`

## Workers

**ApplyTransformWorker** (task: `sh_apply_transform`)

Applies schema transforms to sample data.

- Parses strings to `int`
- Sets `status` = `"active"`
- Reads `sampleData`. Writes `transformedData`, `recordCount`

**DetectChangesWorker** (task: `sh_detect_changes`)

Detects schema changes between current and target schemas.

- Formats output strings
- Sets `type` = `"ADD_FIELD"`, `type` = `"RENAME_FIELD"`, `type` = `"CHANGE_TYPE"`, `type` = `"DROP_FIELD"`
- Writes `changes`, `changeCount`, `changeTypes`

**GenerateTransformWorker** (task: `sh_generate_transform`)

Generates transform operations from detected schema changes.

- Sets `action` = `"addColumn"`, `action` = `"renameColumn"`, `action` = `"castType"`, `action` = `"dropColumn"`
- Reads `changes`. Writes `transforms`, `transformCount`

**ValidateSchemaWorker** (task: `sh_validate_schema`)

Validates transformed data against the target schema.

- Reads `transformedData`. Writes `passed`, `compatibilityLevel`, `validatedRecords`

---

**15 tests** | Workflow: `schema_evolution` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
