# Time-Based Triggers in Java Using Conductor :  Route Jobs by Time Window (Morning, Afternoon, Evening)

A Java Conductor workflow example for time-based triggering. checking the current time and routing to different jobs based on the time window (morning batch jobs, afternoon reports, evening maintenance tasks).

## The Problem

Different jobs should run at different times of day. morning is for data ingestion and ETL, afternoon for report generation and distribution, evening for maintenance and cleanup. You need to check the current time window and route to the appropriate job. This is more flexible than cron (which fires at exact times) because it handles late triggers, catch-up runs, and timezone-aware routing.

Without orchestration, time-based routing is hardcoded in cron schedules or manual if/else checks at the top of scripts. When the morning job runs late, it doesn't automatically become an afternoon job. Adding a new time window means restructuring the schedule.

## The Solution

**You just write the time-window detection and per-window job logic. Conductor handles SWITCH-based time-window routing, retries when individual jobs fail, and a full history of every trigger showing which window was detected and which job executed.**

A time checker worker determines the current time window (morning, afternoon, evening). Conductor's SWITCH task routes to the appropriate job. Each job is an independent worker that does its time-window-specific work. Every trigger is tracked with the detected time window and which job ran. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CheckTimeWorker determines the current time window (morning, afternoon, or evening), then Conductor routes to the appropriate handler: MorningJobWorker for data ingestion, AfternoonJobWorker for report generation, or EveningJobWorker for maintenance and cleanup.

| Worker | Task | What It Does |
|---|---|---|
| **AfternoonJobWorker** | `tb_afternoon_job` | Runs afternoon tasks (e.g., report generation), returning the count of reports generated |
| **CheckTimeWorker** | `tb_check_time` | Determines the current time window (morning/afternoon/evening) based on hour and timezone |
| **EveningJobWorker** | `tb_evening_job` | Runs evening tasks (e.g., cleanup and maintenance), returning the count of files cleaned up |
| **MorningJobWorker** | `tb_morning_job` | Runs morning tasks (e.g., data sync and ETL), returning the count of records synced |

Workers implement scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic. the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
tb_check_time
    │
    ▼
SWITCH (tb_switch_ref)
    ├── morning: tb_morning_job
    ├── afternoon: tb_afternoon_job
    └── default: tb_evening_job

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
java -jar target/time-based-triggers-1.0.0.jar

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
java -jar target/time-based-triggers-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow time_based_triggers_405 \
  --version 1 \
  --input '{"timezone": "2026-01-01T00:00:00Z", "currentHour": "sample-currentHour"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w time_based_triggers_405 -s COMPLETED -c 5

```

## How to Extend

Each worker runs one time-window job. connect the morning worker to your ETL pipeline, the afternoon worker to report generation, the evening worker to maintenance scripts, and the check-time-then-route workflow stays the same.

- **AfternoonJobWorker** (`tb_afternoon_job`): run afternoon tasks. generate daily reports, send summary emails, sync CRM data
- **CheckTimeWorker** (`tb_check_time`): determine the time window using configurable hour ranges and timezone support for global teams
- **EveningJobWorker** (`tb_evening_job`): run evening tasks. database maintenance, log rotation, cache warm-up for the next day

Point each time-window worker at your real ETL, reporting, or maintenance systems and the routing logic requires no changes.

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
time-based-triggers/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/timebasedtriggers/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TimeBasedTriggersExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AfternoonJobWorker.java
│       ├── CheckTimeWorker.java
│       ├── EveningJobWorker.java
│       └── MorningJobWorker.java
└── src/test/java/timebasedtriggers/workers/
    ├── CheckTimeWorkerTest.java        # 4 tests
    └── MorningJobWorkerTest.java        # 2 tests

```
