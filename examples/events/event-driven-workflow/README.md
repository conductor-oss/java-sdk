# Event Driven Workflow in Java Using Conductor

Event-driven workflow that receives events, classifies them by type, and routes to the appropriate handler via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to route incoming events to different handlers based on their type. When events arrive, each one must be classified (user event, order event, system event, etc.) and dispatched to the handler that knows how to process that specific type. Sending an order event to the user handler produces incorrect results; dropping an unknown event type loses data.

Without orchestration, you'd build an event dispatcher with a switch statement or handler registry, manually routing events by type, handling unknown types with fallback logic, and logging every routing decision to debug misrouted events.

## The Solution

**You just write the event-receiver, classifier, and type-specific handler workers. Conductor handles type-based SWITCH routing, per-handler retries, and full event classification and processing history.**

Each event type handler is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of receiving the event, classifying it, routing via a SWITCH task to the correct handler, retrying if the handler fails, and tracking every event's routing and processing. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers route events by type: ReceiveEventWorker ingests events, ClassifyEventWorker determines the category, then HandleOrderWorker, HandlePaymentWorker, or HandleGenericWorker processes the event based on its classification.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyEventWorker** | `ed_classify_event` | Classifies an event by its type into a category and priority. |
| **HandleGenericWorker** | `ed_handle_generic` | Handles generic (unclassified) events. |
| **HandleOrderWorker** | `ed_handle_order` | Handles order-related events. |
| **HandlePaymentWorker** | `ed_handle_payment` | Handles payment-related events. |
| **ReceiveEventWorker** | `ed_receive_event` | Receives an incoming event and enriches it with metadata. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
ed_receive_event
    │
    ▼
ed_classify_event
    │
    ▼
SWITCH (category_switch_ref)
    ├── order: ed_handle_order
    ├── payment: ed_handle_payment
    └── default: ed_handle_generic
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
java -jar target/event-driven-workflow-1.0.0.jar
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
java -jar target/event-driven-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_driven_wf \
  --version 1 \
  --input '{"eventId": "TEST-001", "eventType": "test-value", "eventData": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_driven_wf -s COMPLETED -c 5
```

## How to Extend

Point each handler worker at your real order service, payment processor, and generic event handler, the receive-classify-route event workflow stays exactly the same.

- **Event classifier**: classify events using business rules, ML models, or event schema metadata instead of simple type fields
- **Type-specific handlers**: implement real processing logic for each event type (user events to your CRM, order events to your OMS, system events to your monitoring platform)
- Add new event types by adding a new SWITCH case and worker. No changes to existing handlers required

Adding a new event type requires only a new handler worker and SWITCH branch. Existing handlers remain untouched.

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
event-driven-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventdrivenworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventDrivenWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyEventWorker.java
│       ├── HandleGenericWorker.java
│       ├── HandleOrderWorker.java
│       ├── HandlePaymentWorker.java
│       └── ReceiveEventWorker.java
└── src/test/java/eventdrivenworkflow/workers/
    ├── ClassifyEventWorkerTest.java        # 9 tests
    ├── HandleGenericWorkerTest.java        # 8 tests
    ├── HandleOrderWorkerTest.java        # 9 tests
    ├── HandlePaymentWorkerTest.java        # 9 tests
    └── ReceiveEventWorkerTest.java        # 9 tests
```
