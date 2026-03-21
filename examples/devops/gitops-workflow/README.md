# GitOps Workflow in Java with Conductor : Detect Drift, Plan Sync, Apply, Verify State

Automates GitOps reconciliation using [Conductor](https://github.com/conductor-oss/conductor). This workflow detects configuration drift between a Git repository and a target Kubernetes cluster, plans the synchronization actions needed, applies the changes to bring the cluster into the desired state, and verifies the cluster matches Git after sync.

## Git Is the Source of Truth

Someone ran `kubectl edit` on the production deployment and changed the replica count. Now the cluster does not match what is in Git. GitOps reconciliation detects this drift, plans the sync to restore the desired state, applies the changes, and verifies the cluster is back in alignment. Without this, manual changes accumulate silently until the next Git-based deploy overwrites them, or worse, they persist and nobody knows the cluster differs from what the repo says.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the drift detection and sync logic. Conductor handles the detect-plan-apply-verify reconciliation loop and records every sync operation.**

`DetectDriftWorker` compares the desired state in the Git repository against the live cluster state, identifying resources that have drifted (modified, missing, or extra). `PlanSyncWorker` generates a sync plan listing the specific operations needed. create, update, or delete, with a preview of each change. `ApplySyncWorker` executes the sync plan, applying changes to bring the cluster back to the desired state. `VerifyStateWorker` confirms every resource in the cluster matches the Git-defined specification after sync. Conductor records every drift detection and sync operation for GitOps audit trails.

### What You Write: Workers

Four workers handle GitOps reconciliation. Detecting drift between Git and the cluster, planning the sync, applying changes, and verifying state alignment.

| Worker | Task | What It Does |
|---|---|---|
| **ApplySyncWorker** | `go_apply_sync` | Applies the synchronization plan to bring cluster into desired state. |
| **DetectDriftWorker** | `go_detect_drift` | Detects configuration drift between Git repository and target cluster. |
| **PlanSyncWorker** | `go_plan_sync` | Plans the synchronization actions needed to reconcile drift. |
| **VerifyStateWorker** | `go_verify_state` | Verifies that the cluster state matches the Git repository after sync. |

the workflow and rollback logic stay the same.

### The Workflow

```
go_detect_drift
 │
 ▼
go_plan_sync
 │
 ▼
go_apply_sync
 │
 ▼
go_verify_state

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
