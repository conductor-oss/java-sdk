# Implementing GDPR Compliance in Java with Conductor: Identity Verification, Data Location, Request Processing, and Completion Confirmation

A customer emailed asking you to delete all their data. Legal forwarded it to engineering, who found records in the CRM. The billing team deleted their payment history. Analytics still had their event stream but didn't hear about the request until week three. The marketing team's third-party enrichment vendor? Nobody even thought to check. Day 29: the customer filed a complaint with the Data Protection Authority. You have no audit trail proving what was deleted, what was missed, or when each system was actually processed, and now you're explaining to regulators why three of seven data stores were never touched. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate GDPR data subject requests end-to-end: verify identity, locate data across every system, process the request, and confirm completion, with a regulator-ready audit trail proving every step.

## The Problem

Under GDPR, data subjects can request access to their data, erasure ("right to be forgotten"), or portability. You have 30 days to comply. Each request requires verifying the requester's identity (prevent unauthorized data access), locating their data across all systems (databases, backups, SaaS tools, logs), processing the specific request type, and confirming completion back to the requester.

Without orchestration, GDPR requests are tracked in a spreadsheet. Someone manually searches each database, emails each SaaS vendor, and hopes they found everything before the 30-day deadline. If they miss a system, the organization faces regulatory fines. There's no audit trail proving the request was fully processed.

## The Solution

**You just write the data location queries and erasure operations. Conductor handles the mandated sequence from identity verification through completion, retries when data systems are unavailable, and a regulators-ready audit trail proving every step of the request was fulfilled.**

Each GDPR step is an independent worker: identity verification, data location, request processing, and completion confirmation. Conductor runs them in sequence: verify identity, locate all personal data, process the request, then confirm. Every request is tracked with full audit trail, when it was received, what data was found, what action was taken, and when it was completed, proving compliance to regulators. ### What You Write: Workers

Four workers handle GDPR requests end-to-end: VerifyIdentityWorker confirms the requester is who they claim, LocateDataWorker finds their personal data across all systems, ProcessRequestWorker executes the erasure or export, and ConfirmCompletionWorker notifies the subject within the 30-day deadline.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmCompletionWorker** | `gdpr_confirm_completion` | Sends a completion confirmation to the data subject within the 30-day deadline |
| **LocateDataWorker** | `gdpr_locate_data` | Locates the subject's personal data across all systems (e.g., CRM, billing, analytics, logs) |
| **ProcessRequestWorker** | `gdpr_process_request` | Executes the requested right (erasure, export, rectification) across all identified systems |
| **VerifyIdentityWorker** | `gdpr_verify_identity` | Verifies the identity of the data subject making the GDPR request |

### The Workflow

```
gdpr_verify_identity
 │
 ▼
gdpr_locate_data
 │
 ▼
gdpr_process_request
 │
 ▼
gdpr_confirm_completion

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
