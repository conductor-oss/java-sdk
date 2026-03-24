# Implementing Intrusion Detection in Java with Conductor : Event Analysis, Threat Correlation, Severity Assessment, and Response

## The Problem

You detect a suspicious event. an SSH login from an unusual IP, a port scan, an anomalous database query pattern. You need to analyze the event in context, correlate it with other events and known threat indicators (is this IP on a threat feed?), assess the severity (isolated event vs coordinated attack), and trigger the appropriate response (block IP, isolate host, alert SOC).

Without orchestration, intrusion detection is either a SIEM that generates thousands of uncorrelated alerts or a manual investigation process. Security analysts manually check threat feeds, correlate events across tools, and copy-paste IOCs between systems. Response is delayed because each step depends on the previous one and they're done sequentially by a human.

## The Solution

**You just write the event analysis and threat correlation logic. Conductor handles sequential execution, automatic retries if a threat feed is down, and a complete forensic timeline of every detection.**

Each detection step is an independent worker. event analysis, threat correlation, severity assessment, and automated response. Conductor runs them in sequence: analyze the event, correlate with threat intelligence, assess severity, then trigger the response. Every detection is tracked with full context, event details, correlation results, severity score, and actions taken.

### What You Write: Workers

The detection pipeline chains four focused workers: AnalyzeEventsWorker parses security events, CorrelateThreatsWorker matches against threat feeds, AssessSeverityWorker scores the risk, and RespondWorker triggers automated containment.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeEventsWorker** | `id_analyze_events` | Analyzes security events from a source IP, identifying suspicious patterns like repeated auth failures |
| **AssessSeverityWorker** | `id_assess_severity` | Assesses threat severity (e.g., active brute-force from a known malicious IP) |
| **CorrelateThreatsWorker** | `id_correlate_threats` | Correlates the event IP against threat intelligence feeds for known indicators |
| **RespondWorker** | `id_respond` | Executes automated response actions. blocks the IP and notifies the security team |

the workflow logic stays the same.

### The Workflow

```
id_analyze_events
 │
 ▼
id_correlate_threats
 │
 ▼
id_assess_severity
 │
 ▼
id_respond

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
