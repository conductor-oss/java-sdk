# Medical Billing in Java Using Conductor : CPT/ICD Coding, Coverage Verification, Claim Submission, and Payment Tracking

## The Problem

You need to bill for a clinical encounter. After a patient visit, the encounter must be coded with CPT procedure codes (e.g., 99213 for an office visit, 36415 for venipuncture) and ICD diagnosis codes (e.g., E11.9 for type 2 diabetes, I10 for hypertension). The patient's insurance coverage must be verified against the coded procedures. A claim is then submitted to the payer with the coded line items and total charge. Finally, the payment must be tracked through adjudication to reconcile what was billed versus what was reimbursed.

Without orchestration, you'd build a monolithic billing service that queries the encounter record, looks up codes, calls the eligibility API, formats and submits the 837 claim, and polls for the 835 remittance. If the payer's eligibility endpoint is down, you'd need retry logic. If the system crashes after coding but before claim submission, the encounter sits unbilled and revenue is delayed. Billing managers need a clear audit trail of every claim from coding through payment for denial management and compliance.

## The Solution

**You just write the billing workers. CPT/ICD coding, coverage verification, claim submission, and payment tracking. Conductor handles task ordering, automatic retries when the payer system is temporarily down, and a complete audit trail from encounter to payment.**

Each stage of the billing cycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running coding before coverage verification, submitting claims only after coverage is confirmed, retrying if the payer's system is temporarily unavailable, and maintaining a complete audit trail from encounter to payment.

### What You Write: Workers

Four workers cover the billing cycle: CodeProceduresWorker assigns CPT and ICD codes, VerifyCoverageWorker checks insurance eligibility, SubmitClaimWorker sends the 837 to the clearinghouse, and TrackPaymentWorker monitors adjudication.

| Worker | Task | What It Does |
|---|---|---|
| **CodeProceduresWorker** | `mbl_code_procedures` | Assigns CPT procedure codes and ICD diagnosis codes to the encounter, calculates total charge |
| **VerifyCoverageWorker** | `mbl_verify_coverage` | Checks the patient's insurance eligibility and benefit coverage for the coded procedures |
| **SubmitClaimWorker** | `mbl_submit_claim` | Formats and submits the claim (837P/837I) to the payer clearinghouse |
| **TrackPaymentWorker** | `mbl_track_payment` | Monitors the claim through adjudication and records the 835 remittance/payment |

the workflow and compliance logic stay the same.

### The Workflow

```
mbl_code_procedures
 │
 ▼
mbl_verify_coverage
 │
 ▼
mbl_submit_claim
 │
 ▼
mbl_track_payment

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
