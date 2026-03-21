# Real Estate Commission Calculation in Java with Conductor : Base Rate, Tiered Adjustments, Deductions, and Payout

## The Problem

You need to calculate agent commissions when a property sale closes. The calculation is never as simple as "sale price times percentage." You start with a base commission rate, then apply tiered adjustments. senior agents earn a higher split, top performers get bonus percentages, team leads take an override. Then you subtract deductions: brokerage desk fees, marketing costs, referral fees paid to other agents. Finally, the net commission is finalized and a payment is issued. Each step depends on the previous one, and any error in the chain means an agent gets paid the wrong amount.

Without orchestration, commission calculations live in a spreadsheet or a monolithic script that nobody trusts. When the tier structure changes, the deduction logic breaks. When a payment fails, nobody knows whether the calculation was correct but the transfer failed, or whether the whole thing needs to be rerun. Agents dispute their payouts, and finance can't reconstruct how a number was derived.

## The Solution

**You just write the base commission, tier adjustment, deduction, and payout logic. Conductor handles tier calculation retries, deduction sequencing, and payout audit trails.**

Each step in the commission pipeline is a simple, independent worker. one computes the base commission, one applies tier adjustments, one subtracts deductions, one finalizes the payout. Conductor takes care of executing them in sequence, retrying if the payment system is temporarily unavailable, and maintaining a complete audit trail showing exactly how each commission was calculated. ### What You Write: Workers

Base rate calculation, tiered adjustment, deduction application, and payout finalization workers each handle one layer of the commission computation.

| Worker | Task | What It Does |
|---|---|---|
| **BaseCommissionWorker** | `cmc_base` | Calculates the base commission amount from the sale price using the standard commission rate |
| **TiersWorker** | `cmc_tiers` | Applies tiered adjustments based on agent seniority, performance level, and team split structure |
| **DeductionsWorker** | `cmc_deductions` | Subtracts brokerage fees, desk fees, marketing costs, and referral fees from the tiered amount |
| **FinalizeWorker** | `cmc_finalize` | Records the net commission and initiates payment to the agent, returning a payment confirmation ID |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs. ### The Workflow

```
cmc_base
 │
 ▼
cmc_tiers
 │
 ▼
cmc_deductions
 │
 ▼
cmc_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
