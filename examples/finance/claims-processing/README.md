# Claims Processing in Java with Conductor

A policyholder rear-ends someone at a stoplight and files a claim that afternoon. Eight days pass before an adjuster is assigned. the intake system routed it to a queue that nobody monitors on weekends. The adjuster schedules an inspection, but the body shop estimate doesn't come back for another three weeks because the request sat in the shop's email. Six weeks after a fender bender, the customer still doesn't have a repair estimate, and they switch carriers. The claim itself was straightforward, the process just had no urgency, no tracking, and no escalation when steps stalled. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full claims lifecycle, submission, verification, damage assessment, settlement, and closure, as independent workers.

## The Problem

You need to process insurance claims from submission to settlement. A policyholder submits a claim with description and estimated amount, the claims team verifies policy coverage and claim details, a damage assessment determines the actual loss, the settlement amount is calculated based on coverage limits and deductibles, and the claim is closed. Settling without proper verification exposes the insurer to fraudulent claims; inadequate damage assessment leads to underpayment disputes.

Without orchestration, you'd build a monolithic claims handler that validates policies, contacts adjusters, calculates settlements, and updates the claims database. Manually coordinating between adjusters in the field, retrying failed policy lookups, and maintaining claim state across what can be a weeks-long process.

## The Solution

**You just write the claims workers. Submission intake, policy verification, damage assessment, settlement calculation, and claim closure. Conductor handles sequential step execution, automatic retries when the policy system is unavailable, and full claims lifecycle tracking for regulatory compliance.**

Each claims concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (submit, verify, assess, settle, close), retrying if the policy system is unavailable, tracking every claim's full lifecycle for regulatory compliance, and resuming from the last step if the process crashes. ### What You Write: Workers

Five workers handle the claims lifecycle: SubmitClaimWorker intakes the claim, VerifyDetailsWorker checks policy coverage, AssessDamageWorker determines actual loss, SettleAmountWorker calculates the payout, and CloseClaimWorker finalizes the record.

| Worker | Task | What It Does |
|---|---|---|
| **AssessDamageWorker** | `clp_assess_damage` | Assesses damage for a claim and computes assessed amount (85% of requested). |
| **CloseClaimWorker** | `clp_close_claim` | Closes a claim after settlement. |
| **SettleAmountWorker** | `clp_settle_amount` | Settles claim amount (assessed minus $500 deductible). |
| **SubmitClaimWorker** | `clp_submit_claim` | Submits an insurance claim and returns policy metadata. |
| **VerifyDetailsWorker** | `clp_verify_details` | Verifies policy details for a claim. |

### The Workflow

```
clp_submit_claim
 │
 ▼
clp_verify_details
 │
 ▼
clp_assess_damage
 │
 ▼
clp_settle_amount
 │
 ▼
clp_close_claim

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
