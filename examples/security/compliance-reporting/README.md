# Implementing Compliance Reporting in Java with Conductor : Evidence Collection, Control Mapping, Gap Assessment, and Report Generation

## The Problem

You need to produce compliance reports that demonstrate your organization meets the requirements of a regulatory framework. This means gathering hundreds of evidence items. access review logs, network diagrams, encryption configurations, incident response records, change management tickets, and mapping each one to the specific control objective it satisfies (e.g., SOC 2 CC6.1 for logical access, ISO 27001 A.12.3 for backup verification). After mapping, you must identify gaps where a control has insufficient or missing evidence, so remediation can happen before the audit. The final report must be structured for auditor consumption, with clear traceability from each control to its supporting evidence.

Without orchestration, compliance reporting is a quarterly fire drill. Someone manually collects screenshots and exports from a dozen tools (AWS Config, Jira, Confluence, Slack, HR systems), dumps them into a shared drive, and tries to match them to a spreadsheet of control objectives. When evidence is missing, nobody notices until the auditor asks for it. The mapping is done in spreadsheets that drift out of sync with the actual controls framework. Different team members collect evidence differently, so the report quality varies wildly. And when the auditor requests a re-run with updated evidence, the entire manual process starts over.

## The Solution

**You just write the evidence collection and control mapping logic. Conductor handles the evidence-to-report pipeline, retries when source systems are temporarily unavailable, and a proof chain showing when evidence was collected and how controls were mapped.**

Each compliance reporting step is a simple, independent worker. one collects evidence artifacts from your systems for the specified framework and reporting period, one maps each evidence item to the control objectives it satisfies, one identifies gaps where controls lack sufficient evidence, one generates the structured compliance report for auditor review. Conductor takes care of executing them in strict order so no report is generated without a complete gap assessment, retrying if an evidence source is temporarily unavailable, and maintaining a complete audit trail that proves when evidence was collected, how controls were mapped, and what gaps were identified for every reporting cycle.

### What You Write: Workers

The reporting pipeline uses CollectEvidenceWorker to gather artifacts from connected systems, MapControlsWorker to link evidence to control objectives, AssessGapsWorker to identify insufficient coverage, and GenerateReportWorker to produce the auditor-ready package.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEvidenceWorker** | `cr_collect_evidence` | Gathers evidence artifacts (access logs, config snapshots, policy documents, change tickets) from connected systems for the specified compliance framework and reporting period |
| **MapControlsWorker** | `cr_map_controls` | Maps each collected evidence item to the control objectives it satisfies (e.g., SOC 2 CC6.1, ISO 27001 A.12.3) and tracks coverage across the full controls matrix |
| **AssessGapsWorker** | `cr_assess_gaps` | Identifies controls that lack sufficient evidence. missing artifacts, expired certifications, stale configurations, and flags them for remediation before audit |
| **GenerateReportWorker** | `cr_generate_report` | Produces the structured compliance report with control-to-evidence traceability, gap summaries, and remediation status for auditor review |

the workflow logic stays the same.

### The Workflow

```
cr_collect_evidence
 │
 ▼
cr_map_controls
 │
 ▼
cr_assess_gaps
 │
 ▼
cr_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
