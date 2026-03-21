# Wearable Health Data Pipeline in Java with Conductor : Vitals Collection, Processing, Anomaly Detection, and Notifications

## Why Wearable Health Pipelines Need Orchestration

Processing health data from wearable devices involves a pipeline where each stage builds on the previous one. You sync raw vitals from the wearable. heart rate samples, blood oxygen readings, accelerometer data, sleep stages. You process those raw readings into meaningful metrics: average heart rate over the collection window, resting vs: active heart rate zones, step counts normalized to the user's baseline. You run anomaly detection against the processed data to flag irregular patterns, sustained elevated heart rate without activity, SpO2 drops below safe thresholds, abnormal sleep fragmentation. If anomalies are detected, you notify the user, their healthcare provider, or emergency contacts depending on severity.

Each stage depends on clean data from the previous one. Anomaly detection on raw, unprocessed samples would produce false positives from sensor noise and motion artifacts. Notifications without anomaly context would be meaningless. Without orchestration, you'd build a monolithic health processor that mixes BLE data sync, signal processing, statistical analysis, and push notification logic. making it impossible to upgrade your anomaly detection model without risking the data processing pipeline.

## How This Workflow Solves It

**You just write the wearable health workers. Vitals collection, data processing, anomaly detection, and caregiver notification. Conductor handles vitals-to-notification sequencing, device sync retries, and a complete audit trail for clinical review of every reading and anomaly.**

Each pipeline stage is an independent worker. collect vitals, process data, detect anomalies, notify. Conductor sequences them, passes raw readings through processing into anomaly detection and notification, retries if a device sync times out, and maintains a complete audit trail of every vital sign reading, derived metric, and anomaly alert for clinical review.

### What You Write: Workers

Four workers process wearable health data: CollectVitalsWorker syncs heart rate, SpO2, and step counts from devices, ProcessDataWorker derives meaningful metrics and baselines, DetectAnomaliesWorker flags irregular vital sign patterns, and NotifyWorker alerts users or caregivers based on severity.

| Worker | Task | What It Does |
|---|---|---|
| **CollectVitalsWorker** | `wer_collect_vitals` | Syncs raw vital signs from the wearable device. heart rate, SpO2, step count, and temperature. |
| **DetectAnomaliesWorker** | `wer_detect_anomalies` | Flags irregular patterns. sustained elevated heart rate, abnormal SpO2 drops, or unusual readings. |
| **NotifyWorker** | `wer_notify` | Sends health anomaly notifications to the user, caregiver, or healthcare provider based on severity. |
| **ProcessDataWorker** | `wer_process_data` | Processes raw readings into derived metrics. average heart rate, activity summaries, and baseline comparisons. |

the workflow and alerting logic stay the same.

### The Workflow

```
wer_collect_vitals
 │
 ▼
wer_process_data
 │
 ▼
wer_detect_anomalies
 │
 ▼
wer_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
