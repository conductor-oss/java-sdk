# Tenant Screening in Java with Conductor : Application, Background Check, Credit Check, and Decision

## The Problem

You need to screen rental applicants before signing a lease. Each applicant submits their information, and you must run a background check (criminal history, eviction records, identity verification) and a credit check (credit score, debt-to-income ratio relative to monthly rent). Both checks must complete before the decision step can weigh the results and issue an approve, conditional approve, or deny decision. Screening must be consistent across applicants and comply with Fair Housing regulations. every applicant must go through the same steps with the same criteria.

Without orchestration, screening is inconsistent and slow. Property managers run credit checks on some applicants but forget background checks on others. When the credit bureau API times out, the application sits in limbo. Nobody can prove that all applicants were evaluated with the same criteria, which creates Fair Housing liability. Building this as a monolithic script means a failure in the background check silently produces an incomplete decision.

## The Solution

**You just write the application intake, background check, credit report, and screening decision logic. Conductor handles credit check retries, verification sequencing, and screening audit trails.**

Each screening step is a simple, independent worker. one logs the application, one runs the background check, one pulls the credit report, one makes the approval decision. Conductor takes care of executing them in order, retrying if the credit bureau is temporarily unavailable, ensuring every applicant goes through the exact same pipeline, and maintaining an audit trail that proves consistent evaluation.

### What You Write: Workers

Application intake, credit check, background verification, and decision notification workers each evaluate one dimension of tenant qualification.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `tsc_apply` | Logs the rental application with applicant name, requested property, and contact details |
| **BackgroundCheckWorker** | `tsc_background` | Runs criminal history, eviction records, and identity verification checks |
| **CreditCheckWorker** | `tsc_credit` | Pulls credit score and evaluates debt-to-income ratio against the monthly rent amount |
| **DecisionWorker** | `tsc_decision` | Decisions the input and returns decision, score |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
tsc_apply
 │
 ▼
tsc_background
 │
 ▼
tsc_credit
 │
 ▼
tsc_decision

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
