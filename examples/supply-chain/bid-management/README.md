# Bid Management in Java with Conductor : RFP Creation, Vendor Distribution, Bid Collection, Evaluation, and Award

## The Problem

You need to run a competitive bidding process across multiple vendors. The procurement team creates a bid package with project specs and budget, distributes it to a shortlist of vendors (Alpha Corp, Beta Ltd, Gamma Inc), collects their proposals by a deadline, evaluates each bid on cost, schedule, and qualifications, and awards the contract. If a vendor's submission fails to upload, you need to retry without losing other submissions. If the evaluation step crashes, you need to resume without re-soliciting bids.

Without orchestration, you'd manage this in email threads and spreadsheets. manually tracking which vendors received the RFP, chasing late submissions, and comparing proposals in a shared doc. There is no audit trail of when bids were received, evaluation criteria applied inconsistently across reviewers, and the award decision has no traceable link to the scored evaluations.

## The Solution

**You just write the bid lifecycle workers. RFP creation, vendor distribution, proposal collection, evaluation scoring, and contract award. Conductor handles sequencing, retries, and full audit trails for procurement compliance.**

Each phase of the bidding lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so the RFP is fully created before distribution, bids are only collected after all vendors have been notified, evaluation only runs once all submissions are in, and the award references the evaluation scores. If the distribution worker fails for one vendor, Conductor retries without re-sending to vendors already notified. Every step is recorded with timestamps and outputs for procurement audit compliance.

### What You Write: Workers

Five workers divide the bid lifecycle: CreateWorker builds the RFP package, DistributeWorker sends it to vendors, CollectWorker gathers proposals, EvaluateWorker scores them, and AwardWorker issues the contract.

| Worker | Task | What It Does |
|---|---|---|
| **AwardWorker** | `bid_award` | Awards the contract to the winning bidder based on evaluation results. |
| **CollectWorker** | `bid_collect` | Collects submitted bid responses from vendors by the deadline. |
| **CreateWorker** | `bid_create` | Creates a bid package with project specifications and budget. |
| **DistributeWorker** | `bid_distribute` | Distributes the RFP to the shortlisted vendors. |
| **EvaluateWorker** | `bid_evaluate` | Scores each bid against cost, timeline, and capability criteria. |

### The Workflow

```
bid_create
 │
 ▼
bid_distribute
 │
 ▼
bid_collect
 │
 ▼
bid_evaluate
 │
 ▼
bid_award

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
