# Automated Incident Response with Real System Diagnostics

An alert fires at 2 AM. The on-call engineer opens their laptop, SSHes into the wrong box,
runs `top` for a while, and 40 minutes later finds the API gateway at 95% CPU. No ticket
was created, no one knows what happened. This workflow creates the incident record as a JSON
file in `/tmp/incidents/`, pages the on-call via webhook, gathers real system diagnostics,
and attempts auto-remediation.

## Workflow

```
alertName, severity
        |
        v
+-----------------------+     +--------------------+     +---------------------------+     +------------------------+
| ir_create_incident    | --> | ir_notify_oncall   | --> | ir_gather_diagnostics     | --> | ir_auto_remediate      |
+-----------------------+     +--------------------+     +---------------------------+     +------------------------+
  INC-{uuid} written           webhook POST with          real CPU load (uptime),          identify-cpu-hogs (ps),
  to /tmp/incidents/           5s timeout                  disk usage (FileStore API),      identify-disk-usage (du),
  severity: P1-P4              incidentId sent             heap memory stats                service status check
```

## Workers

**CreateIncidentWorker** -- Takes `alertName` and `severity` (default `"P3"`). Generates a
UUID-based `incidentId` like `"INC-A3B2C1D4"` and writes a JSON file to
`/tmp/incidents/{incidentId}.json` with alert details, status `"open"`, and a timeline entry.

**NotifyOncallWorker** -- Sends a webhook POST with the `incidentId` and incident details.
Uses a 5-second connection and read timeout. Falls back gracefully on webhook errors.

**GatherDiagnosticsWorker** -- Collects real system data: CPU load via `uptime`, disk usage
via Java's `FileStore` API (total/used/available bytes, `usedPercent`), and JVM heap memory
(`heapUsedBytes`).

**AutoRemediateWorker** -- Analyzes diagnostics and takes action: if `cpuLoad > 2.0`, runs
`ps aux` to identify top CPU-consuming processes. If any disk exceeds 90% usage, runs `du -sh
/tmp`. If a service name is provided, checks status via `launchctl list` (macOS) or
`systemctl status` (Linux). Never runs destructive commands (no `kill -9`, no `rm -rf`).

## Tests

29 unit tests cover incident creation, on-call notification, diagnostics gathering, and
auto-remediation scenarios.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
