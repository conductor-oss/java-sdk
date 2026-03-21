# Insurance Renewal in Java with Conductor : Notify, Review Risk, Reprice, Route Decision

## Policy Renewals Require Claims Review, Repricing, and Conditional Routing

When an insurance policy approaches its renewal date, the insurer must review the policyholder's claims history, calculate a risk score, reprice the premium accordingly, and decide whether to renew or non-renew the policy. If the risk is acceptable, the policy renews at the adjusted premium. If the claims history is too costly, the policy is cancelled with a stated reason. The repricing step must use the risk score from the review. if repricing fails, you need to retry it without re-reviewing the entire claims history.

## The Solution

**You just write the renewal notification, risk review, premium repricing, and renew-or-cancel routing logic. Conductor handles repricing retries, decision routing, and renewal audit trails for every policy.**

`NotifyWorker` sends the renewal notice to the policyholder with the current policy details and renewal timeline. `ReviewWorker` examines the policy's claims history, loss ratio, risk factor changes, and market conditions for the coverage area. `RepriceWorker` calculates the updated premium based on the risk review. applying experience rating, inflation adjustments, and regulatory rate changes. Conductor's `SWITCH` routes to renewal (issue at the new premium) or non-renewal (send non-renewal notice with reason) based on the repricing results and underwriting guidelines. Conductor records the complete renewal decision chain for regulatory compliance.

### What You Write: Workers

Notification, risk review, repricing, and renewal processing workers each address one stage of the policy renewal decision.

| Worker | Task | What It Does |
|---|---|---|
| **NotifyWorker** | `irn_notify` | Sends a renewal notice to the policyholder. identifies the policy and customer, then dispatches the renewal communication |
| **ReviewWorker** | `irn_review` | Reviews the policy's claims history. analyzes past claims for the policyId and outputs a riskScore (0.35) and claimsCount (1) that determine repricing |
| **RepriceWorker** | `irn_reprice` | Reprices the premium based on the risk score. adjusts the annual premium ($1,200/year) and outputs a decision ("renew" or "cancel") along with the newPremium that the SWITCH uses for routing |
| *SWITCH* | `route_decision` | Routes based on the reprice decision: "renew" advances to renewal processing with the new premium, "cancel" routes to cancellation processing with the reason | Built-in Conductor SWITCH. no worker needed |
| **ProcessRenewWorker** | `irn_process_renew` | Processes the renewal. issues the renewed policy at the new premium amount and updates the policy term |
| **ProcessCancelWorker** | `irn_process_cancel` | Processes the cancellation. records the non-renewal reason and triggers required regulatory notices to the policyholder |

### The Workflow

```
irn_notify
 │
 ▼
irn_review
 │
 ▼
irn_reprice
 │
 ▼
SWITCH (irn_switch_ref)
 ├── renew: irn_process_renew
 ├── cancel: irn_process_cancel

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
