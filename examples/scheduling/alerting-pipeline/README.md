# Alerting Pipeline in Java Using Conductor : Metric Evaluation, Anomaly Detection, Suppression, and Alert Dispatch

## The Problem

You need to evaluate incoming metrics against alerting rules. When a metric exceeds its threshold, you need to detect whether it's a genuine anomaly (not just noise), suppress duplicate alerts if the same condition was already flagged recently, and send notifications to the appropriate channel. A naive threshold check fires too many alerts; a proper pipeline requires anomaly detection and suppression logic.

Without orchestration, alerting logic is scattered. threshold checks in one script, suppression in another, notification dispatch in a third. Alerts fire for every metric sample above threshold instead of once per incident. Engineers suffer alert fatigue from duplicates and false positives.

## The Solution

**You just write the threshold evaluation and notification dispatch logic. Conductor handles sequential evaluation with conditional routing, retries on notification delivery failures, and a full record of every alert evaluated, suppressed, or dispatched.**

Each alerting concern is an independent worker. rule evaluation, anomaly detection, suppression checking, and alert dispatch. Conductor runs them in sequence, ensuring suppression is checked before any alert is sent. Every alert evaluation is tracked with full context, you can see which metrics triggered, which were suppressed, and which actually fired. ### What You Write: Workers

The alerting pipeline chains DetectAnomalyWorker to flag metric deviations, EvaluateRulesWorker to apply severity-based firing criteria, and then routes to either SendAlertWorker for dispatch or SuppressAlertWorker for duplicate suppression.

| Worker | Task | What It Does |
|---|---|---|
| **DetectAnomalyWorker** | `alt_detect_anomaly` | Compares a metric's current value against its threshold and flags anomalies, returning severity and a deviation score |
| **EvaluateRulesWorker** | `alt_evaluate_rules` | Evaluates alerting rules based on severity. decides whether to fire or suppress the alert and selects the notification channel |
| **SendAlertWorker** | `alt_send_alert` | Dispatches the alert to the selected channel (e.g., PagerDuty) and returns an alert ID for tracking |
| **SuppressAlertWorker** | `alt_suppress_alert` | Suppresses alerts that don't meet firing criteria, logging the suppression reason for audit |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
alt_detect_anomaly
 │
 ▼
alt_evaluate_rules
 │
 ▼
SWITCH (alt_switch_ref)
 ├── fire: alt_send_alert
 └── default: alt_suppress_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
