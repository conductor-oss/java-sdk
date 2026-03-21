# Threshold Alerting in Java with Conductor : Check Metric, Route by Severity via SWITCH

Automates threshold-based alerting using [Conductor](https://github.com/conductor-oss/conductor). This workflow checks a metric value against warning and critical thresholds, then routes to the appropriate action: logging for normal values, sending a Slack warning for elevated values, or paging the on-call engineer for critical breaches.

## The Right Alert to the Right Person

CPU usage is at 87%. Is that a problem? It depends on the thresholds. Below 70% is fine: just log it. Between 70% and 90% is a warning, send a Slack message so the team is aware. Above 90% is critical, page the on-call engineer immediately. Each severity level needs a different response, and the check needs to happen reliably every time, with the right routing for each outcome.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the threshold checks and notification handlers. Conductor handles severity-based routing, delivery retries, and alerting decision tracking.**

`CheckMetricWorker` evaluates the current metric value against configured warning and critical thresholds, classifying the status. Conductor's routing directs the workflow to the appropriate handler: `LogOkWorker` records the metric value for normal readings. `SendWarningWorker` sends a notification to the team channel for warning-level breaches. `PageOncallWorker` pages the on-call engineer via PagerDuty or Opsgenie for critical breaches. Conductor records every alert decision. metric value, threshold comparison, and action taken, for alerting analytics and threshold tuning.

### What You Write: Workers

Four workers handle threshold alerting. Checking the metric value, then routing to the appropriate action: logging, warning via Slack, or paging the on-call engineer.

| Worker | Task | What It Does |
|---|---|---|
| **CheckMetric** | `th_check_metric` | Checks a metric value against warning and critical thresholds, determining the severity level. |
| **LogOk** | `th_log_ok` | Logs that the metric is within normal range. |
| **PageOncall** | `th_page_oncall` | Pages the on-call engineer for critical alerts. |
| **SendWarning** | `th_send_warning` | Sends a warning alert to Slack. |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> CheckMetric -> LogOk -> PageOncall -> SendWarning -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
