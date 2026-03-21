# Compliance Review in Java with Conductor

## The Problem

A regulatory audit is approaching. You need to identify the applicable compliance requirements (e.g., 45 controls), assess your organization's current posture against each one, perform a gap analysis to find the 7 unmet controls (like missing encryption), and create a remediation plan to close those gaps before the deadline. Failing to identify a critical gap can result in regulatory fines, consent orders, or loss of operating licenses.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the requirement identification, gap analysis, remediation planning, and compliance certification logic. Conductor handles assessment retries, gap analysis sequencing, and compliance audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Regulatory identification, compliance assessment, gap analysis, and remediation planning workers each tackle one phase of the compliance review lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `cmr_identify` | Identifies applicable regulatory requirements (45 controls) for the entity under review and maps them to the relevant compliance framework |
| **AssessWorker** | `cmr_assess` | Evaluates current compliance posture, scoring 84 out of 100 with 38 controls met and 7 not met, and flags critical gaps like missing encryption |
| **GapAnalysisWorker** | `cmr_gap_analysis` | Analyzes the 7 unmet controls in detail, categorizing each gap by severity (e.g., encryption flagged as critical) and producing a prioritized gap report |
| **RemediateWorker** | `cmr_remediate` | Creates a remediation plan (REM-695) with specific action items to close each identified gap before the audit deadline |

### The Workflow

```
cmr_identify
 │
 ▼
cmr_assess
 │
 ▼
cmr_gap_analysis
 │
 ▼
cmr_remediate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
