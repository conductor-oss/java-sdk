# RFP Automation in Java with Conductor : RFP Creation, Vendor Distribution, Response Collection, Evaluation, and Vendor Selection

## The Problem

You need to run a structured RFP process for a cloud infrastructure migration. The RFP must clearly specify requirements (scalability, security, 24/7 support) and a response deadline. It must be distributed to qualified vendors in your approved vendor list. Vendor responses must be collected and validated for completeness before the deadline. Each response must be evaluated against the stated requirements with consistent scoring. The vendor with the best overall score is selected, and the decision must be defensible in case of a protest.

Without orchestration, procurement creates RFPs in Word documents, emails them to vendors, and tracks responses in a spreadsheet. Some vendors never receive the RFP because the email bounced. Evaluation criteria are applied inconsistently because different reviewers use different weights. When a losing vendor asks why they weren't selected, there is no traceable scoring record linking their response to the evaluation criteria.

## The Solution

**You just write the RFP workers. Creation, vendor distribution, response collection, evaluation scoring, and vendor selection. Conductor handles vendor notification retries, consistent scoring sequencing, and traceable evaluation records for protest defense.**

Each phase of the RFP process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so the RFP is finalized before distribution, all vendors are notified before collection begins, responses are complete before evaluation, and evaluation scores drive selection. If the distribution worker fails for one vendor, Conductor retries without re-sending to vendors already notified. Every RFP version, distribution receipt, response submission, evaluation score, and selection decision is recorded for procurement audit and vendor protest defense.

### What You Write: Workers

Five workers automate the RFP process: CreateWorker defines requirements and criteria, DistributeWorker sends to qualified vendors, CollectWorker gathers responses, EvaluateWorker scores proposals, and SelectWorker picks the winner.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWorker** | `rfp_collect` | Collects vendor responses and validates completeness before the deadline. |
| **CreateWorker** | `rfp_create` | Creates the RFP with project requirements, evaluation criteria, and response deadline. |
| **DistributeWorker** | `rfp_distribute` | Distributes the RFP to qualified vendors in the approved vendor list. |
| **EvaluateWorker** | `rfp_evaluate` | Scores each vendor response against the stated requirements with consistent criteria. |
| **SelectWorker** | `rfp_select` | Selects the winning vendor based on evaluation scores. |

### The Workflow

```
rfp_create
 │
 ▼
rfp_distribute
 │
 ▼
rfp_collect
 │
 ▼
rfp_evaluate
 │
 ▼
rfp_select

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
