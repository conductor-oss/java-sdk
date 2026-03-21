# Event Handlers in Java with Conductor

Workflow triggered by external events. Processes the event type and payload. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to trigger a workflow automatically whenever an external event arrives. a webhook fires, a message lands on a queue, or a system emits a lifecycle event. The event carries a type (e.g., `order.created`, `user.signup`, `payment.failed`) and an arbitrary JSON payload. Your processing logic must dispatch based on the event type, parse the payload, execute the appropriate business action, and confirm that the event was handled successfully.

Without orchestration, you'd write a message consumer or webhook handler that processes events inline, with no retry logic if processing fails, no record of which events were handled, and no way to replay a missed event. If the handler crashes mid-processing, the event is lost or redelivered with no idempotency guarantee. Debugging which events were processed, and what the handler did with each one. requires digging through application logs.

## The Solution

**You just write the event processing worker. Conductor handles the event-to-workflow triggering, retries, and audit trail.**

This example demonstrates Conductor's event handler mechanism. external events trigger workflow executions automatically. When an event arrives with an `eventType` and `payload`, Conductor starts the `event_triggered_workflow`, which routes the event to the ProcessEventWorker. The worker parses the event type and payload, executes the appropriate processing logic, and returns a confirmation. Conductor tracks every event-triggered execution with its input event, processing result, and timing, giving you a complete audit trail of event handling. If the worker fails to process an event, Conductor retries it automatically without losing the event data.

### What You Write: Workers

A single ProcessEventWorker parses the event type and payload, executes the appropriate business logic, and returns a processing confirmation. Conductor triggers this worker automatically when external events arrive.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessEventWorker** | `eh_process_event` | Processes an incoming event. Takes an eventType and payload, returns a result message confirming processing. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
eh_process_event

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
java -jar target/event-handlers-1.0.0.jar

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
java -jar target/event-handlers-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_triggered_workflow \
  --version 1 \
  --input '{"eventType": "standard", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_triggered_workflow -s COMPLETED -c 5

```

## How to Extend

Replace the event processor with your real business logic for handling webhooks, queue messages, or lifecycle events, and the event-triggered workflow runs unchanged.

- **ProcessEventWorker** (`eh_process_event`): route events by type to real handlers: write `order.created` events to your order database, send `user.signup` events to your CRM (Salesforce, HubSpot), publish `payment.failed` events to a Slack channel or PagerDuty incident, or forward events to a data lake (S3, BigQuery) for analytics

Replacing the demo processing with real event handling logic (order creation, payment processing, etc.) does not require workflow changes, since the event-to-workflow triggering mechanism remains the same.

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
event-handlers/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventhandlers/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventHandlersExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── ProcessEventWorker.java
└── src/test/java/eventhandlers/workers/
    └── ProcessEventWorkerTest.java        # 7 tests

```
