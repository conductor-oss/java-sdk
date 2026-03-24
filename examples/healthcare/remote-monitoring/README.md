# Remote Patient Monitoring in Java Using Conductor : Vital Signs Collection, Trend Analysis, Alert Routing, and Clinical Action

## The Problem

You need to monitor patients remotely using connected medical devices. Vital signs. blood pressure, heart rate, blood glucose, weight, oxygen saturation, are transmitted from the patient's home device. Those readings must be analyzed against the patient's individual baseline and clinical thresholds to detect concerning trends (rising blood pressure over 3 days, weight gain suggesting fluid retention in CHF patients, glucose spikes in diabetics). Based on the analysis, the system must route to different clinical actions, log the reading as normal, or trigger an alert that notifies the care team and may schedule an intervention. A missed alert on a deteriorating trend can result in an avoidable hospitalization or emergency visit.

Without orchestration, you'd build a monolithic monitoring service that polls device data, runs the trend analysis, branches with if/else into normal or alert paths, and sends notifications. If the device data platform is temporarily unavailable, you'd need retry logic. If the system crashes after detecting an alert but before notifying the care team, the patient's deterioration goes unaddressed. CMS RPM billing codes (99453, 99454, 99457) require documentation of every monitoring interaction.

## The Solution

**You just write the RPM workers. Vital signs collection, trend analysis, and conditional routing to normal logging or clinical alert actions. Conductor handles conditional SWITCH routing between normal and alert paths, automatic retries when the device platform is down, and complete monitoring records for RPM billing.**

Each stage of the monitoring cycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting vitals before analyzing trends, routing to the correct clinical action (normal or alert) via SWITCH based on trend analysis, retrying if the device platform is temporarily unavailable, and maintaining a complete record of every monitoring cycle for RPM billing and clinical documentation.

### What You Write: Workers

Four workers form the RPM cycle: CollectVitalsWorker retrieves device readings, AnalyzeTrendsWorker evaluates against baselines, NormalActionWorker logs compliant readings, and AlertActionWorker triggers clinical notifications when trends are concerning.

| Worker | Task | What It Does |
|---|---|---|
| **CollectVitalsWorker** | `rpm_collect_vitals` | Retrieves the latest vital signs (BP, HR, SpO2, glucose, weight) from the patient's connected device |
| **AnalyzeTrendsWorker** | `rpm_analyze_trends` | Analyzes readings against the patient's baseline and clinical thresholds, returns "normal" or "alert" |
| **NormalActionWorker** | `rpm_normal_action` | Logs the reading as within normal limits and updates the patient's monitoring dashboard |
| **AlertActionWorker** | `rpm_alert_action` | Triggers a clinical alert. notifies the care team, flags for intervention, and may schedule a follow-up visit |

the workflow and compliance logic stay the same.

### The Workflow

```
rpm_collect_vitals
 │
 ▼
rpm_analyze_trends
 │
 ▼
SWITCH (rpm_switch_ref)
 ├── normal: rpm_normal_action
 ├── alert: rpm_alert_action
 └── default: rpm_alert_action

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
