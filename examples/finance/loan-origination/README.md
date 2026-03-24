# Loan Origination in Java with Conductor

Loan origination: application intake, credit check, underwriting, approval, and funding.

## The Problem

You need to originate a loan from application to funding. An applicant submits a loan application, a credit check evaluates their creditworthiness, underwriting assesses the loan's risk and terms, an approval decision is made, and funds are disbursed. Each step depends on the previous. you cannot underwrite without a credit report, and you cannot fund without approval. Funding a loan without proper credit assessment creates bad debt exposure.

Without orchestration, you'd build a monolithic loan pipeline that collects applications, pulls credit reports, runs underwriting models, updates loan status, and triggers funding. manually handling the weeks-long lifecycle, retrying failed credit bureau calls, and tracking every step for TILA/RESPA compliance.

## The Solution

**You just write the loan workers. Application intake, credit check, underwriting, approval, and fund disbursement. Conductor handles pipeline ordering, automatic retries when the credit bureau API times out, and full application lifecycle tracking for TILA/RESPA compliance.**

Each origination concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (intake, credit check, underwriting, approval, funding), retrying if the credit bureau API times out, tracking every application's full journey for regulatory compliance, and resuming from the last step if the process crashes.

### What You Write: Workers

Five workers cover the origination pipeline: ApplicationWorker intakes the application, CreditCheckWorker pulls the credit report, UnderwriteWorker assesses risk and terms, ApproveWorker records the decision, and FundWorker disburses the loan.

| Worker | Task | What It Does |
|---|---|---|
| **ApplicationWorker** | `lnr_application` | Receives a loan application and records intake details. |
| **ApproveWorker** | `lnr_approve` | Approves the loan after underwriting. |
| **CreditCheckWorker** | `lnr_credit_check` | Runs a credit check for the loan applicant. |
| **FundWorker** | `lnr_fund` | Disburses the loan funds to the applicant. |
| **UnderwriteWorker** | `lnr_underwrite` | Underwrites the loan based on credit score and DTI ratio. |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
lnr_application
 │
 ▼
lnr_credit_check
 │
 ▼
lnr_underwrite
 │
 ▼
lnr_approve
 │
 ▼
lnr_fund

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
