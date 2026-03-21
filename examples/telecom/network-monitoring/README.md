# Network Monitoring in Java Using Conductor

## Why Network Monitoring Needs Orchestration

Monitoring a telecom network segment requires a closed-loop process from detection through resolution. You poll performance metrics (latency, jitter, packet loss, throughput) from the network segment's switches and routers. You analyze those metrics to detect issues. threshold breaches, trending degradation, or outright failures. You diagnose the root cause by correlating the detected issues with topology, recent changes, and known failure patterns. You execute the repair, rerouting traffic, resetting interfaces, or rolling back a bad configuration. Finally, you verify the segment is healthy by re-polling metrics and confirming the issue is resolved.

If a repair succeeds but verification fails, the network may appear fixed while a deeper issue persists. If detection finds multiple issues but diagnosis only addresses one, the remaining issues go unresolved. Without orchestration, you'd build a monitoring loop that mixes SNMP polling, alarm correlation, and CLI-based remediation into a single script. making it impossible to swap NMS vendors, test diagnosis rules independently, or audit which repair action resolved which alarm.

## The Solution

**You just write the metrics polling, issue detection, root cause diagnosis, automated repair, and health verification logic. Conductor handles metric collection retries, alert routing, and monitoring audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Metric collection, threshold evaluation, alert generation, and incident creation workers each own one layer of network health surveillance.

| Worker | Task | What It Does |
|---|---|---|
| **DetectIssuesWorker** | `nmn_detect_issues` | Analyzes collected metrics to detect threshold breaches, degradation trends, and link failures. |
| **DiagnoseWorker** | `nmn_diagnose` | Diagnoses root causes by correlating detected issues with network topology and recent changes. |
| **MonitorWorker** | `nmn_monitor` | Polls performance metrics (latency, jitter, packet loss, throughput) from a network segment. |
| **RepairWorker** | `nmn_repair` | Executes automated repairs. rerouting traffic, resetting interfaces, or rolling back configurations. |
| **VerifyWorker** | `nmn_verify` | Re-polls the network segment after repair to confirm the issue is resolved and metrics are healthy. |

### The Workflow

```
nmn_monitor
 │
 ▼
nmn_detect_issues
 │
 ▼
nmn_diagnose
 │
 ▼
nmn_repair
 │
 ▼
nmn_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
