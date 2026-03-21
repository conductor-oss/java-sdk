# Compliance Nonprofit in Java with Conductor

## The Problem

Your nonprofit's fiscal year just closed, and you need to run the annual compliance review before filing deadlines. The compliance team must audit the organization's financials and governance, verify that all required filings (Form 990, state registration, annual report) are current, check IRS, state, and donor compliance requirements, generate a compliance report with findings and risk scores, and submit the final compliance package to regulators. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the compliance checking, documentation gathering, review, and regulatory filing logic. Conductor handles documentation retries, assessment sequencing, and compliance audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Regulation identification, documentation review, gap assessment, and reporting workers each handle one aspect of nonprofit regulatory compliance.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `cnp_audit` | Audits the organization's financials, governance, and program expense ratio for the fiscal year |
| **CheckRequirementsWorker** | `cnp_check_requirements` | Validates IRS, state, and donor compliance requirements, returning an overall compliance status |
| **ReportWorker** | `cnp_report` | Generates the annual compliance report with findings count, overall status, and recommended actions |
| **SubmitWorker** | `cnp_submit` | Submits the compliance package for the organization's EIN and returns a confirmation ID |
| **VerifyFilingsWorker** | `cnp_verify_filings` | Verifies that Form 990, state registration, and annual report filings are current for the EIN |

### The Workflow

```
cnp_audit
 │
 ▼
cnp_verify_filings
 │
 ▼
cnp_check_requirements
 │
 ▼
cnp_report
 │
 ▼
cnp_submit

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
