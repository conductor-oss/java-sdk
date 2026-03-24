# Clinical Trials in Java Using Conductor: Screening, Consent, Randomization, Monitoring, and Analysis

A promising cardiac drug candidate has 200 patients waiting to enroll. Eligibility screening is a manual chart review that takes three days per patient. Consent forms are mailed, signed, scanned, and uploaded to a shared drive. Randomization happens in a spreadsheet that the biostatistician updates on Tuesdays. The result: it takes six weeks from "patient expressed interest" to "patient is enrolled and randomized," and by then a quarter of them have dropped out or enrolled in a competing trial. When the FDA auditor asks to see the audit trail for participant SUBJ-4401. exactly when they were screened, who obtained consent, how they were randomized, the clinical ops team spends two days assembling it from three disconnected systems. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full trial enrollment pipeline, screening, consent, randomization, monitoring, and analysis, with strict step sequencing and a 21 CFR Part 11-compliant audit trail built in.

## The Problem

You need to manage participants through the lifecycle of a clinical trial. Each potential participant must be screened against the trial's inclusion and exclusion criteria. Those who qualify must provide informed consent before being enrolled. Consented participants are then randomized into treatment or control arms. Throughout the trial, participants must be monitored for adverse events and protocol deviations. At the end, trial data must be analyzed and outcomes reported. Every step requires strict sequencing. You cannot randomize without consent, and you cannot analyze without monitoring data. FDA 21 CFR Part 11 requires a complete, tamper-evident audit trail of every action.

Without orchestration, you'd build a monolithic trial management system that checks eligibility, records consent, calls the randomization service, logs monitoring events, and runs the analysis, all with inline error handling. If the randomization service fails after consent is recorded, you'd need to track where the participant is in the process. Sponsors and the FDA demand full traceability of every participant interaction for compliance audits.

## The Solution

**You just write the trial management workers. Participant screening, consent collection, arm randomization, adverse event monitoring, and outcome analysis. Conductor handles strict step sequencing, automatic retries, and a 21 CFR Part 11-compliant audit trail of every participant interaction.**

Each stage of the trial enrollment pipeline is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of running screening before consent, randomizing only after consent is obtained, triggering monitoring after randomization, analyzing only after monitoring is complete, and maintaining a 21 CFR Part 11-compliant audit trail of every step.

### What You Write: Workers

Five workers manage the trial enrollment pipeline: ScreenWorker checks eligibility, ConsentWorker records informed consent, RandomizeWorker assigns treatment arms, MonitorWorker tracks adverse events, and AnalyzeTrialWorker runs outcome analysis.

| Worker | Task | What It Does |
|---|---|---|
| **ScreenWorker** | `clt_screen` | Evaluates the participant against inclusion/exclusion criteria for the specified trial and condition |
| **ConsentWorker** | `clt_consent` | Records the participant's informed consent with electronic signature and version tracking |
| **RandomizeWorker** | `clt_randomize` | Assigns the participant to a treatment arm using stratified block randomization |
| **MonitorWorker** | `clt_monitor` | Tracks the participant through the trial for adverse events, protocol deviations, and study visits |
| **AnalyzeTrialWorker** | `clt_analyze` | Runs the statistical analysis on collected trial data and generates outcome reports |

### The Workflow

```
clt_screen
 │
 ▼
clt_consent
 │
 ▼
clt_randomize
 │
 ▼
clt_monitor
 │
 ▼
clt_analyze

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
