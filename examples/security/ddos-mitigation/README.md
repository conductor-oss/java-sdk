# Implementing DDoS Mitigation in Java with Conductor : Traffic Detection, Attack Classification, Mitigation, and Service Verification

## The Problem

Your service is under a DDoS attack. traffic volume is spiking abnormally. You need to detect the anomaly, classify the attack type (SYN flood vs HTTP flood vs DNS amplification, each requires different mitigation), apply the right countermeasures (rate limiting, traffic scrubbing, blackholing), and verify that legitimate traffic can still reach the service after mitigation is applied.

Without orchestration, DDoS response is a runbook executed by an on-call engineer under pressure. They log into the CDN console, enable rate limiting, check if the service is still up, and adjust settings manually. Each attack is handled slightly differently, response time is measured in minutes, and there's no audit trail of what mitigation was applied and when.

## The Solution

**You just write the traffic detection and mitigation rules. Conductor handles rapid sequential execution under attack conditions, retries on CDN API failures, and a timestamped record of every mitigation action and service availability check.**

Each mitigation step is an independent worker. traffic detection, attack classification, mitigation application, and service verification. Conductor runs them in sequence: detect the anomaly, classify the attack, apply mitigation, then verify the service. Every mitigation action is tracked with traffic metrics, attack classification, and service availability status.

### What You Write: Workers

The mitigation chain runs DetectWorker to spot traffic anomalies, ClassifyWorker to identify the attack vector (SYN flood, HTTP flood, DNS amplification), MitigateWorker to activate rate limiting and scrubbing, and VerifyServiceWorker to confirm legitimate traffic is flowing again.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `ddos_classify` | Classifies the attack type (e.g., Layer 7 HTTP flood from botnet) |
| **DetectWorker** | `ddos_detect` | Detects anomalous traffic spikes by comparing current volume against baseline thresholds |
| **MitigateWorker** | `ddos_mitigate` | Activates mitigation controls. rate limiting, challenge pages, and IP blocking |
| **VerifyServiceWorker** | `ddos_verify_service` | Verifies service is restored. confirms latency is normal and legitimate traffic is flowing |

the workflow logic stays the same.

### The Workflow

```
ddos_detect
 │
 ▼
ddos_classify
 │
 ▼
ddos_mitigate
 │
 ▼
ddos_verify_service

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
