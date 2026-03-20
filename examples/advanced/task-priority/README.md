# Task Priority Routing in Java Using Conductor :  Classify Urgency, Route to Priority-Specific Queues

A Java Conductor workflow example for task priority routing .  classifying incoming tasks by urgency and impact into high/medium/low priority, then routing each to the appropriate processing queue via a `SWITCH` task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Not All Tasks Are Equal :  High Priority Must Jump the Queue

A production incident alert, a routine log rotation, and a monthly report request arrive at the same time. The incident needs immediate attention with dedicated resources. The log rotation can wait. The report can run overnight. Without priority classification and routing, all three compete for the same worker pool, and the incident sits behind the report in the queue.

Priority routing means classifying each task based on urgency and impact (a P1 incident is high/high, a log rotation is low/low), then routing it to the matching queue .  high-priority tasks go to a dedicated fast lane with more workers and shorter timeouts, while low-priority tasks go to a batch queue that runs during off-peak hours.

## The Solution

**You write the classification and per-tier handling logic. Conductor handles priority routing, retries, and SLA tracking.**

`TprClassifyPriorityWorker` evaluates the task's urgency and impact to determine its priority level .  high, medium, or low. A `SWITCH` task routes based on the classification: `TprRouteHighWorker` handles urgent tasks with dedicated resources and aggressive SLAs, `TprRouteMediumWorker` processes normal tasks with standard resources, and `TprRouteLowWorker` queues low-priority work for batch processing. Conductor's conditional routing makes the priority lanes declarative, and every execution records the classification rationale.

### What You Write: Workers

Four workers handle priority-based routing. urgency classification and three tier-specific handlers (high, medium, low), each queue operating with its own SLA and resource allocation.

| Worker | Task | What It Does |
|---|---|---|
| **TprClassifyPriorityWorker** | `tpr_classify_priority` | Evaluates urgency and impact to assign a priority level (high/medium/low) and SLA deadline |
| **TprRouteHighWorker** | `tpr_route_high` | Routes the task to the high-priority queue with a 15-minute SLA |
| **TprRouteLowWorker** | `tpr_route_low` | Routes the task to the low-priority queue with a 240-minute SLA |
| **TprRouteMediumWorker** | `tpr_route_medium` | Routes the task to the medium-priority queue with a 60-minute SLA |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

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
tpr_classify_priority
    │
    ▼
SWITCH (tpr_switch_ref)
    ├── high: tpr_route_high
    ├── medium: tpr_route_medium
    ├── low: tpr_route_low
    └── default: tpr_route_medium
```

## Example Output

```
=== Task Priority Demo ===

Step 1: Registering task definitions...
  Registered: tpr_classify_priority, tpr_route_high, tpr_route_medium, tpr_route_low

Step 2: Registering workflow 'tpr_task_priority'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [classify] Processing
  [route-high] Processing
  [route-low] Processing
  [route-medium] Processing

  Status: COMPLETED
  Output: {priority=..., slaMinutes=..., queue=..., acknowledged=...}

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
java -jar target/task-priority-1.0.0.jar
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
java -jar target/task-priority-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tpr_task_priority \
  --version 1 \
  --input '{"taskId": "TASK-9001", "TASK-9001": "urgency", "urgency": "high", "high": "impact", "impact": "high"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tpr_task_priority -s COMPLETED -c 5
```

## How to Extend

Each worker handles one priority tier .  replace the simulated queue routing with real priority-based SQS queues or Kubernetes resource allocations and the classify-then-route logic runs unchanged.

- **TprClassifyPriorityWorker** (`tpr_classify_priority`): implement real priority classification using PagerDuty severity levels, Jira priority fields, or custom urgency/impact matrices from your incident management system
- **TprRouteHighWorker** (`tpr_route_high`): dispatch to dedicated high-priority infrastructure: a separate Kubernetes namespace with guaranteed resources, a priority SQS queue, or trigger PagerDuty incident creation
- **TprRouteLowWorker** (`tpr_route_low`): queue for batch processing: write to a low-priority SQS queue consumed by spot instances, schedule via cron for off-peak hours, or add to a backlog in your project management tool

The classification output contract stays fixed. Swap the simulated queue dispatch for real SQS priority queues or PagerDuty escalation and the priority routing runs unchanged.

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
task-priority/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskpriority/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskPriorityExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── TprClassifyPriorityWorker.java
│       ├── TprRouteHighWorker.java
│       ├── TprRouteLowWorker.java
│       └── TprRouteMediumWorker.java
└── src/test/java/taskpriority/workers/
    ├── TprClassifyPriorityWorkerTest.java        # 4 tests
    ├── TprRouteHighWorkerTest.java        # 4 tests
    ├── TprRouteLowWorkerTest.java        # 4 tests
    └── TprRouteMediumWorkerTest.java        # 4 tests
```
