# Scheduled Maintenance Without False Alert Storms

You need to upgrade the database-cluster, but every time someone does maintenance without
suppressing alerts, PagerDuty fires dozens of false pages and the status page stays green
while the system is actually down. This workflow notifies stakeholders, suppresses alerts,
executes the maintenance, and restores everything when done.

## Workflow

```
system, maintenanceType, duration
              |
              v
+--------------------+     +----------------------+     +---------------------------+     +---------------------+
| mw_notify_start    | --> | mw_suppress_alerts   | --> | mw_execute_maintenance    | --> | mw_restore_normal   |
+--------------------+     +----------------------+     +---------------------------+     +---------------------+
  NOTIFY_START-1336          suppression enabled          version-upgrade completed        alerts re-enabled
  database-cluster (2h)                                                                    status page updated
```

## Workers

**NotifyStartWorker** -- Announces the maintenance window for database-cluster (2 hours).
Returns `notify_startId: "NOTIFY_START-1336"`.

**SuppressAlertsWorker** -- Enables alert suppression for the maintenance window. Returns
`suppress_alerts: true`.

**ExecuteMaintenanceWorker** -- Performs the version-upgrade. Returns
`execute_maintenance: true`.

**RestoreNormalWorker** -- Re-enables alerts, updates the status page, and ends the
maintenance window. Returns `restore_normal: true`.

## Tests

2 unit tests cover the maintenance window pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
