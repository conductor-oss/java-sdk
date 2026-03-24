# Property Inspection in Java with Conductor : Schedule, Inspect, Document, and Report

## The Problem

You need to coordinate property inspections for real estate transactions. When a buyer makes an offer contingent on inspection, an inspector must be scheduled, the on-site inspection must cover all systems (roof, foundation, plumbing, electrical, HVAC), findings must be documented with photos and severity ratings, and a formal report must be generated for the buyer, seller, and their agents. Each step depends on the previous one. you can't document findings before the inspection, and the report can't be generated without documentation.

Without orchestration, inspection coordination happens over phone calls and email. The agent schedules the inspector, the inspector emails findings in a Word doc, someone reformats it into a report, and it gets forwarded to the buyer. If the documentation step fails, the report is incomplete. If the inspector's notes are lost, the entire inspection must be repeated. Nobody can track which inspections are in progress, complete, or overdue.

## The Solution

**You just write the scheduling, on-site inspection, documentation, and report generation logic. Conductor handles scheduling retries, assessment sequencing, and inspection audit trails.**

Each inspection step is a simple, independent worker. one schedules the inspector, one records the inspection findings, one organizes documentation (photos, notes, checklists), one generates the final report. Conductor takes care of executing them in order, retrying if the scheduling API is unavailable, and maintaining a permanent record of every inspection from scheduling through report delivery.

### What You Write: Workers

Scheduling, on-site assessment, deficiency documentation, and report generation workers each own one phase of property condition evaluation.

| Worker | Task | What It Does |
|---|---|---|
| **ScheduleWorker** | `pin_schedule` | Books the inspection appointment, coordinating inspector availability with property access |
| **InspectWorker** | `pin_inspect` | Conducts the on-site inspection, evaluating structural, plumbing, electrical, and HVAC systems |
| **DocumentWorker** | `pin_document` | Organizes inspection findings into structured documentation with photos, severity ratings, and notes |
| **ReportWorker** | `pin_report` | Generates the formal inspection report with overall condition assessment and recommended repairs |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
pin_schedule
 │
 ▼
pin_inspect
 │
 ▼
pin_document
 │
 ▼
pin_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
