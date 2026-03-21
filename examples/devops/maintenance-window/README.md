# Maintenance Window in Java with Conductor : Notify, Suppress Alerts, Execute Maintenance, Restore

Automates scheduled maintenance windows using [Conductor](https://github.com/conductor-oss/conductor). This workflow notifies stakeholders that maintenance is starting, suppresses monitoring alerts to prevent false pages, executes the maintenance task (upgrades, patches, migrations), and restores normal operations by re-enabling alerts and updating the status page.

## Planned Downtime Without the Chaos

You need to upgrade the database cluster tonight. That means a 2-hour maintenance window. Everyone needs to know it is happening, alerting needs to be silenced so the on-call engineer does not get paged for expected downtime, the actual upgrade needs to run, and when it is done, alerts need to come back on and the status page needs to reflect normal operations. If any step is missed: say alerts are not re-enabled, the next real incident goes unnoticed.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the maintenance execution and notification logic. Conductor handles alert suppression sequencing, guaranteed restore, and maintenance window tracking.**

`NotifyStartWorker` sends maintenance notifications to stakeholders. operations team, customer-facing status page, and affected service owners, with expected duration and impact. `SuppressAlertsWorker` creates maintenance windows in monitoring systems to prevent false alerts during the planned downtime. `ExecuteMaintenanceWorker` performs the actual maintenance tasks, database upgrades, server patching, configuration changes. `RestoreNormalWorker` re-enables monitoring alerts, sends completion notifications, and verifies that all systems have returned to healthy state. Conductor ensures the restore step runs even if maintenance encounters issues.

### What You Write: Workers

Four workers manage the maintenance window. Notifying stakeholders, suppressing alerts, executing the maintenance task, and restoring normal operations.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteMaintenanceWorker** | `mw_execute_maintenance` | Runs the scheduled maintenance task (version upgrade, patch, migration) on the target system |
| **NotifyStartWorker** | `mw_notify_start` | Notifies stakeholders that a maintenance window has started, including system name and duration |
| **RestoreNormalWorker** | `mw_restore_normal` | Re-enables alerts, updates the status page, and marks the maintenance window as ended |
| **SuppressAlertsWorker** | `mw_suppress_alerts` | Suppresses monitoring alerts for the target system during the maintenance window |

the workflow and rollback logic stay the same.

### The Workflow

```
mw_notify_start
 │
 ▼
mw_suppress_alerts
 │
 ▼
mw_execute_maintenance
 │
 ▼
mw_restore_normal

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
