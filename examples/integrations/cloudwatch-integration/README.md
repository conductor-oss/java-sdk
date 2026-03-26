# Cloudwatch Integration

Orchestrates cloudwatch integration through a multi-stage Conductor workflow.

**Input:** `namespace`, `metricName`, `value`, `threshold`, `notifyEmail` | **Timeout:** 60s

## Pipeline

```
cw_put_metric
    │
cw_create_alarm
    │
cw_check_status
    │
cw_notify
```

## Workers

**CheckStatusWorker** (`cw_check_status`): Checks CloudWatch alarm status.

```java
String state = currentVal > thresholdVal ? "ALARM" : "OK";
```

Reads `alarmName`, `currentValue`, `threshold`. Outputs `state`, `stateReason`.

**CreateAlarmWorker** (`cw_create_alarm`): Creates a CloudWatch alarm.

Reads `metricName`, `threshold`. Outputs `alarmName`, `alarmArn`.

**CwNotifyWorker** (`cw_notify`): Sends a CloudWatch alarm notification.

Reads `alarmName`, `alarmState`, `email`. Outputs `notified`, `sentAt`.

**PutMetricWorker** (`cw_put_metric`): Publishes a metric to CloudWatch.

Reads `metricName`, `namespace`, `value`. Outputs `published`, `timestamp`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
