# User Migration in Java Using Conductor : ETL Pipeline for Cross-Database User Transfer

## The Problem

You need to migrate user data from one database to another. a legacy system to a new platform, a monolith to microservices, or an old schema to a new one. The migration is a classic ETL pipeline: extract users in batches from the source, transform each record to the new schema (adding fields, renaming columns, converting formats), load the transformed records into the target database, verify that every extracted record made it through, and notify the team whether the migration succeeded or had mismatches.

Without orchestration, you'd write a long procedural script that couples extraction, transformation, and loading into one fragile process. If the load step fails halfway through a batch, you have no idea which records made it. If verification detects a mismatch, there's no audit trail to diagnose where records were lost. Retrying means re-running the entire pipeline from scratch, and there's no visibility into how long each phase took or where bottlenecks lie.

## The Solution

**You just write the extract, transform, load, verify, and notify workers. Conductor handles the ETL pipeline sequencing and batch data flow.**

Each ETL phase. extract, transform, load, verify, notify, is a simple, independent worker. Conductor runs them in sequence, threads the extracted user list into the transform step, passes the transformed data to the loader, feeds the loaded/expected counts into verification, and delivers the final result to the notification step. If any step fails (a database timeout, a schema error), Conductor retries automatically and resumes from exactly where it left off. ### What You Write: Workers

ExtractWorker reads users from the source database, TransformWorker converts schemas, LoadWorker batch-inserts into the target, VerifyMigrationWorker confirms record counts, and NotifyMigrationWorker reports the result.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractWorker** | `umg_extract` | Reads user records in batches from the source database and returns the extracted list with a count |
| **TransformWorker** | `umg_transform` | Converts user records from the old schema to the new one, adding fields like avatar and timezone |
| **LoadWorker** | `umg_load` | Writes the transformed user records into the target database and reports loaded/failed counts |
| **VerifyMigrationWorker** | `umg_verify` | Compares the loaded count against the expected count to confirm all records migrated successfully |
| **NotifyMigrationWorker** | `umg_notify` | Sends a migration status notification to stakeholders with the verification result |

Replace with real identity provider and database calls and ### The Workflow

```
umg_extract
 │
 ▼
umg_transform
 │
 ▼
umg_load
 │
 ▼
umg_verify
 │
 ▼
umg_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
