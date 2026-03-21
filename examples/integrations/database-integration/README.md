# Database Integration in Java Using Conductor

## Migrating Data Between Databases Reliably

Database migration involves a strict sequence: connect to both databases, query the source, transform the data to match the target schema, write the transformed rows, and verify that source and target counts match. Each step depends on the previous one. you cannot transform without data, and you cannot verify without knowing how many rows were written. If any step fails mid-migration, you need to know exactly where it stopped and be able to resume.

Without orchestration, you would manage connection lifecycles, chain SQL operations manually, and build custom verification logic. Conductor sequences the five steps and tracks every row count and connection ID between them.

## The Solution

**You just write the ETL workers. Database connection, source query, data transformation, target write, and row count verification. Conductor handles connection lifecycle management, query retries on timeouts, and row-count tracking for migration verification.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Five workers run the ETL migration: ConnectWorker establishes database connections, QueryWorker extracts source rows, TransformWorker normalizes and maps fields, WriteWorker inserts into the target, and VerifyWorker confirms row counts match.

| Worker | Task | What It Does |
|---|---|---|
| **ConnectWorker** | `dbi_connect` | Establishes connections to source and target databases. returns sourceConnectionId and targetConnectionId for use by subsequent query and write steps |
| **QueryWorker** | `dbi_query` | Queries data from the source database. executes the SQL query using the source connection and returns the result rows and rowCount |
| **TransformWorker** | `dbi_transform` | Transforms the queried rows. applies the transform rules (field normalization, timestamp addition, schema mapping) and returns the transformedRows and transformedCount |
| **WriteWorker** | `dbi_write` | Writes transformed rows to the target database. inserts the rows using the target connection and returns the writtenCount |
| **VerifyWorker** | `dbi_verify` | Verifies the migration. compares the source rowCount against the writtenCount and returns verified=true if they match |

the workflow orchestration and error handling stay the same.

### The Workflow

```
dbi_connect
 │
 ▼
dbi_query
 │
 ▼
dbi_transform
 │
 ▼
dbi_write
 │
 ▼
dbi_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
