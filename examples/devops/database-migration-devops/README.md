# Safe Database Migration with Pre-Backup and Schema Validation

Applying ALTER statements to a production database without a backup is a one-way door. If
the migration corrupts data or breaks the schema, you cannot roll back. This workflow takes
a snapshot first, applies the migration, validates the schema matches the expected state,
and only then deploys the application with updated mappings.

## Workflow

```
database, migrationVersion
           |
           v
+--------------+     +---------------+     +-----------------------+     +------------------+
| dbm_backup   | --> | dbm_migrate   | --> | dbm_validate_schema   | --> | dbm_update_app   |
+--------------+     +---------------+     +-----------------------+     +------------------+
  BACKUP-1363          3 ALTER              schema matches,                app deployed
  success=true         statements           no data loss                   completedAt set
```

## Workers

**BackupWorker** -- Takes `database` (default `"unknown-db"`) and `migrationVersion` (default
`"V000"`). Creates a snapshot and returns `backupId: "BACKUP-1363"` with `success: true`.

**MigrateWorker** -- Receives the backup output via `migrateData`. Applies 3 ALTER statements
and returns `migrate: true`, `alterStatements: 3`.

**ValidateSchemaWorker** -- Receives the migration output. Confirms the schema matches the
expected state with no data loss. Returns `validate_schema: true`.

**UpdateAppWorker** -- Deploys the application with the updated schema mapping. Returns
`update_app: true` and `completedAt: "2026-03-14T00:00:00Z"`.

## Tests

28 unit tests cover backup creation, migration execution, schema validation, and app
deployment.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
