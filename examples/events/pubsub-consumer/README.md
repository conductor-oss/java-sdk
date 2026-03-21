# Pubsub Consumer in Java Using Conductor

Pub/Sub Consumer. receive a Pub/Sub message, decode the base64 payload, process sensor data with threshold checks, and acknowledge the message. Uses [Conductor](https://github.

## The Problem

You need to process messages from a Google Cloud Pub/Sub subscription. Each message arrives base64-encoded with attributes metadata, must be decoded, processed (e.g., sensor data with threshold checks), and acknowledged so Pub/Sub stops redelivering it. Failing to acknowledge means the message is redelivered indefinitely; acknowledging before processing means you lose it on failure.

Without orchestration, you'd build a Pub/Sub subscriber with manual message decoding, threshold logic, and acknowledge/nack calls. handling message lease extensions for slow processing, managing subscriber flow control, and debugging why messages are being redelivered.

## The Solution

**You just write the message-receive, payload-decode, sensor-processing, and acknowledgment workers. Conductor handles receive-to-ack sequencing, guaranteed acknowledgment only after processing, and full message lifecycle visibility.**

Each Pub/Sub consumption concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the message, decoding the base64 payload, processing the sensor data with threshold checks, and acknowledging the message,  retrying on transient failures, tracking every message's lifecycle, and resuming if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers process Pub/Sub messages: PsReceiveMessageWorker extracts the raw message, PsDecodePayloadWorker converts the base64 payload into structured sensor data, PsProcessDataWorker evaluates threshold alerts, and PsAckMessageWorker confirms delivery.

| Worker | Task | What It Does |
|---|---|---|
| **PsAckMessageWorker** | `ps_ack_message` | Acknowledges a Pub/Sub message after successful processing. Records the subscription, message ID, and acknowledgement... |
| **PsDecodePayloadWorker** | `ps_decode_payload` | Decodes a base64-encoded Pub/Sub message payload into structured sensor data. Determines event type from message attr... |
| **PsProcessDataWorker** | `ps_process_data` | Processes decoded sensor data by checking thresholds and generating alerts. Thresholds: temperature high=85, temperat... |
| **PsReceiveMessageWorker** | `ps_receive_message` | Receives a Pub/Sub message and extracts its data, encoding, and attributes. The raw data is passed through as encoded... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ps_receive_message
    │
    ▼
ps_decode_payload
    │
    ▼
ps_process_data
    │
    ▼
ps_ack_message

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
java -jar target/pubsub-consumer-1.0.0.jar

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
java -jar target/pubsub-consumer-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pubsub_consumer_wf \
  --version 1 \
  --input '{"subscription": "sample-subscription", "messageId": "TEST-001", "publishTime": "2026-01-01T00:00:00Z", "data": {"key": "value"}, "attributes": "sample-attributes"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pubsub_consumer_wf -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real Google Cloud Pub/Sub subscription, payload decoder, and sensor-data processing pipeline, the receive-decode-process-acknowledge workflow stays exactly the same.

- **Message receiver**: consume from real Pub/Sub subscriptions using the Google Cloud Pub/Sub client library with flow control
- **Payload decoder**: decode base64 payloads and deserialize sensor data (JSON, Protobuf, Avro) with schema validation
- **Processor**: implement real threshold checks, anomaly detection, or data transformation for sensor data
- **Acknowledger**: acknowledge messages via the Pub/Sub API only after successful processing; nack for retry on failure

Pointing PsDecodePayloadWorker at real sensor payloads or PsAckMessageWorker at the Pub/Sub API requires no workflow modifications.

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
pubsub-consumer/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/pubsubconsumer/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PubsubConsumerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PsAckMessageWorker.java
│       ├── PsDecodePayloadWorker.java
│       ├── PsProcessDataWorker.java
│       └── PsReceiveMessageWorker.java
└── src/test/java/pubsubconsumer/workers/
    ├── PsAckMessageWorkerTest.java        # 9 tests
    ├── PsDecodePayloadWorkerTest.java        # 9 tests
    ├── PsProcessDataWorkerTest.java        # 10 tests
    └── PsReceiveMessageWorkerTest.java        # 9 tests

```
