# Financial Audit in Java with Conductor

Financial audit: define scope, collect evidence, test controls, generate report, remediate. ## The Problem

You need to conduct a financial audit for a business entity. This involves defining the audit scope and objectives, collecting evidence (financial statements, transaction records, supporting documents), testing internal controls for effectiveness, generating the audit report with findings, and tracking remediation of any deficiencies. An audit without proper evidence collection is incomplete; findings without remediation tracking are toothless.

Without orchestration, you'd manage the audit process through spreadsheets and email. manually tracking which evidence has been collected, scheduling control tests, drafting reports in documents, and following up on remediation through calendar reminders.

## The Solution

**You just write the audit workers. Scope definition, evidence collection, control testing, report generation, and remediation tracking. Conductor handles stage sequencing, automatic retries when a data source is unavailable, and timestamped evidence collection for SOX and regulatory compliance.**

Each audit concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (scope, collect, test, report, remediate), retrying if a data source is unavailable, tracking the entire audit lifecycle with timestamps and evidence, and resuming from the last step if the process crashes. ### What You Write: Workers

Four workers cover the audit lifecycle: DefineScopeWorker sets objectives, CollectEvidenceWorker gathers financial records, GenerateReportWorker produces findings and recommendations, and RemediateWorker tracks deficiency resolution.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEvidenceWorker** | `fau_collect_evidence` | Collecting evidence for scope areas |
| **DefineScopeWorker** | `fau_define_scope` | Defines the scope |
| **GenerateReportWorker** | `fau_generate_report` | Generates the audit report with findings, discrepancies, and recommendations |
| **RemediateWorker** | `fau_remediate` | Remediate. Computes and returns remediation plan id, actions created, target completion date |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
fau_define_scope
 │
 ▼
fau_collect_evidence
 │
 ▼
fau_test_controls
 │
 ▼
fau_generate_report
 │
 ▼
fau_remediate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
