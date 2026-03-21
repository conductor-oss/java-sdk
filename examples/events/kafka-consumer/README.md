# Kafka Consumer in Java Using Conductor

Kafka consumer pipeline: receives a message, deserializes it, processes the payload, and commits the offset. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process messages from a Kafka topic reliably. Each message must be received from a specific topic/partition/offset, deserialized from its wire format, processed according to your business logic, and have its offset committed only after successful processing. Committing the offset before processing is complete means you lose the message on failure; not committing means you reprocess it on restart.

Without orchestration, you'd write a Kafka consumer loop with manual offset management, deserialization try/catch, processing logic, and commit calls .  handling rebalances, serialization errors, and exactly-once semantics with ad-hoc code that becomes increasingly fragile as message formats evolve.

## The Solution

**You just write the message-receive, deserialize, process, and offset-commit workers. Conductor handles receive-to-commit sequencing, guaranteed offset commit only after processing, and per-message lifecycle tracking.**

Each Kafka consumption concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of receiving the message, deserializing it, processing the payload, and committing the offset ,  retrying on transient failures, tracking every message's processing lifecycle, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the Kafka consumption pipeline: ReceiveMessage extracts the raw message, Deserialize converts it to structured data, ProcessPayload applies business logic, and CommitOffset marks the message as consumed only after successful processing.

| Worker | Task | What It Does |
|---|---|---|
| **CommitOffset** | `kc_commit_offset` | Commits the Kafka consumer offset after successful message processing. |
| **Deserialize** | `kc_deserialize` | Deserializes a raw Kafka message into structured data with type and schema information. |
| **ProcessPayload** | `kc_process_payload` | Processes the deserialized Kafka message payload and applies the relevant action. |
| **ReceiveMessage** | `kc_receive_message` | Receives a Kafka message and wraps it in a raw message envelope with headers and timestamp. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
kc_receive_message
    │
    ▼
kc_deserialize
    │
    ▼
kc_process_payload
    │
    ▼
kc_commit_offset

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
java -jar target/kafka-consumer-1.0.0.jar

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
java -jar target/kafka-consumer-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow kafka_consumer_wf \
  --version 1 \
  --input '{"topic": "microservices best practices", "partition": "sample-partition", "offset": "sample-offset", "messageKey": "Process this order for customer C-100", "messageValue": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w kafka_consumer_wf -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real Kafka cluster, Avro/Protobuf deserializer (with Schema Registry), and consumer group offset management, the receive-deserialize-process-commit pipeline workflow stays exactly the same.

- **Message receiver**: consume from real Kafka topics using the Kafka Consumer API with consumer group coordination
- **Deserializer**: deserialize using Avro (with Schema Registry), Protobuf, or JSON Schema with proper error handling for malformed messages
- **Processor**: implement your actual message processing logic (database writes, API calls, event emission)
- **Offset committer**: commit offsets to Kafka with exactly-once semantics using transactional producers or idempotent processing

Connecting ReceiveMessage to a real Kafka consumer client or ProcessPayload to live business logic preserves the four-step consumption pipeline.

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
kafka-consumer/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/kafkaconsumer/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── KafkaConsumerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CommitOffset.java
│       ├── Deserialize.java
│       ├── ProcessPayload.java
│       └── ReceiveMessage.java
└── src/test/java/kafkaconsumer/workers/
    ├── CommitOffsetTest.java        # 9 tests
    ├── DeserializeTest.java        # 8 tests
    ├── ProcessPayloadTest.java        # 9 tests
    └── ReceiveMessageTest.java        # 9 tests

```
