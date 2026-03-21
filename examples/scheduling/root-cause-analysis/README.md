# Root Cause Analysis in Java Using Conductor : Issue Detection, Evidence Collection, Analysis, and Root Cause Identification

## The Problem

An incident is happening. high error rate, latency spike, service degradation. You need to find the root cause fast. This requires detecting the specific issue, collecting evidence from multiple sources (logs, metrics, traces, recent deployments), analyzing correlations (did the error rate spike after a deployment? does the latency correlate with CPU usage?), and identifying the root cause. Each step feeds the next, you can't analyze without evidence, and evidence collection depends on knowing what issue to investigate.

Without orchestration, root cause analysis is manual. an engineer opens 5 dashboards, searches logs, checks recent deployments, and pieces together the story. This takes 30-60 minutes per incident. Automated RCA scripts exist but don't coordinate: one collects metrics, another parses logs, but they don't feed results to a common analysis step.

## The Solution

**You just write the evidence collection queries and correlation analysis logic. Conductor handles the detect-collect-analyze-identify sequence, retries when log or metric sources are temporarily unavailable, and a complete record of every RCA session's evidence and conclusions.**

Each RCA step is an independent worker. issue detection, evidence collection, correlation analysis, and root cause identification. Conductor runs them in sequence: detect the issue, collect evidence, analyze correlations, then identify the root cause. Every RCA run is tracked, you can see what evidence was collected, what correlations were found, and what root cause was identified. ### What You Write: Workers

Four workers automate RCA: DetectIssueWorker identifies the incident scope, CollectEvidenceWorker gathers logs/metrics/deployment data, an AnalyzeCorrelationsWorker finds patterns, and IdentifyRootCauseWorker pinpoints the most likely cause with a remediation recommendation.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEvidenceWorker** | `rca_collect_evidence` | Gathers evidence from logs, metrics, and recent deployment changes for the affected services |
| **DetectIssueWorker** | `rca_detect_issue` | Identifies the incident's time window, related services, and impact level from the reported symptom |
| **IdentifyRootCauseWorker** | `rca_identify_root_cause` | Confirms the top candidate root cause based on confidence score and suggests a remediation action |
| **RcaAnalyzeWorker** | `rca_analyze` | Analyzes collected logs and metrics to identify the most likely root cause candidate with a confidence percentage |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
rca_detect_issue
 │
 ▼
rca_collect_evidence
 │
 ▼
rca_analyze
 │
 ▼
rca_identify_root_cause

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
