# Reinsurance in Java with Conductor : Assess Risk, Treaty Lookup, Cede, Confirm, Reconcile

## Insurers Transfer Risk to Reinsurers for Large Exposures

An insurer writes a $10M commercial property policy. That's more risk than they want on their own books. Reinsurance transfers a portion of the risk (and premium) to reinsurers. The process assesses the risk profile (coverage amount, peril, territory, loss history), identifies which reinsurance treaties apply (quota share for the first $5M, excess of loss for amounts above $5M), cedes the appropriate share (notify the reinsurer, transfer premium), confirms the reinsurer's acceptance, and reconciles the accounts at period end.

Reinsurance cession is governed by treaty terms. automatic treaties cede without individual approval, facultative treaties require case-by-case negotiation. The cession must be accurate: ceding too much transfers unnecessary premium, ceding too little retains too much risk. Every cession must be tracked for financial reporting and regulatory capital calculations.

## The Solution

**You just write the risk assessment, treaty lookup, cession calculation, confirmation, and reconciliation logic. Conductor handles cession calculation retries, settlement sequencing, and treaty audit trails.**

`AssessRiskWorker` evaluates the policy's risk profile. coverage amount, peril category, geographic exposure, and loss potential. `TreatyLookupWorker` identifies applicable reinsurance treaties based on the risk characteristics, quota share, surplus, excess of loss, or catastrophe treaties. `CedeWorker` calculates and executes the cession, determining the ceded premium, retained premium, and commission, then notifying the reinsurer. `ConfirmWorker` records the reinsurer's acceptance and binding confirmation. `ReconcileWorker` reconciles ceded premiums and recoveries for financial reporting. Conductor tracks every cession for treaty compliance and financial audit.

### What You Write: Workers

Treaty analysis, cession calculation, premium allocation, and settlement workers each handle one layer of risk transfer between insurers.

| Worker | Task | What It Does |
|---|---|---|
| **AssessRiskWorker** | `rin_assess_risk` | Assesses the policy's risk profile. evaluates the coverage amount and exposure to determine net exposure and retained risk for reinsurance cession decisions |
| **TreatyLookupWorker** | `rin_treaty_lookup` | Identifies the applicable reinsurance treaty. matches the risk category and exposure amount against treaty terms to find the correct treaty (quota share, excess of loss) and determines the cession amount and reinsurer |
| **CedeWorker** | `rin_cede` | Executes the cession. transfers the cession amount under the identified treaty, calculates the ceded premium and ceding commission, and generates the cessionId |
| **ConfirmWorker** | `rin_confirm` | Records the reinsurer's acceptance. confirms the cession binding using the cessionId and reinsurer identity from the treaty lookup |
| **ReconcileWorker** | `rin_reconcile` | Reconciles the cession accounts. matches ceded premiums against reinsurer statements, identifies variances, and prepares the bordereau for financial reporting |

### The Workflow

```
rin_assess_risk
 │
 ▼
rin_treaty_lookup
 │
 ▼
rin_cede
 │
 ▼
rin_confirm
 │
 ▼
rin_reconcile

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
