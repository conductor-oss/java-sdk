# Change Tracking in Java Using Conductor : Detect, Diff, Classify, and Record Infrastructure Changes

## The Problem

You need to know when infrastructure changes. a security group rule was modified, an instance type was changed, a deployment rolled out a new version. For each change, you need to compute what exactly changed (diff the before/after state), classify whether it was a configuration change, scaling event, or deployment, and record everything for compliance audits and rollback capability.

Without orchestration, change tracking is either absent (you discover changes after incidents) or incomplete (you detect changes but don't record the diff). Classification is manual, diffs are computed inconsistently, and there's no centralized audit trail connecting change detection to change details.

## The Solution

**You just write the change detection and diff computation logic. Conductor handles the detect-diff-classify-record pipeline, retries when version control APIs are temporarily down, and a centralized audit trail connecting every change to its diff and risk classification.**

Each tracking concern is an independent worker. change detection, diff computation, classification, and recording. Conductor runs them in sequence: detect a change, compute the diff, classify it, then record it. Every change event is tracked with full context, you can see exactly what changed, when, and how it was classified. ### What You Write: Workers

Four workers track infrastructure changes: DetectChangeWorker spots version differences, DiffWorker computes lines added and removed, ClassifyChangeWorker rates the risk level, and RecordChangeWorker writes the classified change to the audit trail for rollback capability.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyChangeWorker** | `chg_classify` | Classifies changes as minor/moderate/major with a risk level based on files changed and lines modified |
| **DetectChangeWorker** | `chg_detect_change` | Detects a change in a resource by comparing current vs, previous version identifiers |
| **DiffWorker** | `chg_diff` | Computes the diff between resource versions, returning lines added/removed, files changed, and a summary |
| **RecordChangeWorker** | `chg_record` | Records the classified change to the audit trail with a unique change ID for tracking and rollback |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
chg_detect_change
 │
 ▼
chg_diff
 │
 ▼
chg_classify
 │
 ▼
chg_record

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
