# Inspection Workflow in Java with Conductor

Conducts a government property inspection: scheduling the visit, performing the on-site assessment, documenting findings, and recording pass or fail via a SWITCH task with code violations cited.

## The Problem

You need to conduct a government property inspection (building code, fire safety, health, environmental). The inspection is scheduled, the inspector visits the property and conducts the assessment, findings are documented with photos and notes, and the result is recorded as pass or fail with specific code violations cited. Passing a property without thorough inspection creates safety risks; failing without documented evidence invites legal challenges.

Without orchestration, you'd manage inspections with paper checklists, phone-scheduled appointments, handwritten notes, and manual data entry back at the office. losing inspector notes between field and office, missing scheduled inspections, and struggling to produce evidence when a building owner contests a violation.

## The Solution

**You just write the scheduling, on-site assessment, findings documentation, and pass/fail determination logic. Conductor handles scheduling retries, findings routing, and inspection audit trails.**

Each inspection concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (schedule, inspect, document, record pass/fail), routing via a SWITCH task based on the inspection result, tracking every inspection with timestamped evidence, and resuming from the last step if the process crashes.

### What You Write: Workers

Scheduling, on-site inspection, findings recording, and compliance determination workers each own one step of the regulatory inspection process.

| Worker | Task | What It Does |
|---|---|---|
| **DocumentWorker** | `inw_document` | Documents inspection findings (structural, electrical, plumbing) with pass/fail determinations |
| **InspectWorker** | `inw_inspect` | Performs the on-site property inspection and records findings for structural, electrical, and plumbing systems |
| **RecordFailWorker** | `inw_record_fail` | Records a failed inspection result with specific code violations and re-inspection deadline |
| **RecordPassWorker** | `inw_record_pass` | Records a passed inspection result and issues a compliance certificate for the property |
| **ScheduleWorker** | `inw_schedule` | Schedules the inspection for the property, assigning a date and inspector |

### The Workflow

```
inw_schedule
 │
 ▼
inw_inspect
 │
 ▼
inw_document
 │
 ▼
SWITCH (inw_switch_ref)
 ├── pass: inw_record_pass
 ├── fail: inw_record_fail

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
