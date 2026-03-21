# Government Benefit Determination in Java with Conductor : Eligibility Verification, Calculation, and Applicant Notification

## The Problem

You need to process benefit applications for a government assistance program. Each application requires intake validation, income verification against eligibility thresholds, benefit amount calculation based on the applicant's financial profile, and then routing to either an approval notice (with the benefit amount) or a denial notice (with the specific reason). The eligibility decision must branch the workflow. eligible applicants receive a benefit calculation and approval letter, while ineligible applicants receive a denial with an explanation.

Without orchestration, you'd build a monolithic service that queries the applicant database, runs the income check, computes the benefit, and branches with if/else into notification logic. If the eligibility service is temporarily unavailable, you'd need retry logic. If the notification step fails after eligibility is already determined, you'd need to track partial progress. Auditors require a complete trail of every determination for compliance reviews.

## The Solution

**You just write the application intake, eligibility verification, benefit calculation, and approval or denial notification logic. Conductor handles eligibility retries, benefit routing, and determination audit trails.**

Each stage of the benefit determination is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in the right order, routing eligible and ineligible applicants to different notification paths via SWITCH, retrying on failure, and providing a complete audit trail of every determination. ### What You Write: Workers

Application intake, eligibility verification, benefit calculation, and enrollment workers process government benefits through transparent, rule-based stages.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `bnd_apply` | Receives and validates the benefit application for a given applicant and program type |
| **VerifyEligibilityWorker** | `bnd_verify_eligibility` | Checks the applicant's income against the program threshold ($50,000) and returns eligible/ineligible with reason |
| **CalculateWorker** | `bnd_calculate` | Computes the benefit amount based on eligibility status and income level |
| **NotifyEligibleWorker** | `bnd_notify_eligible` | Sends an approval notice to the applicant with their calculated benefit amount |
| **NotifyIneligibleWorker** | `bnd_notify_ineligible` | Sends a denial notice to the applicant with the specific reason for ineligibility |

### The Workflow

```
bnd_apply
 │
 ▼
bnd_verify_eligibility
 │
 ▼
bnd_calculate
 │
 ▼
SWITCH (bnd_switch_ref)
 ├── eligible: bnd_notify_eligible
 ├── ineligible: bnd_notify_ineligible

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
