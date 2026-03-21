# Cloudwatch Integration in Java Using Conductor

## Setting Up Metric Monitoring with Alerting

CloudWatch monitoring involves a chain of dependent steps: publish a metric data point, create an alarm that watches that metric against a threshold, evaluate whether the current value triggers the alarm, and send a notification if it does. Each step depends on the previous one. you cannot create an alarm without a metric namespace, and you cannot notify without knowing the alarm state.

Without orchestration, you would chain CloudWatch API calls manually, manage alarm ARNs between steps, and build custom notification logic. Conductor sequences the four steps and routes metric names, alarm ARNs, and alarm states between them automatically.

## The Solution

**You just write the CloudWatch workers. Metric publishing, alarm creation, status checking, and threshold-based notification. Conductor handles metric-to-notification sequencing, CloudWatch API retries, and alarm state routing between workers.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers build the monitoring pipeline: PutMetricWorker publishes data points, CreateAlarmWorker sets thresholds, CheckStatusWorker evaluates alarm state, and CwNotifyWorker dispatches alerts when thresholds breach.

| Worker | Task | What It Does |
|---|---|---|
| **PutMetricWorker** | `cw_put_metric` | Publishes a metric data point to CloudWatch. writes the value to the specified namespace and metric name, returns published=true |
| **CreateAlarmWorker** | `cw_create_alarm` | Creates a CloudWatch alarm. sets up monitoring for the metric against the specified threshold and returns the alarmName and alarm ARN |
| **CheckStatusWorker** | `cw_check_status` | Checks the alarm status. evaluates the current metric value against the alarm threshold and returns the alarm state (OK or ALARM) |
| **CwNotifyWorker** | `cw_notify` | Sends an alarm notification. dispatches an alert email to the notifyEmail address with the alarm name and state (triggered only when the alarm is in ALARM state) |

the workflow orchestration and error handling stay the same.

### The Workflow

```
cw_put_metric
 │
 ▼
cw_create_alarm
 │
 ▼
cw_check_status
 │
 ▼
cw_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
