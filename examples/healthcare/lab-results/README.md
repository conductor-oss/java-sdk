# Lab Results Processing in Java Using Conductor : Sample Collection, Processing, Analysis, Reporting, and Physician Notification

## The Problem

You need to manage the lifecycle of a lab order from sample collection through physician notification. A lab order comes in with a patient ID, order ID, and test type (CBC, BMP, lipid panel, etc.). The sample must be collected and accessioned with a barcode linking it to the order. The specimen is processed (centrifuged, aliquoted, loaded onto the analyzer). The analyzer runs the test and produces raw values. Those results must be formatted into a report with reference ranges, abnormal flags, and critical value alerts. Finally, the ordering physician must be notified. immediately for critical values, routinely for normal results. A lost sample or unreported critical value can delay diagnosis or endanger the patient.

Without orchestration, you'd build a monolithic LIS (Laboratory Information System) integration that tracks the sample through each stage, polls the analyzer for results, formats the report, and sends notifications. If the analyzer interface is down, you'd need retry logic. If the system crashes after analysis but before reporting, results sit unreported. CAP and CLIA accreditation require a complete chain of custody from sample receipt to result delivery.

## The Solution

**You just write the lab pipeline workers. Sample accessioning, specimen processing, test analysis, report generation, and physician notification. Conductor handles pipeline sequencing, automatic retries when an analyzer interface is temporarily offline, and a complete chain of custody for CAP/CLIA compliance.**

Each stage of the lab pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of processing only after collection, analyzing only after processing, generating the report only after analysis completes, notifying the physician after the report is finalized, and maintaining a complete chain of custody for CAP/CLIA compliance.

### What You Write: Workers

Five workers cover the lab pipeline: CollectSampleWorker accessions specimens, ProcessSampleWorker prepares them for analysis, AnalyzeSampleWorker runs the ordered tests, LabReportWorker generates results with reference ranges, and LabNotifyWorker alerts the ordering physician.

| Worker | Task | What It Does |
|---|---|---|
| **CollectSampleWorker** | `lab_collect_sample` | Accessions the sample with a barcode, records collection time and specimen type |
| **ProcessSampleWorker** | `lab_process` | Processes the specimen. centrifugation, aliquoting, and loading onto the appropriate analyzer |
| **AnalyzeSampleWorker** | `lab_analyze` | Runs the ordered test on the analyzer and produces raw result values |
| **LabReportWorker** | `lab_report` | Generates the lab report with results, reference ranges, abnormal flags, and critical value alerts |
| **LabNotifyWorker** | `lab_notify` | Sends the results to the ordering physician. immediate notification for critical values, routine for normal |

the workflow and compliance logic stay the same.

### The Workflow

```
lab_collect_sample
 │
 ▼
lab_process
 │
 ▼
lab_analyze
 │
 ▼
lab_report
 │
 ▼
lab_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
