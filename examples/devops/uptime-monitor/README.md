# Uptime Monitoring with Dynamic Endpoint Checks and Multi-Channel Alerting

Your status page says "All Systems Operational" while three endpoints are returning 500s.
This workflow dynamically creates a health check task for each configured endpoint using
FORK_JOIN_DYNAMIC, aggregates the results (healthy/degraded/down), then routes through a
SWITCH: healthy endpoints get recorded, failures trigger Slack/email/SMS alerts plus a
status page update, and critical failures escalate to page the on-call engineer.

## Workflow

```
endpoints, notificationChannels, escalationThreshold
                      |
                      v
+---------------------+
| uptime_prepare_     |   builds dynamic task list from endpoints
| checks              |
+---------------------+
          |
          v
 FORK_JOIN_DYNAMIC: uptime_check_endpoint (one per endpoint)
   - real DNS resolution via InetAddress
   - HTTP(S) connection with 5s timeout
   - measures responseTime, statusCode
          |
          v
+---------------------------+
| uptime_aggregate_results  |   totalEndpoints / healthy / degraded / down
+---------------------------+
          |
          v
     SWITCH on has_failures
     +--true---------------------------+--false--------------------+
     | uptime_update_status_page       | uptime_record_healthy    |
     | uptime_send_slack_alert         | writes health record     |
     | uptime_send_email_alert         | to /tmp/uptime/          |
     | uptime_send_sms_alert           +---------------------------+
     | uptime_check_escalation         |
     |    +--SWITCH on shouldEscalate--+
     |    | true: uptime_page_oncall   |
     |    | false: (skip)              |
     +----------------------------------+
          |
          v
+----------------------+
| uptime_store_metrics |   stores data points with 90d retention
+----------------------+
```

## Workers

**PrepareChecks** -- Builds `dynamicTasks` and `dynamicTasksInput` from the endpoint list
for the FORK_JOIN_DYNAMIC.

**CheckEndpoint** -- Performs real DNS resolution via `InetAddress`, opens an HTTP(S)
connection with 5-second timeout, and measures `responseTime` and `statusCode`.

**AggregateResults** -- Counts `totalEndpoints`, `healthy`, `degraded`, and `down`.
Lists failures with their names, URLs, and failed checks.

**UpdateStatusPage** -- Writes component statuses to `/tmp/uptime/status-page.json`.

**SendSlackAlert / SendEmailAlert / SendSmsAlert** -- Multi-channel alerting: Slack webhook
(real HTTP POST, falls back to console), email JSON to `/tmp/uptime/email-alerts/`, SMS
records to `/tmp/uptime/sms-alerts/` with per-phone SIDs.

**CheckEscalation** -- Compares `failureCount` to `escalationThreshold`. Returns
`shouldEscalate` boolean. **PageOncall** -- Creates an incident record in
`/tmp/uptime/pages/` with a UUID-based `incidentId` and assigned engineer.

**RecordHealthy** -- Writes a health record with uptime percentage. **StoreMetrics** --
Persists data points to `/tmp/uptime/metrics/` with `retention: "90d"`.

## Tests

21 unit tests cover endpoint checking, result preparation, and aggregation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
