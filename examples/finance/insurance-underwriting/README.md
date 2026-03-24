# Insurance Underwriting in Java with Conductor

Insurance underwriting with SWITCH decision routing for accept/decline/refer.

## The Problem

You need to underwrite an insurance application. The workflow collects applicant information and coverage requirements, assesses the risk based on applicant profile and coverage type, makes an underwriting decision (accept at standard rates, accept with modified terms, decline, or refer for manual review), and communicates the decision. Accepting high-risk applicants at standard rates leads to adverse selection; declining without proper assessment loses good business.

Without orchestration, you'd build a single underwriting engine that collects data, runs risk models, makes decisions, and sends letters. manually handling referrals that require human underwriter review, retrying failed risk-model API calls, and logging every decision to satisfy state insurance commissioner audits.

## The Solution

**You just write the underwriting workers. Application collection, risk assessment, accept/decline/refer routing, quote generation, and policy binding. Conductor handles conditional SWITCH routing for accept, decline, and refer decisions, automatic retries when the risk model is unavailable, and full application tracking for insurance commissioner audits.**

Each underwriting concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting information, assessing risk, routing via a SWITCH task to the correct decision (accept, decline, refer), and communicating the outcome, retrying if the risk model service is unavailable, tracking every application's underwriting journey, and resuming from the last step if the process crashes.

### What You Write: Workers

Seven workers manage the underwriting process: CollectAppWorker gathers applicant data, AssessRiskWorker evaluates the risk profile, SWITCH routes to AcceptWorker, DeclineWorker, or ReferWorker based on the assessment, QuoteWorker calculates the premium, and BindWorker issues the policy.

| Worker | Task | What It Does |
|---|---|---|
| **AcceptWorker** | `uw_accept` | Accept. Computes and returns accepted, accepted at |
| **AssessRiskWorker** | `uw_assess_risk` | Assess Risk. Computes and returns risk class, decision, risk score, decline reason |
| **BindWorker** | `uw_bind` | Binds the policy if the underwriting decision is 'accept'. Generates a policy number, records the effective date and premium. If the decision is not 'accept', marks the policy as unbound |
| **CollectAppWorker** | `uw_collect_app` | Collect App. Computes and returns applicant data |
| **DeclineWorker** | `uw_decline` | Records the application decline with the reason and flags the application as eligible for appeal |
| **QuoteWorker** | `uw_quote` | Quote. Computes and returns premium, premium frequency, quote valid days |
| **ReferWorker** | `uw_refer` | Routes the application for senior underwriter review with the referral reason and assigns a reviewer |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
uw_collect_app
 │
 ▼
uw_assess_risk
 │
 ▼
uw_quote
 │
 ▼
SWITCH (uw_switch_ref)
 ├── accept: uw_accept
 ├── decline: uw_decline
 ├── refer: uw_refer
 └── default: uw_refer
 │
 ▼
uw_bind

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
