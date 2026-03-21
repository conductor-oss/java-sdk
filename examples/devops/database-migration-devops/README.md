# Database Migration in Java with Conductor : Backup, Migrate Schema, Validate, Update Application

Automates database schema migrations using [Conductor](https://github.com/conductor-oss/conductor). This workflow creates a pre-migration backup, applies ALTER statements and schema changes, validates the new schema matches the expected state, and deploys the application with updated schema mappings.

## Schema Changes Without the Fear

You need to add a column to the orders table in production. A wrong ALTER statement could lock the table, corrupt data, or leave the application pointing at a schema that no longer matches its code. The safe path: snapshot the database first, apply the migration, validate that every expected table and column exists, then deploy the app version that knows about the new schema. If validation fails, you have the backup to restore from.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the migration and validation logic. Conductor handles backup-before-migrate sequencing, schema verification gates, and migration audit trails.**

`BackupWorker` creates a point-in-time backup of the database before any schema changes. enabling rollback if the migration fails. `MigrateWorker` applies the schema migration. ALTER TABLE, CREATE INDEX, data transformation, using the appropriate migration tool and strategy. `ValidateSchemaWorker` verifies the migration succeeded, checking table structures, column types, constraint integrity, and data consistency. `UpdateAppWorker` deploys or configures the application to use the new schema, updating connection strings, enabling new features that depend on the schema changes. Conductor sequences these four steps and records the migration outcome for audit.

### What You Write: Workers

Four workers execute the migration safely. Creating a pre-migration backup, applying schema changes, validating the result, and deploying the updated application.

| Worker | Task | What It Does |
|---|---|---|
| **BackupWorker** | `dbm_backup` | Creates a database snapshot before migration. |
| **MigrateWorker** | `dbm_migrate` | Applies database migration ALTER statements. |
| **UpdateAppWorker** | `dbm_update_app` | Deploys the application with updated schema mapping. |
| **ValidateSchemaWorker** | `dbm_validate_schema` | Validates that schema matches expected state after migration. |

the workflow and rollback logic stay the same.

### The Workflow

```
dbm_backup
 │
 ▼
dbm_migrate
 │
 ▼
dbm_validate_schema
 │
 ▼
dbm_update_app

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
