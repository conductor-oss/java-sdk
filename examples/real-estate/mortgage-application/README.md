# Mortgage Application in Java with Conductor : Apply, Credit Check, Underwriting, Approval, and Closing

## The Problem

You need to process mortgage applications from submission to closing. An applicant requests a loan. the application must be logged, a credit check must be run to pull their score, underwriting must evaluate the risk by comparing credit score, loan amount, and property value (LTV ratio), an approval or denial decision must be issued, and approved loans must proceed to closing with final documentation. Each step feeds into the next: underwriting can't start without the credit score, approval can't happen without the underwriting assessment.

Without orchestration, mortgage processing is a manual pipeline prone to bottlenecks. Loan officers email underwriters, credit checks are requested via phone, and applications sit in queues for days. A monolithic script that tries to automate this breaks when the credit bureau API times out, and nobody knows whether the underwriting step ran or not. Regulators require an audit trail of every decision, and reconstructing one from logs is a nightmare.

## The Solution

**You just write the application intake, credit check, underwriting analysis, approval decision, and loan closing logic. Conductor handles credit check retries, underwriting sequencing, and application audit trails.**

Each mortgage processing step is a simple, independent worker. one logs the application, one pulls the credit score, one performs underwriting analysis, one issues the approval, one handles closing. Conductor takes care of executing them in strict order, retrying if the credit bureau API is temporarily unavailable, and maintaining a complete audit trail of every decision point for regulatory compliance.

### What You Write: Workers

Application intake, credit evaluation, underwriting, and closing workers each handle one stage of the mortgage approval process.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `mtg_apply` | Logs the mortgage application with applicant details and requested loan amount |
| **CreditCheckWorker** | `mtg_credit_check` | Pulls the applicant's credit score and credit history from a bureau (Equifax, Experian, TransUnion) |
| **UnderwriteWorker** | `mtg_underwrite` | Evaluates loan risk using credit score, loan-to-value ratio, and debt-to-income analysis |
| **ApproveWorker** | `mtg_approve` | Issues the approval or denial decision based on underwriting results, assigns a loan ID |
| **CloseWorker** | `mtg_close` | Finalizes the loan. generates closing documents, records the mortgage, and disburses funds |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
mtg_apply
 │
 ▼
mtg_credit_check
 │
 ▼
mtg_underwrite
 │
 ▼
mtg_approve
 │
 ▼
mtg_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
