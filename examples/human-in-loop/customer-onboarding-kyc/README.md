# Customer Onboarding KYC in Java Using Conductor: Risk Assessment, SWITCH for Auto-Approve vs. Manual Review, and Account Activation

## The Problem

You need to onboard customers with KYC compliance checks. Each customer is assessed for risk based on their identity, country of residence, source of funds, and PEP (Politically Exposed Person) status. Low-risk customers: those with clean identity checks, domestic addresses, and no PEP flags, should be auto-approved and activated instantly for a frictionless experience. High-risk customers, those with sanctions hits, high-risk jurisdictions, or PEP connections, must be routed to a compliance analyst for manual review before the account can be activated. The analyst reviews the flags, may request additional documentation, and makes a final approve/reject decision. Without this conditional routing, you either manually review every customer (slow and expensive) or auto-approve everyone (regulatory violation).

Without orchestration, you'd build a monolithic KYC system that runs risk checks, branches with if/else into auto-approve or manual-review queues, polls a database for analyst decisions, and activates accounts. If the identity verification API is down, you'd need retry logic. If the system crashes after the analyst approves but before activation, the customer is approved but never gets access. BSA/AML regulators require a complete audit trail of every KYC decision, including who reviewed high-risk cases and when.

## The Solution

**You just write the KYC risk-check and account-activation workers. Conductor handles the routing between auto-approve and manual review.**

The SWITCH + WAIT pattern is the key here. After the KYC check determines the risk level and whether manual review is needed, a SWITCH routes the workflow: if `needsReview` is true (high-risk), the workflow enters a WAIT task for a compliance analyst's decision; if false (low-risk), it auto-approves via SET_VARIABLE and proceeds directly to activation. Conductor takes care of routing based on risk assessment, holding the workflow durably while a compliance analyst reviews high-risk cases, activating accounts only after approval (automatic or manual), and providing a complete BSA/AML audit trail of every KYC decision. ### What You Write: Workers

KycCheckWorker assesses identity and watchlist risk, while KycActivateWorker enables account access. Neither manages the routing between auto-approval and manual compliance review.

| Worker | Task | What It Does |
|---|---|---|
| **KycCheckWorker** | `kyc_check` | Performs automated KYC risk assessment: checks identity, watchlists, PEP status, and jurisdiction risk, returning a risk level and needsReview flag with any compliance flags |
| *SWITCH task* | `kyc_review_decision` | Routes based on needsReview. "true" sends to a manual WAIT task for compliance analyst review, default auto-approves and proceeds to activation | Built-in Conductor SWITCH.; no worker needed |
| *WAIT task* | `manual_kyc_review` | Pauses for a compliance analyst to review the flags and make an approve/reject decision via `POST /tasks/{taskId}` | Built-in Conductor WAIT.; no worker needed |
| **KycActivateWorker** | `kyc_activate` | Activates the customer account after KYC approval (automatic or manual), enabling login and product access |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
kyc_check
 │
 ▼
SWITCH (kyc_review_decision_ref)
 ├── true: manual_kyc_review
 └── default: kyc_auto_approved
 │
 ▼
kyc_activate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
