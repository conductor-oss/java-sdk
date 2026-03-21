# Vendor Onboarding in Java with Conductor : Application, Credential Verification, Evaluation, Approval, and System Activation

## The Problem

You need to onboard new vendors into your supply chain. A prospective vendor submits an application with their business details, category, and country of operation. Their credentials must be verified. business registration, insurance coverage, industry certifications (ISO, SOC 2). The vendor must be evaluated on financial health (D&B rating, credit check) and operational capability. Based on the evaluation score, procurement approves or rejects the application. Approved vendors must be activated in the ERP vendor master so buyers can issue purchase orders to them.

Without orchestration, vendor applications arrive via email, sit in someone's inbox for weeks, and credential checks happen over phone calls with no record. Vendors get activated in the ERP before their insurance is verified, exposing the company to liability risk. When compliance asks which vendors were onboarded last quarter and what checks were performed, nobody can produce the documentation without hours of email archaeology.

## The Solution

**You just write the onboarding workers. Application intake, credential verification, evaluation, approval, and system activation. Conductor handles credential verification retries, enforced approval gates, and timestamped records for compliance audits.**

Each stage of the vendor onboarding process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so applications are received before verification, credentials are verified before evaluation, evaluation gates approval, and activation only happens for approved vendors. If the credential verification API is temporarily unavailable, Conductor retries without losing the application data. Every application, verification result, evaluation score, approval decision, and activation timestamp is recorded for compliance audits and vendor management analytics.

### What You Write: Workers

Five workers manage vendor onboarding: ApplyWorker receives the application, VerifyWorker checks credentials and insurance, EvaluateWorker scores financial health, ApproveWorker decides eligibility, and ActivateWorker enables the vendor in the ERP.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateWorker** | `von_activate` | Activates the vendor in the system. |
| **ApplyWorker** | `von_apply` | Submits a vendor application. |
| **ApproveWorker** | `von_approve` | Approves or rejects a vendor based on score. |
| **EvaluateWorker** | `von_evaluate` | Evaluates the vendor and assigns a score. |
| **VerifyWorker** | `von_verify` | Verifies vendor credentials. |

### The Workflow

```
von_apply
 │
 ▼
von_verify
 │
 ▼
von_evaluate
 │
 ▼
von_approve
 │
 ▼
von_activate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
