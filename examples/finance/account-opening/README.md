# Account Opening in Java with Conductor

A customer spent eight minutes filling out your online account application on their phone during their lunch break. They uploaded a photo of their driver's license, entered their SSN, picked a checking account with a $1,000 initial deposit. then hit a screen that said "Please visit your nearest branch to complete identity verification." They didn't. They opened an account with a competitor that afternoon. Your conversion report shows 62% of applications abandoned at the identity verification step, but nobody connects that to the fact that the verification API, the credit check, and the core banking provisioning are three separate manual workflows stitched together by a case management queue that a human has to advance. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate account opening end-to-end, collect documents, verify identity, run credit checks, provision the account, and send the welcome package, as a single automated pipeline that completes in minutes, not days.

## The Problem

You need to open a new bank account for a customer. This requires collecting applicant information, verifying their identity against government databases, running a credit check to determine eligibility, provisioning the account in your core banking system with the initial deposit, and sending a welcome package with account details. Opening an account without identity verification exposes the bank to fraud; skipping the credit check violates regulatory requirements.

Without orchestration, you'd chain identity verification API calls, credit bureau queries, and core banking system writes in a single service. Manually handling timeouts from slow identity providers, retrying failed credit checks, and ensuring a partially opened account is never left in an inconsistent state.

## The Solution

**You just write the account opening workers. Document collection, identity verification, credit check, account provisioning, and welcome package. Conductor handles step ordering, automatic retries when the credit bureau API times out, and complete application lifecycle tracking.**

Each account-opening concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (collect info, verify identity, credit check, open account, send welcome), retrying if the credit bureau API times out, tracking every application's full lifecycle, and resuming from the last successful step if the process crashes mid-verification. ### What You Write: Workers

Five workers cover the onboarding pipeline: CollectInfoWorker gathers identity documents, VerifyIdentityWorker runs KYC checks, CreditCheckWorker queries ChexSystems, OpenAccountWorker provisions the account, and WelcomeWorker sends the welcome package.

| Worker | Task | What It Does |
|---|---|---|
| **CollectInfoWorker** | `acc_collect_info` | Collects applicant documents (driver's license, SSN, proof of address) and records that all required identity documents are present |
| **CreditCheckWorker** | `acc_credit_check` | Runs a credit and banking history check via ChexSystems |
| **OpenAccountWorker** | `acc_open_account` | Provisions the new account in the core banking system. Generates an account number, assigns a routing number, and records the account type and initial deposit |
| **VerifyIdentityWorker** | `acc_verify_identity` | Verifies the applicant's identity using KYC document checks |
| **WelcomeWorker** | `acc_welcome` | Sends the welcome package to the new account holder. Includes debit card, checks, online banking enrollment, and mobile app setup |

### The Workflow

```
acc_collect_info
 │
 ▼
acc_verify_identity
 │
 ▼
acc_credit_check
 │
 ▼
acc_open_account
 │
 ▼
acc_welcome

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
