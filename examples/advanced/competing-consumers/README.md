# Competing Consumers in Java Using Conductor :  Publish, Compete, Process, Acknowledge

A Java Conductor workflow example for the competing consumers pattern .  publishing a task to a shared queue, having multiple consumer instances race to claim it, processing the task with the winning consumer, and acknowledging completion. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Scaling Throughput with Multiple Consumers

When a single consumer can't keep up with the message rate on a queue, you add more consumers. But multiple consumers reading from the same queue introduces coordination problems: two consumers might grab the same message, a consumer might crash after claiming a message but before processing it, and you need to know which consumer actually handled each task for debugging and audit.

The competing consumers pattern formalizes this: publish a message to a queue, let multiple consumers compete for it (only one wins), process the task with the winner, and acknowledge completion so the message is removed. Getting the compete-process-acknowledge lifecycle right .  especially with retries and crash recovery ,  requires careful coordination that's easy to get wrong in hand-rolled code.

## The Solution

**You write the publish and consume logic. Conductor handles the competition coordination, retries, and audit trail.**

`CcsPublishWorker` places the task payload on the queue and returns a message ID. `CcsCompeteWorker` simulates the consumer competition .  given the consumer count, it determines which consumer instance wins the race to claim the message. `CcsProcessWorker` executes the actual business logic with the winning consumer's context. `CcsAcknowledgeWorker` confirms the message was processed and removes it from the queue. Conductor ensures these steps run in sequence, retries if the processing step fails, and records which consumer won and what result it produced.

### What You Write: Workers

Four workers handle the compete-and-process lifecycle. Publishing to a shared queue, consumer competition, task execution by the winner, and delivery acknowledgment.

| Worker | Task | What It Does |
|---|---|---|
| **CcsAcknowledgeWorker** | `ccs_acknowledge` | Confirms processing is complete and removes the message from the queue |
| **CcsCompeteWorker** | `ccs_compete` | Selects a winning consumer from the pool, recording the claim timestamp |
| **CcsProcessWorker** | `ccs_process` | Executes the message payload using the winning consumer and reports processing time |
| **CcsPublishWorker** | `ccs_publish` | Publishes a message with a unique ID to the shared queue for consumers to compete over |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ccs_publish
    │
    ▼
ccs_compete
    │
    ▼
ccs_process
    │
    ▼
ccs_acknowledge
```

## Example Output

```
=== Competing Consumers Demo ===

Step 1: Registering task definitions...
  Registered: ccs_publish, ccs_compete, ccs_process, ccs_acknowledge

Step 2: Registering workflow 'ccs_competing_consumers'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [ack] Processing
  [compete] Processing
  [process] Processing
  [publish] Processing

  Status: COMPLETED
  Output: {acknowledged=..., removedFromQueue=..., winner=..., competitors=...}

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
java -jar target/competing-consumers-1.0.0.jar
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
java -jar target/competing-consumers-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ccs_competing_consumers \
  --version 1 \
  --input '{"taskPayload": "sample-taskPayload", "image_resize": 5, "queueName": "sample-name", "image_processing": "sample-image-processing", "consumerCount": 5}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ccs_competing_consumers -s COMPLETED -c 5
```

## How to Extend

Each worker manages one phase of the compete-process-acknowledge cycle .  replace the simulated queue operations with real SQS or RabbitMQ consumer group APIs and the competing consumer logic runs unchanged.

- **CcsPublishWorker** (`ccs_publish`): publish to a real SQS queue (`sqs.sendMessage()`), RabbitMQ exchange, or Kafka topic instead of simulating the message send
- **CcsCompeteWorker** (`ccs_compete`): implement real consumer group coordination using Kafka consumer groups, SQS visibility timeout with `receiveMessage()`, or Redis-based distributed locking (Redisson)
- **CcsAcknowledgeWorker** (`ccs_acknowledge`): call `sqs.deleteMessage()` or commit the Kafka offset to actually remove the message from the queue after processing

The competition and acknowledgment contract stays fixed. Only the consumer selection and queue implementation behind each worker changes.

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
competing-consumers/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/competingconsumers/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CompetingConsumersExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CcsAcknowledgeWorker.java
│       ├── CcsCompeteWorker.java
│       ├── CcsProcessWorker.java
│       └── CcsPublishWorker.java
└── src/test/java/competingconsumers/workers/
    ├── CcsAcknowledgeWorkerTest.java        # 4 tests
    ├── CcsCompeteWorkerTest.java        # 4 tests
    ├── CcsProcessWorkerTest.java        # 4 tests
    └── CcsPublishWorkerTest.java        # 4 tests
```
