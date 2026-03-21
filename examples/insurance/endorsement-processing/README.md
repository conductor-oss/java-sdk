# Endorsement Processing in Java with Conductor : Request Change, Assess Impact, Price, Approve, Apply

## Mid-Term Policy Changes Require Impact Assessment, Repricing, and Approval Before Amendment

When a policyholder requests a mid-term change (adding a driver, increasing coverage, changing a deductible), the insurer must assess the coverage impact, calculate the premium adjustment, obtain approval for the change, and apply the endorsement to the active policy. The premium adjustment depends on the impact assessment, and the endorsement is only applied after approval. If the pricing step fails, you need to retry it without re-assessing the impact.

## The Solution

**You just write the change request intake, impact assessment, repricing, approval, and policy amendment logic. Conductor handles coverage evaluation retries, amendment routing, and endorsement audit trails.**

`RequestChangeWorker` captures the endorsement request. what's changing, effective date, and supporting documentation. `AssessWorker` evaluates the impact on risk exposure, coverage adequacy, and underwriting acceptability. `PriceWorker` calculates the pro-rated premium change, additional premium for increased coverage or refund for reduced coverage. `ApproveWorker` routes the endorsement through approval, auto-approving standard changes or escalating exceptions to underwriters. `ApplyWorker` updates the active policy, modifying coverage terms, issuing an updated declarations page, and adjusting the billing schedule. Conductor tracks the endorsement from request to application.

### What You Write: Workers

Change request intake, coverage evaluation, premium adjustment, and policy amendment workers each handle one aspect of mid-term policy modifications.

| Worker | Task | What It Does |
|---|---|---|
| **RequestChangeWorker** | `edp_request_change` | Receives the endorsement request. logs the policyId and changeType, creates an endorsementId, and initiates the change process |
| **AssessWorker** | `edp_assess` | Assesses the impact of the requested change on the policy. evaluates how the change type and details affect coverage, limits, and risk profile |
| **PriceWorker** | `edp_price` | Calculates the premium adjustment for the endorsement. uses the impact assessment to determine the pro-rata premium change (+$150/year) for the remaining policy term |
| **ApproveWorker** | `edp_approve` | Approves the endorsement. reviews the policyId and premium change amount, then authorizes the amendment to the active policy |
| **ApplyWorker** | `edp_apply` | Applies the endorsement to the active policy. amends the policy record with the approved changes using the endorsementId, updates the declarations page, and triggers billing adjustment |

### The Workflow

```
edp_request_change
 │
 ▼
edp_assess
 │
 ▼
edp_price
 │
 ▼
edp_approve
 │
 ▼
edp_apply

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
