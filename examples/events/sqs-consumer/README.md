# SQS Consumer in Java Using Conductor

SQS Consumer. receive an SQS message, validate it, process the event, and delete the message from the queue. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process messages from an AWS SQS queue. Each message must be received, validated for correct structure, processed according to your business logic, and deleted from the queue only after successful processing. If you delete before processing, you lose the message on failure; if you do not delete after success, SQS redelivers it after the visibility timeout expires.

Without orchestration, you'd write an SQS polling loop with ReceiveMessage, validation logic, processing, and DeleteMessage calls. handling visibility timeout extensions for slow processing, managing long polling, and dealing with poison messages that repeatedly fail and block the queue.

## The Solution

**You just write the message-receive, validate, process, and queue-delete workers. Conductor handles receive-to-delete sequencing, guaranteed queue deletion only after processing, and per-message audit trail.**

Each SQS consumption concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the message, validating it, processing the event, and deleting it from the queue,  retrying on transient failures, tracking every message's lifecycle, and resuming if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers process SQS messages: ReceiveMessageWorker parses the SQS message body, ValidateMessageWorker checks required fields, ProcessMessageWorker applies business logic, and DeleteMessageWorker removes the message from the queue after success.

| Worker | Task | What It Does |
|---|---|---|
| **DeleteMessageWorker** | `qs_delete_message` | Deletes an SQS message from the queue after successful processing, confirming deletion with a timestamp. |
| **ProcessMessageWorker** | `qs_process_message` | Processes a validated SQS message based on its type. For invoice.generated events, records the invoice and returns th... |
| **ReceiveMessageWorker** | `qs_receive_message` | Receives and parses an SQS message body into structured data, extracting the event payload and SQS message attributes. |
| **ValidateMessageWorker** | `qs_validate_message` | Validates an SQS message payload by checking required fields (eventType, invoiceId, customerId, amount) and confirmin... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
qs_receive_message
    │
    ▼
qs_validate_message
    │
    ▼
qs_process_message
    │
    ▼
qs_delete_message

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
java -jar target/sqs-consumer-1.0.0.jar

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
java -jar target/sqs-consumer-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sqs_consumer_wf \
  --version 1 \
  --input '{"queueUrl": "https://example.com", "messageId": "TEST-001", "receiptHandle": "sample-receiptHandle", "body": "sample-body"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sqs_consumer_wf -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real AWS SQS queue, message validation schema, and business processing logic, the receive-validate-process-delete consumer workflow stays exactly the same.

- **Message receiver**: receive from real SQS queues using the AWS SDK with long polling and batch receive for efficiency
- **Validator**: validate message structure against your schema and check for required fields before processing
- **Processor**: implement your actual message processing logic (database writes, API calls, event emission)
- **Message deleter**: delete processed messages from SQS using the receipt handle; route failures to the SQS dead-letter queue

Connecting ReceiveMessageWorker to a real SQS client or ProcessMessageWorker to live invoice processing preserves the consumption pipeline.

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
sqs-consumer/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sqsconsumer/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SqsConsumerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeleteMessageWorker.java
│       ├── ProcessMessageWorker.java
│       ├── ReceiveMessageWorker.java
│       └── ValidateMessageWorker.java
└── src/test/java/sqsconsumer/workers/
    ├── DeleteMessageWorkerTest.java        # 9 tests
    ├── ProcessMessageWorkerTest.java        # 9 tests
    ├── ReceiveMessageWorkerTest.java        # 8 tests
    └── ValidateMessageWorkerTest.java        # 10 tests

```
