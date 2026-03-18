# Dead Letter Events in Java Using Conductor

A `payment.charge` event hits your processor with a malformed payload. The catch block logs a warning. Nobody reads the log. Three days later, a customer calls: "Where's my order?" It's not stuck in a retry loop. It's not in a dead letter queue. It's gone. silently swallowed by a `catch (Exception e) { log.warn(...) }` that your team wrote at 4 PM on a Friday. There are 47 more just like it, and you only know about the one customer who bothered to call. This example builds a dead letter event pipeline with Conductor: receive the event, attempt processing, and route failures to a DLQ with alerting via a `SWITCH` task, so no failed event is ever silently dropped. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to handle events that fail processing. When an event is received and processing fails (malformed payload, missing required fields, downstream service unavailable), it must be routed to a dead letter queue rather than silently dropped, and an alert must be sent so engineers can investigate. Silently losing failed events means data loss; not alerting means failures pile up unnoticed.

Without orchestration, you'd wrap your event processor in try/catch blocks, manually push failed events to a separate queue, send alert emails inline, and manage retry counts with ad-hoc counters. Hoping the DLQ push itself does not fail, and logging everything to debug why events are disappearing.

## The Solution

**You just write the event-receive, processing-attempt, DLQ-routing, and alert workers. Conductor handles failure-path SWITCH routing, guaranteed DLQ delivery via retries, and a full audit of every event's fate.**

Each dead-letter concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of receiving the event, attempting processing, routing failures via a SWITCH task to the DLQ path with alerting or routing successes to finalization, retrying transient failures automatically, and tracking every event's fate. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage failed-event routing: DlReceiveEventWorker ingests the event, DlAttemptProcessWorker tries to handle it, DlRouteToDlqWorker moves failures to the dead-letter queue, DlSendAlertWorker notifies engineers, and DlFinalizeSuccessWorker stamps successful completions.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **DlAttemptProcessWorker** | `dl_attempt_process` | Attempts to process an event. If the payload contains a "requiredField" key, processing succeeds. Otherwise it fails ... | Simulated |
| **DlFinalizeSuccessWorker** | `dl_finalize_success` | Finalizes a successfully processed event by stamping a finalized timestamp. | Simulated |
| **DlReceiveEventWorker** | `dl_receive_event` | Receives an incoming event, normalizes retryCount to an integer, and stamps a receivedAt timestamp. | Simulated |
| **DlRouteToDlqWorker** | `dl_route_to_dlq` | Routes a failed event to the dead letter queue, producing a DLQ entry with all relevant details. | Simulated |
| **DlSendAlertWorker** | `dl_send_alert` | Sends an alert notification when an event is routed to the dead letter queue. | Simulated |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
dl_receive_event
    │
    ▼
dl_attempt_process
    │
    ▼
SWITCH (result_switch_ref)
    ├── success: dl_finalize_success
    └── default: dl_route_to_dlq -> dl_send_alert
```

## Example Output

```
=== Dead Letter Events Demo ===

Step 1: Registering task definitions...
  Registered: dl_receive_event, dl_attempt_process, dl_finalize_success, dl_route_to_dlq, dl_send_alert

Step 2: Registering workflow 'dead_letter_events_wf'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: d4a30129-b50c-cffe-2529-f91cad44b7ee

  [dl_receive_event] Received event: evt-fixed-001 type=payment.charge
  [dl_attempt_process] Attempting to process event: evt-fixed-001
  [dl_finalize_success] Finalizing event: evt-fixed-001


  Status: COMPLETED
  Output: {eventId=evt-fixed-001, processingResult=success, errorReason=errorReason-value}

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
java -jar target/dead-letter-events-1.0.0.jar
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
java -jar target/dead-letter-events-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dead_letter_events_wf \
  --version 1 \
  --input '{"eventId": "evt-fixed-001", "eventType": "payment.charge", "payload": {"amount": 99.99, "currency": "USD"}, "retryCount": 3}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dead_letter_events_wf -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event consumer, dead letter queue (SQS DLQ, Kafka DLT), and alerting service (PagerDuty, Slack), the receive-process-or-DLQ workflow stays exactly the same.

- **DlReceiveEventWorker** (`dl_receive_event`): consume events from your message broker (Kafka, SQS, RabbitMQ) with proper deserialization and metadata extraction
- **DlAttemptProcessWorker** (`dl_attempt_process`): implement your actual event processing logic with schema validation and business rule checks
- **DlRouteToDlqWorker** (`dl_route_to_dlq`): publish failed events to your dead letter queue (Kafka DLQ topic, SQS DLQ, dedicated database table) with failure metadata
- **DlSendAlertWorker** (`dl_send_alert`): send DLQ alerts via PagerDuty, Slack, or OpsGenie with event details and failure reasons
- **DlFinalizeSuccessWorker** (`dl_finalize_success`): commit offsets, acknowledge messages, or update processing status in your event tracking system

Connecting DlRouteToDlqWorker to a real SQS dead-letter queue or DlSendAlertWorker to PagerDuty requires no workflow changes.

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
dead-letter-events/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/deadletterevents/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DeadLetterEventsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DlAttemptProcessWorker.java
│       ├── DlFinalizeSuccessWorker.java
│       ├── DlReceiveEventWorker.java
│       ├── DlRouteToDlqWorker.java
│       └── DlSendAlertWorker.java
└── src/test/java/deadletterevents/workers/
    ├── DlAttemptProcessWorkerTest.java        # 9 tests
    ├── DlFinalizeSuccessWorkerTest.java        # 8 tests
    ├── DlReceiveEventWorkerTest.java        # 9 tests
    ├── DlRouteToDlqWorkerTest.java        # 8 tests
    └── DlSendAlertWorkerTest.java        # 9 tests
```
