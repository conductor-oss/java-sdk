# Air Quality in Java with Conductor

## Why Air Quality Monitoring Needs Orchestration

Monitoring air quality requires a pipeline that collects pollutant data, evaluates it against standards, and takes different actions depending on the result. You collect readings from a monitoring station. PM2.5, PM10, ozone, and CO concentrations for a given region. You check those readings against air quality standards to compute an AQI score and categorize conditions as good, moderate, or poor. Based on the category, you route to entirely different response handlers: log a routine checkpoint for good air, issue a sensitive-groups advisory for moderate conditions, or broadcast a public health warning for poor air quality.

This is a natural fit for conditional routing. The same set of readings can lead to routine logging, targeted advisories, or emergency health warnings. each with different notification recipients and follow-up actions. Without orchestration, you'd build a monolithic air quality processor that mixes sensor polling, AQI calculation, and multi-tier notification dispatch in one class, using if/else chains to decide which alert to send. Adding new response tiers (hazardous, very unhealthy) would require modifying the core processor.

## The Solution

**You just write the air quality workers. Pollutant collection, AQI evaluation, and tier-specific response handlers. Conductor handles AQI-based conditional routing, sensor polling retries, and complete records linking each reading to its response action.**

Each worker handles one IoT operation. data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Five workers monitor air quality: CollectReadingsWorker gathers pollutant concentrations, CheckStandardsWorker computes AQI scores, and three response workers. ActionGoodWorker, ActionModerateWorker, and ActionPoorWorker. Handle tier-specific actions via SWITCH routing.

| Worker | Task | What It Does |
|---|---|---|
| **ActionGoodWorker** | `aq_action_good` | Logs a routine checkpoint when AQI indicates good air quality. |
| **ActionModerateWorker** | `aq_action_moderate` | Issues a health advisory for sensitive groups when AQI is moderate. |
| **ActionPoorWorker** | `aq_action_poor` | Broadcasts a public health warning when AQI indicates poor air quality. |
| **CheckStandardsWorker** | `aq_check_standards` | Evaluates PM2.5, PM10, ozone, and CO concentrations against air quality standards to compute an AQI score and category. |
| **CollectReadingsWorker** | `aq_collect_readings` | Collects pollutant readings (PM2.5, PM10, ozone, CO) from a monitoring station. |

the workflow and alerting logic stay the same.

### The Workflow

```
aq_collect_readings
 │
 ▼
aq_check_standards
 │
 ▼
SWITCH (aq_switch_ref)
 ├── good: aq_action_good
 ├── moderate: aq_action_moderate
 ├── poor: aq_action_poor

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
