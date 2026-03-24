# Implementing Access Review in Java with Conductor : Entitlement Collection, Anomaly Detection, Certification, and Enforcement

## The Problem

Compliance frameworks (SOX, SOC2, ISO 27001) require periodic access reviews. verifying that every user's permissions are still appropriate. You must collect entitlements from all systems (Active Directory, AWS IAM, SaaS apps), identify anomalies (users with admin access who changed roles, dormant accounts still provisioned), send certification requests to managers, and revoke access for denied items.

Without orchestration, access reviews are quarterly spreadsheet exercises. Someone exports user lists from each system, manually highlights anomalies, emails managers for certification, and hopes they respond before the deadline. Revocation is manual and often forgotten.

## The Solution

**You just write the entitlement queries and revocation calls. Conductor handles the multi-step review sequence, retries when identity providers are unavailable, and a compliance-ready record of every entitlement reviewed and decision made.**

Each review step is an independent worker. entitlement collection, anomaly detection, certification requests, and enforcement. Conductor runs them in sequence with conditional routing: anomalous access gets flagged for extra scrutiny. Every review cycle is tracked with entitlements collected, anomalies found, certifications received, and revocations applied.

### What You Write: Workers

The review pipeline uses CollectEntitlementsWorker to gather permissions across systems, IdentifyAnomaliesWorker to flag excessive or dormant access, RequestCertificationWorker to obtain manager approvals, and EnforceDecisionsWorker to revoke denied items.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEntitlementsWorker** | `ar_collect_entitlements` | Collects all user entitlements across systems for a department (e.g., 45 users, 312 entitlements) |
| **EnforceDecisionsWorker** | `ar_enforce_decisions` | Revokes access grants that were denied during certification (e.g., 5 revocations across 3 systems) |
| **IdentifyAnomaliesWorker** | `ar_identify_anomalies` | Detects excessive access grants and dormant accounts that need review |
| **RequestCertificationWorker** | `ar_request_certification` | Sends certification requests to managers and records their approve/deny decisions |

the workflow logic stays the same.

### The Workflow

```
ar_collect_entitlements
 │
 ▼
ar_identify_anomalies
 │
 ▼
ar_request_certification
 │
 ▼
ar_enforce_decisions

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
