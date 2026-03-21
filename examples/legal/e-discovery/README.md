# E Discovery in Java with Conductor

## The Problem

Litigation is underway and opposing counsel has served discovery requests. You need to identify relevant data sources (email, Slack), collect 45,000+ items totaling 120 GB, de-duplicate and process them down to 28,000 unique documents, have attorneys review for responsiveness and privilege (8,500 responsive, 320 privileged), and produce the final set to opposing counsel. Each stage depends on the prior one, and a missed step can lead to spoliation sanctions or waiver of privilege.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the data collection, document processing, relevance classification, and production set generation logic. Conductor handles collection retries, review sequencing, and defensible discovery audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Data collection, processing, review, and production workers handle electronic discovery through distinct, defensible stages.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `edc_identify` | Maps relevant data sources (email, Slack) and outputs initial collection metadata. 45,000 total items across 120 GB, with estimated 28,000 unique and 8,500 responsive documents |
| **CollectWorker** | `edc_collect` | Collects electronically stored information from identified sources, gathering 45,000 items (120 GB) and de-duplicating to 28,000 unique documents |
| **ProcessWorker** | `edc_process` | De-duplicates, indexes, and normalizes collected data, reducing to 28,000 unique processable documents for review |
| **ReviewWorker** | `edc_review` | Runs attorney review on processed documents, classifying 8,500 as responsive and flagging 320 as privileged |
| **ProduceWorker** | `edc_produce` | Packages the final production set (8,180 non-privileged responsive documents) in the agreed-upon format for delivery to opposing counsel |

### The Workflow

```
edc_identify
 │
 ▼
edc_collect
 │
 ▼
edc_process
 │
 ▼
edc_review
 │
 ▼
edc_produce

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
