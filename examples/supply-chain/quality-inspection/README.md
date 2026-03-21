# Quality Inspection in Java with Conductor : Sampling, Testing, Pass/Fail Routing, and Results Recording

## The Problem

You need to inspect production batches before they ship. A sample must be pulled from the batch according to AQL (Acceptable Quality Level) sampling tables. The sample undergoes standardized tests. dimensional measurements, material composition, functional testing. Based on results, the batch is either accepted for shipment (pass) or quarantined for rework/scrap (fail). Regardless of outcome, the inspection results must be recorded for traceability, regulatory compliance (ISO 9001, FDA 21 CFR Part 820), and supplier quality scorecards.

Without orchestration, inspectors fill out paper forms, test results live in a disconnected lab system, and pass/fail decisions are communicated verbally. If a batch fails but the fail handler doesn't trigger. the batch ships anyway. When a customer reports a defect, tracing back to the inspection record requires searching through filing cabinets. There is no automated linkage between test results and the disposition decision.

## The Solution

**You just write the inspection workers. Sampling, testing, pass/fail routing, and results recording. Conductor handles SWITCH-based pass/fail routing, test retries, and timestamped inspection records for ISO 9001 compliance.**

Each step of the quality inspection process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so samples are pulled before testing, test results drive the pass/fail routing decision (via a SWITCH task), and results are recorded regardless of which path was taken. If the test worker fails mid-measurement, Conductor retries without re-pulling the sample. Every sample selection, test measurement, disposition decision, and recording is tracked with timestamps for full batch traceability.

### What You Write: Workers

Four workers drive the inspection process: SampleWorker pulls items per AQL tables and runs tests, HandlePassWorker approves the batch, HandleFailWorker quarantines it, and RecordWorker logs results for traceability.

| Worker | Task | What It Does |
|---|---|---|
| **HandleFailWorker** | `qi_handle_fail` | Quarantines the batch for rework or scrap when inspection fails. |
| **HandlePassWorker** | `qi_handle_pass` | Approves the batch for shipment when inspection passes. |
| **RecordWorker** | `qi_record` | Records inspection results for traceability, regulatory compliance, and supplier scorecards. |
| **SampleWorker** | `qi_sample` | Pulls samples from the production batch according to AQL sampling tables and runs standardized tests. |

### The Workflow

```
qi_sample
 │
 ▼
qi_test
 │
 ▼
SWITCH (qi_switch_ref)
 ├── pass: qi_handle_pass
 └── default: qi_handle_fail
 │
 ▼
qi_record

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
