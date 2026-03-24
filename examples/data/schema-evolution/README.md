# Schema Evolution in Java Using Conductor : Change Detection, Transform Generation, Data Migration, and Validation

## The Problem

Your database schema needs to evolve, a new `phone_number` column is being added, the `address` field is being split into `street`, `city`, and `zip`, and the `status` column is changing from a string to an integer enum. You need to detect exactly what changed between the current and target schemas, generate the right migration transforms (ALTER TABLE for additions, data conversion for type changes, column splitting for restructured fields), apply those transforms to the existing data, and validate that every record in the migrated table conforms to the target schema. If you apply the transforms but skip validation, you might discover weeks later that 2% of records have null values in the new required `zip` field because the address splitting logic didn't handle PO boxes.

Without orchestration, you'd write a single migration script that diffs schemas, generates SQL, runs it, and hopes for the best. If the data transformation fails on row 50,000 of 100,000, the table is left in an inconsistent state with no record of which transforms succeeded. There's no visibility into how many changes were detected, what transform operations were generated, or whether the compatibility level is backward-compatible or breaking. Rolling back means restoring from a backup and hoping the backup is recent.

## The Solution

**You just write the change detection, transform generation, data migration, and schema validation workers. Conductor handles strict ordering so transforms apply only after a valid migration plan exists, retries when databases are temporarily unavailable, and tracking of change counts, transform counts, and compatibility levels at every stage.**

Each stage of the schema evolution pipeline is a simple, independent worker. The change detector compares the current and target schemas field by field, identifying additions, removals, type changes, and renames, and classifying each change's compatibility impact. The transform generator converts those detected changes into concrete transform operations. ADD_COLUMN for new fields, TYPE_CAST for type changes, SPLIT for field restructuring. Producing a migration plan. The transform applier executes the migration plan against the sample data, applying each transform in sequence and tracking how many records were successfully transformed. The schema validator checks every transformed record against the target schema, confirming all required fields are present, types match, and constraints are satisfied, and reports the overall compatibility level (backward, forward, full, or breaking). Conductor executes them in strict sequence, ensures transforms only apply after change detection generates a valid plan, retries if the database is temporarily unavailable, and tracks change counts, transform counts, and validation results at every stage.

### What You Write: Workers

Four workers manage schema evolution: comparing current and target schemas to detect changes, generating concrete transform operations for each change, applying those transforms to existing data, and validating that transformed records conform to the target schema.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyTransformWorker** | `sh_apply_transform` | Applies schema transforms to sample data. |
| **DetectChangesWorker** | `sh_detect_changes` | Detects schema changes between current and target schemas. |
| **GenerateTransformWorker** | `sh_generate_transform` | Generates transform operations from detected schema changes. |
| **ValidateSchemaWorker** | `sh_validate_schema` | Validates transformed data against the target schema. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
sh_detect_changes
 │
 ▼
sh_generate_transform
 │
 ▼
sh_apply_transform
 │
 ▼
sh_validate_schema

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
