# Permission Sync in Java Using Conductor

## The Problem

Your identity provider is the source of truth for user permissions, but roles and access levels have drifted out of sync with downstream systems. The operations team needs to scan the source system and all target systems to capture current permission states, diff the permissions to identify discrepancies (missing roles, extra grants), apply the corrections to bring targets into alignment, and verify that all systems now match the source of truth. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the permission-scanning, diffing, correction, and verification workers. Conductor handles the sync pipeline and discrepancy data flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

ScanSystemsWorker captures permission states from source and targets, DiffWorker identifies missing grants per role, SyncWorker applies corrections, and VerifyWorker confirms all systems are now aligned.

| Worker | Task | What It Does |
|---|---|---|
| **DiffWorker** | `pms_diff` | Compares source and target permission sets, identifying missing grants per role (admin, editor, viewer) |
| **ScanSystemsWorker** | `pms_scan_systems` | Scans permissions from the source system and all target systems, capturing role counts for each |
| **SyncWorker** | `pms_sync` | Applies the detected permission changes to target systems, tracking success and failure counts |
| **VerifyWorker** | `pms_verify` | Re-checks all systems to confirm permissions are now in sync, recording the verification timestamp |

Replace with real identity provider and database calls and ### The Workflow

```
pms_scan_systems
 │
 ▼
pms_diff
 │
 ▼
pms_sync
 │
 ▼
pms_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
