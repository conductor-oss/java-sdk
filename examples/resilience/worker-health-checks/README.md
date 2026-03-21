# Implementing Worker Health Checks in Java with Conductor :  Monitoring Worker Availability and Performance

A Java Conductor workflow example demonstrating worker health monitoring .  running a task and using Conductor's APIs to verify worker availability, poll rates, and execution health across the system.

## The Problem

You have workers deployed across multiple hosts or containers. You need to know: are they running? Are they polling? How fast are they processing tasks? If a worker stops polling (process crashed, deployment failed, network issue), tasks queue up and workflows stall. You need visibility into worker health before it becomes a production incident.

Without orchestration, worker health monitoring requires custom infrastructure .  process supervisors, heartbeat endpoints, and separate monitoring dashboards. Each worker must implement its own health reporting, and there's no unified view of worker fleet health.

## The Solution

Conductor tracks worker health automatically .  poll timestamps, task completion rates, and queue depths are all available via Conductor's APIs. The example demonstrates querying these APIs to build health dashboards and set up alerting. Every worker's polling behavior and task execution is recorded without any health-check code in the workers themselves. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

WhcWorker processes tasks while tracking health metrics (poll counts, completion rates), and Conductor's APIs provide unified visibility into worker availability, queue depths, and execution performance across the fleet.

| Worker | Task | What It Does |
|---|---|---|
| **WhcWorker** | `whc_task` | Worker for whc_task .  processes tasks and tracks health metrics. Maintains thread-safe poll and completed counters th.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
whc_task

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
java -jar target/worker-health-checks-1.0.0.jar

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
java -jar target/worker-health-checks-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow worker_health_checks_demo \
  --version 1 \
  --input '{"data": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w worker_health_checks_demo -s COMPLETED -c 5

```

## How to Extend

Each worker processes real tasks .  connect them to your business services, query Conductor's health APIs for poll rates and queue depths, and the automatic worker health monitoring stays the same.

- **WhcWorker** (`whc_task`): replace with any real worker .  the health monitoring is done via Conductor's APIs, not the worker code itself; build dashboards with Grafana/Datadog pulling from Conductor's metrics endpoints

Replace with any real worker, and Conductor's built-in health APIs provide fleet-wide worker monitoring without adding any health-check code to the workers themselves.

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
worker-health-checks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workerhealthchecks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkerHealthChecksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── WhcWorker.java
└── src/test/java/workerhealthchecks/workers/
    └── WhcWorkerTest.java        # 9 tests

```
