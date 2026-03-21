# Beneficiary Tracking in Java with Conductor

## The Problem

A community health nonprofit enrolls a new beneficiary into a housing-assistance program. The intake team needs to register the person's demographics and location, assess their needs across housing, food security, education, and health, match them with appropriate services like food assistance, health screenings, and tutoring, monitor their progress over time, and produce a case report summarizing outcomes. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the beneficiary enrollment, service delivery, progress tracking, and outcome reporting logic. Conductor handles service delivery retries, progress monitoring, and beneficiary audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Registration, needs assessment, service provision, and monitoring workers each track one dimension of a beneficiary's journey through the program.

| Worker | Task | What It Does |
|---|---|---|
| **AssessNeedsWorker** | `btr_assess_needs` | Evaluates the beneficiary across housing, food security, education, and health dimensions, returning a needs map (e.g., housing: stable, food: insecure) |
| **MonitorWorker** | `btr_monitor` | Tracks ongoing progress across food security, health status, and academic outcomes for the enrolled beneficiary |
| **ProvideServicesWorker** | `btr_provide_services` | Enrolls the beneficiary in matched services (food assistance, health screening, tutoring) and records completed sessions |
| **RegisterWorker** | `btr_register` | Registers the beneficiary with their name and location, assigning a unique beneficiary ID |
| **ReportWorker** | `btr_report` | Generates a case report summarizing the beneficiary's services received and outcome status |

### The Workflow

```
btr_register
 │
 ▼
btr_assess_needs
 │
 ▼
btr_provide_services
 │
 ▼
btr_monitor
 │
 ▼
btr_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
