# Patent Filing in Java with Conductor

## The Problem

An inventor submits a new invention disclosure. You need to draft a patent application with claims (typically 12+), conduct a prior art search to check for novelty, have a patent attorney review the draft for quality and completeness, file the application with the USPTO, and track its status through examination. Missing a filing deadline or overlooking prior art can forfeit patent rights entirely.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the prior art search, application drafting, filing submission, and status tracking logic. Conductor handles search retries, filing sequencing, and patent application audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Prior art search, claim drafting, filing preparation, and submission workers each manage one stage of the patent application process.

| Worker | Task | What It Does |
|---|---|---|
| **DraftWorker** | `ptf_draft` | Takes the invention title and drafts a patent application, generating a draft ID (DRF-{timestamp}) and defining 12 claims |
| **ReviewWorker** | `ptf_review` | Reviews the draft for claim quality and completeness, returning an approval status and reviewer comments (e.g., "Claims well-drafted") |
| **PriorArtWorker** | `ptf_prior_art` | Searches prior art databases for the invention title, reports 3 matches found, and assesses conflict risk as "low" with a clearance flag |
| **FileWorker** | `ptf_file` | Files the patent application with the USPTO, generating an application number (US-2024-XXXXXX) and recording the filing date |
| **TrackWorker** | `ptf_track` | Creates a tracking record (TRK-{timestamp}) for the filed application and reports its status as "pending-examination" |

### The Workflow

```
ptf_draft
 │
 ▼
ptf_review
 │
 ▼
ptf_prior_art
 │
 ▼
ptf_file
 │
 ▼
ptf_track

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
