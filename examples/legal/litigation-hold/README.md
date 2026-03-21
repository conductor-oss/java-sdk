# Litigation Hold in Java with Conductor

## The Problem

A potential lawsuit triggers a legal hold. You need to identify custodians who may possess relevant evidence, notify them of their preservation obligations, collect and preserve electronic data from email, Slack, and cloud storage, and track acknowledgments until the hold is released. Missing a custodian or losing data can result in sanctions and adverse inferences at trial.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the hold initiation, custodian notification, data preservation, and compliance verification logic. Conductor handles notification retries, acknowledgment tracking, and hold compliance audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Custodian identification, hold notification, acknowledgment tracking, and release workers each handle one phase of preserving relevant evidence.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `lth_identify` | Takes a case ID and custodian list, generates a hold ID (e.g., HOLD-{caseId}-001), and maps data sources (email, Slack, Drive) for each custodian |
| **NotifyWorker** | `lth_notify` | Sends legal hold notices to all identified custodians and returns the count of notifications sent (e.g., 5) |
| **CollectWorker** | `lth_collect` | Gathers electronically stored information. Documents (1,250), emails (8,400), and messages (3,200), and reports the total item count (12,850) |
| **PreserveWorker** | `lth_preserve` | Places collected data under preservation with a unique preservation ID (PRSV-{timestamp}) and confirms preservation status |
| **TrackWorker** | `lth_track` | Creates a tracking record (TRK-{timestamp}) for the hold and reports its current status (active) |

### The Workflow

```
lth_identify
 │
 ▼
lth_notify
 │
 ▼
lth_collect
 │
 ▼
lth_preserve
 │
 ▼
lth_track

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
