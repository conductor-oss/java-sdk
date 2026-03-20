# Maintenance Window Management in Java Using Conductor :  Time-Window Checks with Execute or Defer

A Java Conductor workflow example for maintenance window management .  checking whether the current time falls within an approved maintenance window, executing maintenance tasks if it does, or deferring to the next available window if it doesn't.

## The Problem

You need to perform maintenance .  database migrations, certificate rotations, patch deployments; but only during approved maintenance windows. If a maintenance task is triggered outside the window, it must be deferred rather than executed. The system must check the current time against the window schedule and route accordingly: execute now or schedule for later.

Without orchestration, maintenance windows are enforced by human judgment .  an engineer checks the clock before running a script. Automated scripts either ignore maintenance windows entirely (running at any time) or are scheduled with cron at fixed times that don't adapt when windows change.

## The Solution

**You just write the window schedule checks and maintenance task logic. Conductor handles time-window evaluation with conditional routing, retries on maintenance task failures, and a record of every execution or deferral with timing details.**

A window checker worker evaluates whether the current time falls within the maintenance window. Conductor's SWITCH task routes to either the execute path or the defer path. If maintenance runs, it's tracked with timing and results. If deferred, the deferral is recorded with the reason and next available window. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

CheckWindowWorker determines if the current time falls within the maintenance window, then Conductor routes to either ExecuteMaintenanceWorker to run tasks like db-vacuum and index-rebuild, or DeferMaintenanceWorker to schedule for the next approved window.

| Worker | Task | What It Does |
|---|---|---|
| **CheckWindowWorker** | `mnw_check_window` | Checks whether the current time falls within the approved maintenance window for a system, returning window status and remaining minutes |
| **DeferMaintenanceWorker** | `mnw_defer_maintenance` | Defers maintenance to the next available window, recording the reason and scheduled time |
| **ExecuteMaintenanceWorker** | `mnw_execute_maintenance` | Runs maintenance tasks (db-vacuum, index-rebuild, cache-clear) on the target system and reports duration and completed tasks |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
mnw_check_window
    │
    ▼
SWITCH (mnw_switch_ref)
    ├── in_window: mnw_execute_maintenance
    └── default: mnw_defer_maintenance
```

## Example Output

```
=== Example 408: Maintenance Windows ===

Step 1: Registering task definitions...
  Registered: mnw_check_window, mnw_execute_maintenance, mnw_defer_maintenance

Step 2: Registering workflow 'maintenance_windows_408'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [check] Checking maintenance window for
  [defer] Deferring
  [execute] Running

  Status: COMPLETED
  Output: {windowStatus=..., windowStart=..., windowEnd=..., nextWindowStart=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/maintenance-windows-1.0.0.jar
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
java -jar target/maintenance-windows-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow maintenance_windows_408 \
  --version 1 \
  --input '{"system": "sample-system", "production-db": "sample-production-db", "maintenanceType": "standard", "database-optimization": "sample-database-optimization", "currentTime": "2025-01-15T10:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w maintenance_windows_408 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one window concern .  connect the window checker to your maintenance schedule (PagerDuty, ServiceNow), the executor to run real tasks like database migrations, and the check-route-execute-or-defer workflow stays the same.

- **CheckWindowWorker** (`mnw_check_window`): query your maintenance window schedule from PagerDuty, ServiceNow, or a shared calendar API
- **DeferMaintenanceWorker** (`mnw_defer_maintenance`): reschedule the maintenance workflow for the next available window using Conductor's scheduler or a calendar-aware trigger
- **ExecuteMaintenanceWorker** (`mnw_execute_maintenance`): run real maintenance tasks .  database migrations via Flyway, certificate rotations, Kubernetes rolling updates

Connect to your scheduling system and real maintenance commands, and the window-check-then-execute-or-defer flow transfers without any workflow edits.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
maintenance-windows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/maintenancewindows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MaintenanceWindowsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckWindowWorker.java
│       ├── DeferMaintenanceWorker.java
│       └── ExecuteMaintenanceWorker.java
└── src/test/java/maintenancewindows/workers/
    ├── CheckWindowWorkerTest.java        # 2 tests
    └── ExecuteMaintenanceWorkerTest.java        # 2 tests
```
