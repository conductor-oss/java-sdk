# Event Filtering in Java Using Conductor

Event filtering workflow that receives events, classifies them by priority, and routes to urgent, standard, or drop handlers via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to filter incoming events by priority and route each to the appropriate processing lane. High-severity events must be handled urgently with fast-track processing and immediate alerting. Standard events go through normal processing. Low-priority or noise events are dropped to avoid wasting resources. Treating all events equally means critical alerts are delayed by a backlog of low-priority noise.

Without orchestration, you'd build a priority classifier with if/else chains, manually routing events to different processing queues, handling misclassified events that end up in the wrong lane, and tuning severity thresholds with hard-coded constants.

## The Solution

**You just write the event-receive, priority-classification, urgent-handler, standard-handler, and drop workers. Conductor handles priority-based SWITCH routing, per-lane retry policies, and complete visibility into every filter decision.**

Each processing lane is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of receiving the event, classifying its priority, routing via a SWITCH task to the correct lane (urgent, standard, or drop), retrying failed processing, and tracking every event's classification and outcome. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement priority-based filtering: ReceiveEventWorker ingests events, ClassifyPriorityWorker assigns severity, then UrgentHandlerWorker fast-tracks critical events, StandardHandlerWorker queues normal events, and DropEventWorker discards noise.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyPriorityWorker** | `ef_classify_priority` | Classifies an event into a priority level based on severity. |
| **DropEventWorker** | `ef_drop_event` | Handles events that are dropped due to unknown severity levels. |
| **ReceiveEventWorker** | `ef_receive_event` | Receives an incoming event and enriches it with metadata. |
| **StandardHandlerWorker** | `ef_standard_handler` | Handles standard (medium/low severity) events by queuing for batch processing. |
| **UrgentHandlerWorker** | `ef_urgent_handler` | Handles urgent (critical/high severity) events with immediate alerting. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
ef_receive_event
    │
    ▼
ef_classify_priority
    │
    ▼
SWITCH (switch_ref)
    ├── urgent: ef_urgent_handler
    ├── standard: ef_standard_handler
    └── default: ef_drop_event
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
java -jar target/event-filtering-1.0.0.jar
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
java -jar target/event-filtering-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_filtering_wf \
  --version 1 \
  --input '{"eventId": "TEST-001", "eventType": "test-value", "severity": "test-value", "payload": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_filtering_wf -s COMPLETED -c 5
```

## How to Extend

Connect each handler worker to your real alerting system (PagerDuty, OpsGenie for urgent) and batch-processing queue (for standard), the receive-classify-route filtering workflow stays exactly the same.

- **Event classifier**: use ML-based anomaly detection or configurable rule engines to classify severity instead of static thresholds
- **Urgent handler**: implement fast-track processing with immediate PagerDuty/OpsGenie alerting for critical events
- **Standard handler**: process normal events with standard SLAs and batch them for efficiency
- **Drop handler**: log dropped events to your analytics platform for filter-rule tuning and false-positive analysis

Adjusting severity thresholds in ClassifyPriorityWorker or adding a new priority lane requires no changes to the routing workflow.

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
event-filtering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventfiltering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventFilteringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyPriorityWorker.java
│       ├── DropEventWorker.java
│       ├── ReceiveEventWorker.java
│       ├── StandardHandlerWorker.java
│       └── UrgentHandlerWorker.java
└── src/test/java/eventfiltering/workers/
    ├── ClassifyPriorityWorkerTest.java        # 10 tests
    ├── DropEventWorkerTest.java        # 8 tests
    ├── ReceiveEventWorkerTest.java        # 8 tests
    ├── StandardHandlerWorkerTest.java        # 8 tests
    └── UrgentHandlerWorkerTest.java        # 8 tests
```
