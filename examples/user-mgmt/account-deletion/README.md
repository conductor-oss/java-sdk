# Account Deletion in Java Using Conductor

## The Problem

You need to permanently delete a user's account. Verifying their identity and deletion reason, backing up their data for regulatory retention, purging records across all database tables and connected systems, and sending a final confirmation that the account is gone. Each step depends on the previous one's output.

If the backup fails but deletion proceeds anyway, the user's data is gone with no recovery path. If deletion clears the primary database but misses a downstream system, personal data lingers in places it shouldn't, creating GDPR and CCPA liability. Without orchestration, you'd build a monolithic deletion handler that mixes identity verification, S3 backup uploads, multi-table cascading deletes, and email notifications, making it impossible to add a cooling-off period, extend the backup retention policy, or audit which tables were cleared for which deletion request.

## The Solution

**You just write the identity-verification, data-backup, account-purge, and confirmation workers. Conductor handles the deletion sequence and compliance data flow.**

VerifyDeletionWorker confirms the user's identity and validates the deletion reason (user request, policy violation, inactivity) before anything irreversible happens. BackupWorker exports the user's data: profile, activity history, stored files, to durable storage and returns a backup ID for retention compliance. DeleteAccountWorker purges the user's records across all database tables (reporting 12 tables cleared) and marks the account as deleted with a timestamp. ConfirmDeletionWorker sends the user a final notification confirming their account has been removed and providing the backup reference if they ever need a data retrieval within the retention window. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

VerifyDeletionWorker confirms identity and reason, BackupWorker exports data for retention compliance, DeleteAccountWorker purges records across 12 tables, and ConfirmDeletionWorker sends the final GDPR-compliant notification.

| Worker | Task | What It Does |
|---|---|---|
| **BackupWorker** | `acd_backup` | Exports the user's data to durable storage, returning a backup ID with a 30-day retention period |
| **ConfirmDeletionWorker** | `acd_confirm` | Sends a GDPR-compliant deletion confirmation notification to the user |
| **DeleteAccountWorker** | `acd_delete` | Purges the user's records across all database tables (12 tables) and timestamps the deletion |
| **VerifyDeletionWorker** | `acd_verify` | Verifies the user's identity and validates the deletion reason before proceeding |

Replace with real identity provider and database calls and ### The Workflow

```
acd_verify
 │
 ▼
acd_backup
 │
 ▼
acd_delete
 │
 ▼
acd_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
