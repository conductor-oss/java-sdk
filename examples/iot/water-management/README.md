# Water Management in Java with Conductor : Level Monitoring, Quality Analysis, Leak Detection, and Alerting

## Why Water Infrastructure Monitoring Needs Orchestration

Managing a water distribution network requires a monitoring pipeline where each check feeds into the next. You read water levels across the zone. reservoir capacity, tank fill percentages, distribution pressure. You analyze water quality at multiple points in the network to detect contamination, pH drift, or chlorine depletion. You run leak detection algorithms that correlate pressure drops and flow rate anomalies to identify pipe segments losing water. If any check finds an issue, low levels, quality violations, or a high-confidence leak, you trigger alerts to the operations team.

Each stage depends on context from earlier stages. Leak detection needs pressure and flow data alongside level readings. Alerting needs to know whether the issue is a quality violation (which requires a different response team than a leak). Without orchestration, you'd build a monolithic SCADA script that mixes sensor polling, water chemistry analysis, hydraulic modeling, and notification dispatch. making it impossible to upgrade your leak detection algorithm without risking the quality analysis logic, or to audit which sensor reading triggered which alert.

## How This Workflow Solves It

**You just write the water infrastructure workers. Level monitoring, quality analysis, leak detection, and operations alerting. Conductor handles level-to-alert sequencing, remote sensor retries, and audit trails linking every alert to its triggering readings.**

Each monitoring concern is an independent worker. monitor levels, analyze quality, detect leaks, trigger alerts. Conductor sequences them, passes sensor readings and quality results between stages, retries if a remote sensor is temporarily unreachable, and maintains an audit trail linking every alert to the specific readings that triggered it.

### What You Write: Workers

Four workers monitor the water network: MonitorLevelsWorker reads reservoir and distribution pressure, AnalyzeQualityWorker checks pH, turbidity, and chlorine levels, DetectLeaksWorker correlates pressure drops with flow anomalies, and AlertWorker notifies operations when issues are found.

| Worker | Task | What It Does |
|---|---|---|
| **AlertWorker** | `wtr_alert` | Triggers alerts to operations when quality violations or leaks are detected. |
| **AnalyzeQualityWorker** | `wtr_analyze_quality` | Analyzes water quality parameters (pH, turbidity, chlorine, contaminants) against safety standards. |
| **DetectLeaksWorker** | `wtr_detect_leaks` | Detects leaks by correlating pressure drops and flow rate anomalies across the distribution network. |
| **MonitorLevelsWorker** | `wtr_monitor_levels` | Reads water levels, flow rates, and pressure from reservoir and distribution sensors. |

the workflow and alerting logic stay the same.

### The Workflow

```
wtr_monitor_levels
 │
 ▼
wtr_analyze_quality
 │
 ▼
wtr_detect_leaks
 │
 ▼
wtr_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
