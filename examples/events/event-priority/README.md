# Event Priority in Java Using Conductor

Event priority workflow that classifies events by priority and routes to the appropriate processing lane via a SWITCH task, then records the result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to classify incoming events by priority and route each to the appropriate processing lane. Critical events require immediate processing, normal events go through standard pipelines, and low-priority events are handled in batch. The result of each processing lane must be recorded for auditing. Without priority-based routing, critical system alerts wait behind thousands of routine telemetry events.

Without orchestration, you'd build a priority classifier with a switch statement, manually managing separate thread pools or queues for each priority level, handling priority inversions, and recording processing results across different execution paths.

## The Solution

**You just write the priority-classification, per-lane processing, and result-recording workers. Conductor handles SWITCH-based priority routing, per-lane processing guarantees, and a complete audit of every priority decision.**

Each priority lane is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of classifying the event, routing via a SWITCH task to the appropriate lane (critical, normal, low), processing the event, recording the result, and retrying if processing fails. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement priority lanes: ClassifyPriorityWorker assigns a level, then ProcessUrgentWorker, ProcessNormalWorker, or ProcessBatchWorker handles the event in its lane, and RecordProcessingWorker logs the outcome for audit.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyPriorityWorker** | `pr_classify_priority` | Classifies an event into a priority level based on its type. |
| **ProcessBatchWorker** | `pr_process_batch` | Processes low-priority events in the batch lane (default case). |
| **ProcessNormalWorker** | `pr_process_normal` | Processes medium-priority events in the normal lane. |
| **ProcessUrgentWorker** | `pr_process_urgent` | Processes high-priority events in the urgent lane. |
| **RecordProcessingWorker** | `pr_record_processing` | Records the processing result for auditing. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
pr_classify_priority
    │
    ▼
SWITCH (priority_switch_ref)
    ├── high: pr_process_urgent
    ├── medium: pr_process_normal
    └── default: pr_process_batch
    │
    ▼
pr_record_processing
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
java -jar target/event-priority-1.0.0.jar
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
java -jar target/event-priority-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_priority_wf \
  --version 1 \
  --input '{"eventId": "TEST-001", "eventType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_priority_wf -s COMPLETED -c 5
```

## How to Extend

Connect each lane worker to your real urgent, normal, and batch processing queues and auditing system, the classify-route-process-record priority workflow stays exactly the same.

- **Classifier**: implement ML-based priority scoring or configurable rules engines instead of static event-type mappings
- **Critical lane**: process with immediate execution, SLA tracking, and automatic escalation on failure
- **Normal/Low lanes**: batch processing with appropriate latency budgets and resource allocation
- **Result recorder**: persist processing outcomes and SLA compliance metrics to your observability platform

Adding a new priority tier requires one worker and a SWITCH branch. Existing lanes and the recording step stay the same.

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
event-priority/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventpriority/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventPriorityExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyPriorityWorker.java
│       ├── ProcessBatchWorker.java
│       ├── ProcessNormalWorker.java
│       ├── ProcessUrgentWorker.java
│       └── RecordProcessingWorker.java
└── src/test/java/eventpriority/workers/
    ├── ClassifyPriorityWorkerTest.java        # 9 tests
    ├── ProcessBatchWorkerTest.java        # 8 tests
    ├── ProcessNormalWorkerTest.java        # 8 tests
    ├── ProcessUrgentWorkerTest.java        # 8 tests
    └── RecordProcessingWorkerTest.java        # 9 tests
```
