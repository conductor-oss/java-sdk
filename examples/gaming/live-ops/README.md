# Live Ops in Java Using Conductor

Runs a time-limited live ops event in a game: scheduling the event, configuring rewards and difficulty, deploying to servers across regions, monitoring engagement, and closing with reward distribution. Uses [Conductor](https://github.## The Problem

You need to run a live operations event in your game .  a time-limited in-game event with special content, challenges, and rewards. The workflow schedules the event for a start/end date, configures the event parameters (rewards, difficulty, matchmaking rules), deploys the configuration to game servers, monitors player engagement and server health during the event, and closes the event when it ends. Deploying without proper configuration breaks the player experience; not monitoring means missing critical issues during the event.

Without orchestration, you'd manage live ops events through a combination of admin tools, manual server config pushes, monitoring dashboards, and calendar reminders .  risking missed deployment times, misconfigured events, and undetected server issues during peak player activity.

## The Solution

**You just write the event scheduling, reward configuration, server deployment, engagement monitoring, and reward distribution logic. Conductor handles deployment retries, event scheduling, and live ops campaign tracking.**

Each live-ops concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (schedule, configure, deploy, monitor, close), retrying if a server deployment fails, tracking every live event's lifecycle, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Event scheduling, content deployment, player targeting, and metrics collection workers enable live operations through independent, swappable stages.

| Worker | Task | What It Does |
|---|---|---|
| **CloseWorker** | `lop_close` | Closes the event, distributes rewards to participants, and returns final stats |
| **ConfigureWorker** | `lop_configure` | Configures event parameters: rewards (Double XP, Rare Skin), difficulty, and duration |
| **DeployWorker** | `lop_deploy` | Deploys the event configuration to game servers across all regions (NA, EU, APAC) |
| **MonitorWorker** | `lop_monitor` | Monitors the live event tracking participant count, engagement level, and server issues |
| **ScheduleEventWorker** | `lop_schedule_event` | Schedules the event with name and start/end dates, and assigns an event ID |

Workers simulate game backend operations .  matchmaking, score processing, reward distribution ,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
lop_schedule_event
    │
    ▼
lop_configure
    │
    ▼
lop_deploy
    │
    ▼
lop_monitor
    │
    ▼
lop_close
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
java -jar target/live-ops-1.0.0.jar
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
java -jar target/live-ops-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow live_ops_748 \
  --version 1 \
  --input '{"eventName": "test", "startDate": "2026-01-01T00:00:00Z", "endDate": "2026-01-01T00:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w live_ops_748 -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real live ops tools .  your event management system for scheduling, your CDN for content deployment, your analytics platform for engagement monitoring, and the workflow runs identically in production.

- **Scheduler**: create the event schedule in your live-ops platform (PlayFab, AccelByte, custom admin) with timezone-aware start/end times
- **Configurator**: set event parameters (reward tables, difficulty curves, matchmaking pools) in your game config service
- **Deployer**: push event configurations to game servers via your deployment pipeline (feature flags, hot config, content delivery)
- **Monitor**: track real-time engagement metrics (participation rate, completion rate, revenue) and server health via your observability platform
- **Closer**: end the event, distribute final rewards, archive event data for analytics, and restore default game configuration

Swap content delivery backends or targeting rules and the live ops pipeline continues without changes.

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
live-ops/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/liveops/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LiveOpsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseWorker.java
│       ├── ConfigureWorker.java
│       ├── DeployWorker.java
│       ├── MonitorWorker.java
│       └── ScheduleEventWorker.java
└── src/test/java/liveops/workers/
    ├── CloseWorkerTest.java
    └── ScheduleEventWorkerTest.java
```
