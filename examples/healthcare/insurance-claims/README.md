# Health Insurance Claims in Java Using Conductor : Submission, Verification, Adjudication, Payment, and Closure

## The Problem

You need to process health insurance claims from submission through payment. A provider submits a claim with the patient ID, procedure code, and billed amount. The claim must be verified. confirming the member's active coverage, the provider's network status, and that the procedure is a covered benefit. The verified claim is then adjudicated against the policy's benefit rules, applying deductibles, copays, coinsurance, and out-of-pocket maximums to determine the allowed amount. Payment is issued to the provider for the approved amount. Finally, the claim is closed with an explanation of benefits (EOB). Each step depends on the previous one, you cannot adjudicate without verifying eligibility, and you cannot pay without adjudication.

Without orchestration, you'd build a monolithic claims engine that receives the 837 claim, checks eligibility, runs the adjudication rules, triggers the payment, and generates the EOB. If the eligibility check system is temporarily unavailable, you'd need retry logic. If the process crashes after adjudication but before payment, the provider is not paid and the claim is stuck. State insurance regulators require a complete audit trail of every claim decision for compliance reviews and prompt-pay law enforcement.

## The Solution

**You just write the claims workers. Submission intake, eligibility verification, adjudication, payment, and EOB closure. Conductor handles strict step ordering, automatic retries when the eligibility system is temporarily unavailable, and a complete regulatory audit trail from submission to closure.**

Each stage of claims processing is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of verifying before adjudicating, paying only after adjudication approves the claim, retrying if the eligibility system is temporarily unavailable, and maintaining a complete regulatory audit trail from submission to closure. ### What You Write: Workers

Five workers manage the claims lifecycle: SubmitClaimWorker intakes the claim, VerifyClaimWorker checks eligibility, AdjudicateClaimWorker applies benefit rules, PayClaimWorker issues provider payment, and CloseClaimWorker generates the EOB.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitClaimWorker** | `clm_submit` | Receives and registers the incoming claim with procedure code, patient, provider, and billed amount |
| **VerifyClaimWorker** | `clm_verify` | Verifies member eligibility, provider network status, and benefit coverage for the submitted procedure |
| **AdjudicateClaimWorker** | `clm_adjudicate` | Applies benefit rules. deductibles, copays, coinsurance, policy limits, to determine the allowed payment amount |
| **PayClaimWorker** | `clm_pay` | Issues payment to the provider via EFT/check and generates the remittance advice (835) |
| **CloseClaimWorker** | `clm_close` | Closes the claim, generates the Explanation of Benefits (EOB), and archives the record |

the workflow and compliance logic stay the same.

### The Workflow

```
clm_submit
 │
 ▼
clm_verify
 │
 ▼
clm_adjudicate
 │
 ▼
clm_pay
 │
 ▼
clm_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
