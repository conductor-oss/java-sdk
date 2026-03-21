# Document Review in Java with Conductor

## The Problem

A litigation matter requires reviewing 1,500 documents for production. You need to ingest the document batch, classify documents by relevance (420 relevant out of 1,500), have attorneys review for responsiveness (310 responsive), apply privilege designations to protected communications (25 privileged), and produce the final non-privileged responsive set (285 documents) to opposing counsel. Manual handling at this scale is error-prone and risks inadvertent privilege waiver.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the document ingestion, classification, review assignment, and approval logic. Conductor handles classification retries, privilege routing, and document review audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Document ingestion, relevance classification, privilege screening, and annotation workers each handle one stage of the legal review pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **IngestWorker** | `drv_ingest` | Loads 1,500 documents into the review platform, extracting text and metadata for downstream classification |
| **ClassifyWorker** | `drv_classify` | Classifies ingested documents by relevance, identifying 420 relevant documents and flagging 25 potentially privileged items |
| **ReviewWorker** | `drv_review` | Runs attorney review on classified documents, determining 310 as responsive and confirming the 25 privileged documents |
| **PrivilegeWorker** | `drv_privilege` | Applies privilege designations and withholds protected documents, calculating 285 producible (non-privileged responsive) documents |
| **ProduceWorker** | `drv_produce` | Packages the final 285 producible documents into the agreed-upon production format for delivery to opposing counsel |

### The Workflow

```
drv_ingest
 │
 ▼
drv_classify
 │
 ▼
drv_review
 │
 ▼
drv_privilege
 │
 ▼
drv_produce

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
