# GDPR Data Deletion in Java Using Conductor : Record Discovery, Identity Verification, Cross-System Erasure, and Audit Logging

## The Problem

When a user submits a GDPR erasure request, their data lives in five or more systems: profile and credentials in the user accounts database, clickstream and session events in analytics, invoices and payment info in billing, ticket messages and attachments in support, and email preferences in marketing. You need to find every record tied to that user across all of these systems, verify the requester's identity before touching anything (a deletion request from an unverified source must be rejected), delete from every system, and produce an audit trail that proves to regulators exactly what was found, when it was deleted, and from which systems. You have 30 days to comply.

Without orchestration, you'd write a single deletion script that queries each system, deletes inline, and hopes nothing fails halfway. If the billing system is temporarily down after you've already wiped analytics, the user's data is partially deleted with no record of what remains. There's no identity verification gate, the script just deletes. There's no audit log that a regulator would accept, because there's no durable record of what was found before deletion. Adding a new system (say, a recommendation engine that stores user embeddings) means modifying tightly coupled code with no visibility into which system's deletion succeeded or failed.

## The Solution

**You just write the record discovery, identity verification, cross-system deletion, and audit log workers. Conductor handles strict ordering so deletion never runs before identity verification, retries when downstream systems are temporarily unavailable, and a durable audit trail for regulatory compliance.**

Each stage of the erasure pipeline is a simple, independent worker. The record finder scans user accounts, analytics, billing, support, and marketing systems to build a complete inventory of every record tied to the userId. The identity verifier checks the verification token before any deletion can proceed. If verification fails, the workflow aborts without deleting anything. The data deleter iterates through every discovered record and erases it, tracking deletion status per record and per system. The audit log generator creates a compliance-ready record containing the request ID, the user ID, every record that was deleted, and the exact timestamp of deletion. Conductor executes them in strict sequence, ensures deletion only runs after identity verification passes, retries if a system is temporarily unavailable, and provides a complete audit trail of every step. ### What You Write: Workers

Four workers implement the GDPR erasure pipeline: discovering all records across five systems tied to a user, verifying the requester's identity before any deletion, erasing data from every system, and generating a compliance-ready audit log.

| Worker | Task | What It Does |
|---|---|---|
| **DeleteDataWorker** | `gr_delete_data` | Deletes user data from all systems if identity is verified. |
| **FindRecordsWorker** | `gr_find_records` | Finds all records associated with a user across multiple systems. |
| **GenerateAuditLogWorker** | `gr_generate_audit_log` | Generates a GDPR-compliant audit log for the deletion request. |
| **VerifyIdentityWorker** | `gr_verify_identity` | Verifies the identity of the user requesting data deletion. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
gr_find_records
 │
 ▼
gr_verify_identity
 │
 ▼
gr_delete_data
 │
 ▼
gr_generate_audit_log

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
