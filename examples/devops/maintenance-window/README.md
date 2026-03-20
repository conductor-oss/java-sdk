# Maintenance Window in Java with Conductor :  Notify, Suppress Alerts, Execute Maintenance, Restore

Automates scheduled maintenance windows using [Conductor](https://github.com/conductor-oss/conductor). This workflow notifies stakeholders that maintenance is starting, suppresses monitoring alerts to prevent false pages, executes the maintenance task (upgrades, patches, migrations), and restores normal operations by re-enabling alerts and updating the status page.## Planned Downtime Without the Chaos

You need to upgrade the database cluster tonight. That means a 2-hour maintenance window. Everyone needs to know it is happening, alerting needs to be silenced so the on-call engineer does not get paged for expected downtime, the actual upgrade needs to run, and when it is done, alerts need to come back on and the status page needs to reflect normal operations. If any step is missed: say alerts are not re-enabled, the next real incident goes unnoticed.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the maintenance execution and notification logic. Conductor handles alert suppression sequencing, guaranteed restore, and maintenance window tracking.**

`NotifyStartWorker` sends maintenance notifications to stakeholders .  operations team, customer-facing status page, and affected service owners ,  with expected duration and impact. `SuppressAlertsWorker` creates maintenance windows in monitoring systems to prevent false alerts during the planned downtime. `ExecuteMaintenanceWorker` performs the actual maintenance tasks ,  database upgrades, server patching, configuration changes. `RestoreNormalWorker` re-enables monitoring alerts, sends completion notifications, and verifies that all systems have returned to healthy state. Conductor ensures the restore step runs even if maintenance encounters issues.

### What You Write: Workers

Four workers manage the maintenance window. Notifying stakeholders, suppressing alerts, executing the maintenance task, and restoring normal operations.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteMaintenanceWorker** | `mw_execute_maintenance` | Runs the scheduled maintenance task (version upgrade, patch, migration) on the target system |
| **NotifyStartWorker** | `mw_notify_start` | Notifies stakeholders that a maintenance window has started, including system name and duration |
| **RestoreNormalWorker** | `mw_restore_normal` | Re-enables alerts, updates the status page, and marks the maintenance window as ended |
| **SuppressAlertsWorker** | `mw_suppress_alerts` | Suppresses monitoring alerts for the target system during the maintenance window |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/maintenance-window-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/maintenance-window-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow maintenance_window_workflow \
  --version 1 \
  --input '{"system": "test-value", "maintenanceType": "test-value", "duration": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w maintenance_window_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one maintenance phase .  replace the simulated calls with Statuspage.io, PagerDuty maintenance windows, or Ansible upgrade playbooks, and the maintenance workflow runs unchanged.

- **NotifyStartWorker** (`mw_notify_start`): update Statuspage.io to "Under Maintenance," post to Slack #ops channels, and send customer notification emails via SendGrid with expected downtime duration
- **SuppressAlertsWorker** (`mw_suppress_alerts`): create maintenance windows in PagerDuty, Opsgenie, or Datadog via their APIs to suppress alerts for specific services during the planned downtime
- **ExecuteMaintenanceWorker** (`mw_execute_maintenance`): execute Ansible playbooks, Terraform applies, database migration scripts, or Kubernetes rolling upgrades with proper logging and rollback capability
- **RestoreNormalWorker** (`mw_restore_normal`): delete PagerDuty maintenance windows to re-enable alerts, update Statuspage.io back to "Operational," and send completion notifications confirming the maintenance is finished

Integrate with your monitoring and notification systems; the maintenance workflow preserves the same suppress-execute-restore contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
maintenance-window-maintenance-window/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/maintenancewindow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteMaintenanceWorker.java
│       ├── NotifyStartWorker.java
│       ├── RestoreNormalWorker.java
│       └── SuppressAlertsWorker.java
└── src/test/java/maintenancewindow/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
