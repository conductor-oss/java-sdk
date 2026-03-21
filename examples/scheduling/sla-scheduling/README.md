# SLA Scheduling in Java Using Conductor :  Ticket Prioritization, Execution, and Compliance Tracking

A Java Conductor workflow example for SLA-aware scheduling .  prioritizing tickets by SLA urgency, executing tasks in priority order, and tracking SLA compliance to prevent breaches.

## The Problem

You have tickets with SLA commitments. P1 tickets need resolution within 1 hour, P2 within 4 hours, P3 within 24 hours. Tasks must be prioritized by SLA proximity (how close to breach), executed in the right order, and compliance must be tracked. If a P2 ticket will breach its SLA in 30 minutes while a P3 has 20 hours remaining, the P2 must be worked first.

Without orchestration, SLA prioritization is a manual sort in a ticketing system. Agents pick tickets by FIFO rather than SLA urgency, breaches happen because high-urgency tickets were buried, and compliance tracking requires a separate report that's always outdated.

## The Solution

**You just write the SLA prioritization rules and compliance calculations. Conductor handles the prioritize-execute-track sequence, retries on ticket system failures, and SLA compliance metrics with per-ticket timing for every scheduling cycle.**

Each SLA concern is an independent worker .  prioritization by SLA urgency, task execution in priority order, and compliance tracking. Conductor runs them in sequence: prioritize the queue, execute in order, then track compliance. Every scheduling run is tracked with priority assignments, execution timing, and SLA compliance metrics. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers enforce SLA discipline: PrioritizeWorker sorts tickets by SLA urgency so the closest-to-breach items come first, ExecuteTasksWorker processes them in priority order, and TrackComplianceWorker calculates on-time completion rates and flags breaches.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteTasksWorker** | `sla_execute_tasks` | Processes tickets in SLA priority order, resolving each and recording time-to-resolution |
| **PrioritizeWorker** | `sla_prioritize` | Sorts tickets by SLA urgency (1h, 4h, 24h deadlines), returning an ordered list with priority rankings |
| **TrackComplianceWorker** | `sla_track_compliance` | Calculates SLA compliance rate, counting on-time completions vs, breaches and average resolution time |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
sla_prioritize
    │
    ▼
sla_execute_tasks
    │
    ▼
sla_track_compliance

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
java -jar target/sla-scheduling-1.0.0.jar

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
java -jar target/sla-scheduling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sla_scheduling_409 \
  --version 1 \
  --input '{"tickets": "sample-tickets", "slaPolicy": "sample-slaPolicy"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sla_scheduling_409 -s COMPLETED -c 5

```

## How to Extend

Each worker manages one SLA phase .  connect the prioritizer to your ticketing system (Jira, ServiceNow), the compliance tracker to your SLA dashboard, and the prioritize-execute-track workflow stays the same.

- **ExecuteTasksWorker** (`sla_execute_tasks`): route tasks to the right team/agent based on skills and availability, update ticket status in your PM tool
- **PrioritizeWorker** (`sla_prioritize`): pull tickets from Jira/ServiceNow/Zendesk, compute SLA urgency scores based on remaining time and business impact
- **TrackComplianceWorker** (`sla_track_compliance`): calculate SLA compliance rates, report breaches, and push metrics to your compliance dashboard or GRC platform

Integrate with your ticketing system, and the SLA-aware scheduling workflow operates in production with no orchestration changes.

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
sla-scheduling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/slascheduling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SlaSchedulingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteTasksWorker.java
│       ├── PrioritizeWorker.java
│       └── TrackComplianceWorker.java
└── src/test/java/slascheduling/workers/
    └── TrackComplianceWorkerTest.java        # 2 tests

```
