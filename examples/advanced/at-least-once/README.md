# At-Least-Once Message Delivery in Java Using Conductor :  Receive, Process, Acknowledge, Verify

A Java Conductor workflow example for at-least-once message delivery .  receiving a message from a queue, processing its payload, acknowledging the receipt handle back to the broker, and verifying that delivery was recorded. Uses [Conductor](https://github.

## Guaranteeing Every Message Gets Processed

Message queues like SQS and Kafka deliver messages with a receipt handle and a visibility timeout. If your consumer crashes after processing but before acknowledging, the message reappears on the queue and gets delivered again. If it crashes after acknowledging but before recording delivery, you lose the audit trail. The gap between "processed" and "acknowledged" is where messages get lost or silently duplicated.

Manually coordinating receive, process, acknowledge, and verify steps means tracking receipt handles, managing visibility timeout windows, retrying failed acknowledgments before the message becomes visible again, and logging every outcome for auditability. One missed edge case and you either lose a message or process it without anyone knowing.

## The Solution

**You write the receive and acknowledge logic. Conductor handles retries, delivery verification, and execution history.**

Each stage of the delivery pipeline is a separate worker. `AloReceiveWorker` ingests the message and produces a receipt handle with a visibility timeout. `AloProcessWorker` handles the business logic for the payload. `AloAcknowledgeWorker` deletes the message from the queue using the receipt handle, confirming successful processing. `AloVerifyDeliveryWorker` checks that the acknowledgment was recorded, closing the loop. If any step fails .  the process call times out, the acknowledge call gets a transient error. Conductor retries it automatically, and the full execution history shows exactly where things stand.

### What You Write: Workers

Four workers own the delivery lifecycle: receive, process, acknowledge, and verify, each responsible for one step of the at-least-once guarantee.

| Worker | Task | What It Does |
|---|---|---|
| **AloAcknowledgeWorker** | `alo_acknowledge` | Confirms successful processing by acknowledging the message back to the broker |
| **AloProcessWorker** | `alo_process` | Executes the business logic on the message payload and returns processing results |
| **AloReceiveWorker** | `alo_receive` | Ingests a message from the queue, producing a receipt handle, delivery count, and visibility timeout |
| **AloVerifyDeliveryWorker** | `alo_verify_delivery` | Checks that the acknowledgment was recorded and confirms the at-least-once delivery guarantee |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
alo_receive
    │
    ▼
alo_process
    │
    ▼
alo_acknowledge
    │
    ▼
alo_verify_delivery

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
java -jar target/at-least-once-1.0.0.jar

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
java -jar target/at-least-once-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow alo_at_least_once \
  --version 1 \
  --input '{"messageId": "TEST-001", "payload": {"key": "value"}, "topic": "microservices best practices"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w alo_at_least_once -s COMPLETED -c 5

```

## How to Extend

Each worker owns one step of the receive-process-acknowledge lifecycle .  swap the simulated SQS calls for real queue APIs and the delivery guarantee logic runs unchanged.

- **AloReceiveWorker** (`alo_receive`): pull messages from a real SQS queue (`sqs.receiveMessage()`) or Kafka consumer, returning the actual receipt handle and delivery count
- **AloAcknowledgeWorker** (`alo_acknowledge`): call `sqs.deleteMessage()` with the receipt handle, or commit the Kafka offset, to remove the message from the queue
- **AloVerifyDeliveryWorker** (`alo_verify_delivery`): query a delivery ledger (DynamoDB, PostgreSQL) to confirm the message ID was recorded, enabling end-to-end audit

The output contract stays fixed. Swap simulated SQS calls for real queue APIs and the delivery guarantee logic runs unchanged.

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
at-least-once/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/atleastonce/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AtLeastOnceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AloAcknowledgeWorker.java
│       ├── AloProcessWorker.java
│       ├── AloReceiveWorker.java
│       └── AloVerifyDeliveryWorker.java
└── src/test/java/atleastonce/workers/
    ├── AloAcknowledgeWorkerTest.java        # 4 tests
    ├── AloProcessWorkerTest.java        # 4 tests
    ├── AloReceiveWorkerTest.java        # 4 tests
    └── AloVerifyDeliveryWorkerTest.java        # 4 tests

```
