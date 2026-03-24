# Metric Threshold Alerting with Severity-Based Routing

A metric crosses a threshold but the alert goes to the same Slack channel whether it is a
harmless blip or a production outage. This workflow checks a metric against warning and
critical thresholds, then routes via a SWITCH: critical pages the on-call engineer via
PagerDuty, warning sends to `#ops-alerts` in Slack, and normal values are logged quietly.

## Workflow

```
metricName, currentValue, warningThreshold, criticalThreshold
                         |
                         v
+-------------------+
| th_check_metric   |   compares value to warn/crit thresholds
+-------------------+   outputs severity: "ok" / "warning" / "critical"
         |
         v
    SWITCH on severity
    +-------------------+-------------------+-------------------+
    | critical          | warning           | default (ok)      |
    | th_page_oncall    | th_send_warning   | th_log_ok         |
    | PagerDuty page    | Slack #ops-alerts | logged as healthy |
    | INC-20260308-001  | warned=true       | status: "healthy" |
    +-------------------+-------------------+-------------------+
```

## Workers

**CheckMetric** -- Compares `currentValue` against `warningThreshold` and
`criticalThreshold`. Returns `severity` (`"ok"`, `"warning"`, or `"critical"`), `value`,
and both threshold values.

**PageOncall** -- Fires for critical alerts. Pages `oncallEngineer: "eng-oncall-1"` via
`channel: "pagerduty"` and creates `incidentId: "INC-20260308-001"`.

**SendWarning** -- Fires for warning alerts. Sends to `channel: "slack"`,
`sentTo: "#ops-alerts"`.

**LogOk** -- Fires when the metric is within normal range. Logs `status: "healthy"`.

## Tests

33 unit tests cover metric checking, critical paging, warning alerts, and normal logging.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
