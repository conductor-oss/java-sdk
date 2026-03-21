# Policy Issuance in Java with Conductor : Underwrite, Approve, Generate Policy, Issue, Deliver

## New Insurance Applications Must Flow Through Underwriting, Approval, and Issuance

When a new insurance application arrives, the insurer must underwrite the applicant (assess risk class based on coverage type), approve the application (determine the premium from the underwriting result), generate the policy document with all terms and conditions, officially issue the policy in the administration system, and deliver the documents to the policyholder. If document generation fails after approval, you need to retry it without re-underwriting or re-approving the application.

## The Solution

**You just write the underwriting, approval, document generation, policy issuance, and delivery logic. Conductor handles underwriting retries, document generation sequencing, and policy lifecycle audit trails.**

`UnderwriteWorker` evaluates the application. property inspection data, credit score, claims history, coverage amount vs: property value, and produces a risk assessment with a recommendation. `ApproveWorker` makes the acceptance decision based on underwriting guidelines, with auto-approval for standard risks and referral for exceptions. `GeneratePolicyWorker` creates the policy document, declarations page, coverage schedule, endorsements, and exclusions specific to the approved coverage. `IssueWorker` records the policy in the administration system with policy number, effective dates, and premium schedule. `DeliverWorker` sends the policy package to the policyholder via their preferred channel. Conductor tracks the full issuance timeline.

### What You Write: Workers

Application intake, underwriting, policy document generation, and activation workers each handle one step of bringing a new policy into force.

| Worker | Task | What It Does |
|---|---|---|
| **UnderwriteWorker** | `pis_underwrite` | Underwrites the applicant. evaluates the applicantId and coverageType to determine the risk classification (standard, preferred, substandard) and underwriting result |
| **ApproveWorker** | `pis_approve` | Approves the application based on the underwriting result. reads the riskClass to determine the premium and issues the approval decision |
| **GeneratePolicyWorker** | `pis_generate_policy` | Generates the policy document. creates the declarations page, coverage schedule, and terms using the applicant details, coverage type, and approved premium, then outputs the policyId |
| **IssueWorker** | `pis_issue` | Officially issues the policy. records the policy in the administration system with the assigned policyId, setting the effective date and policy status to active |
| **DeliverWorker** | `pis_deliver` | Delivers the policy documents to the policyholder. sends the generated policy via the appropriate delivery method (email, postal mail, portal) |

### The Workflow

```
pis_underwrite
 │
 ▼
pis_approve
 │
 ▼
pis_generate_policy
 │
 ▼
pis_issue
 │
 ▼
pis_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
