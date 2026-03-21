# Insurance Commission Processing in Java with Conductor : Calculate, Validate, Deduct Advances, Pay, Report

## Agent Commissions Have Advances, Tiers, and Clawbacks

An agent writes a $2,000 annual premium policy with a 15% new business commission rate ($300). But the agent received a $200 advance against future commissions last month. And if the policy cancels within 6 months, the commission must be clawed back. The commission calculation must apply the correct rate (new business vs: renewal, product-specific tiers), deduct any outstanding advances, process the net payment, and track the potential clawback liability.

Commission processing involves multiple rates (new business 15%, renewal 10%, bonus tiers above production thresholds), advance tracking (deducting advances from earned commissions), clawback management (reversing commissions on cancelled policies within the clawback period), and regulatory reporting (1099 forms, state premium tax credits).

## The Solution

**You just write the commission calculation, advance deduction, validation, payout processing, and reporting logic. Conductor handles split calculation retries, payment sequencing, and commission audit trails.**

`CalculateWorker` computes the commission amount using the applicable rate. new business or renewal, product-specific tiers, and any bonus qualifications. `ValidateWorker` verifies the calculation against the agent's commission schedule and carrier guidelines. `DeductAdvancesWorker` subtracts any outstanding advance balances from the earned commission. `PayWorker` issues the net commission payment to the agent via their configured payment method. `ReportWorker` generates commission statements and updates the agent's production records for year-end 1099 reporting. Conductor tracks every commission calculation for financial audit.

### What You Write: Workers

Policy matching, rate calculation, split allocation, and payment processing workers each handle one layer of insurance commission computation.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateWorker** | `cin_calculate` | Calculates the commission. applies the applicable rate (15% new business, 10% renewal) to the premium amount, with tier adjustments for production thresholds and product-specific rates |
| **ValidateWorker** | `cin_validate` | Validates the commission. verifies the calculated amount against the agent's commission schedule and carrier guidelines, checking for rate overrides and special agreements |
| **DeductAdvancesWorker** | `cin_deduct_advances` | Deducts outstanding advances. subtracts any advance balance from the gross commission to determine the net commission payable, tracking the amortization schedule |
| **PayWorker** | `cin_pay` | Issues the commission payment. processes the net commission via the agent's configured payment method and generates a paymentId for reconciliation |
| **ReportWorker** | `cin_report` | Files the commission report. records the transaction for the agent and policy, updates year-to-date production totals, and generates the commission statement |

### The Workflow

```
cin_calculate
 │
 ▼
cin_validate
 │
 ▼
cin_deduct_advances
 │
 ▼
cin_pay
 │
 ▼
cin_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
