# KYC AML in Java with Conductor

KYC/AML workflow that verifies customer identity, screens against watchlists, assesses risk, and makes a compliance decision.

## The Problem

You need to verify a customer's identity and screen them against anti-money-laundering watchlists before onboarding. The workflow verifies the customer's identity documents, screens their name against sanctions lists (OFAC, EU, UN), PEP lists, and adverse media, assesses the overall risk level, and makes a compliance decision (approve, enhanced due diligence, or reject). Onboarding a sanctioned individual exposes the institution to massive fines and criminal liability.

Without orchestration, you'd build a single compliance service that calls identity verification APIs, queries watchlist databases, runs risk scoring, and records decisions. manually handling conflicting results from different watchlist providers, retrying failed API calls, and maintaining an audit trail that regulators can inspect.

## The Solution

**You just write the compliance workers. Identity verification, watchlist screening, risk assessment, and approval/rejection decision. Conductor handles step sequencing, automatic retries when a watchlist provider is unavailable, and a tamper-evident compliance audit trail.**

Each KYC/AML concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (verify identity, screen watchlists, assess risk, make decision), retrying if a watchlist provider is unavailable, maintaining a complete compliance audit trail, and resuming from the last step if the process crashes.

### What You Write: Workers

Four workers form the compliance pipeline: VerifyIdentityWorker checks identity documents, ScreenWatchlistsWorker queries OFAC, PEP, and adverse media lists, AssessRiskWorker computes the overall risk level, and DecideWorker makes the approve, EDD, or reject determination.

| Worker | Task | What It Does |
|---|---|---|
| **AssessRiskWorker** | `kyc_assess_risk` | Assesses overall KYC risk based on identity verification and watchlist results. |
| **DecideWorker** | `kyc_decide` | Makes a compliance decision based on the assessed risk level. |
| **ScreenWatchlistsWorker** | `kyc_screen_watchlists` | Screens customer against OFAC, PEP, and adverse media watchlists. |
| **VerifyIdentityWorker** | `kyc_verify_identity` | Verifies customer identity using the provided document type. |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
kyc_verify_identity
 │
 ▼
kyc_screen_watchlists
 │
 ▼
kyc_assess_risk
 │
 ▼
kyc_decide

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
