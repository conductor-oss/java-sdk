# Message Broker Pipeline in Java Using Conductor :  Receive, Route, Deliver, Acknowledge, Log

A Java Conductor workflow example for message brokering .  receiving a message with topic and priority metadata, routing it to the correct destination based on topic rules, delivering the payload to the target subscriber, acknowledging successful delivery, and logging the transaction for audit. Uses [Conductor](https://github.## Messages Need Reliable Routing, Not Just Transport

A message arrives on the `orders` topic with `high` priority. It needs to be routed to the order processing service, not the analytics pipeline. Another message arrives on `notifications` with `low` priority .  it should go to the batch notification queue, not the real-time push service. Topic-based routing, priority handling, delivery confirmation, and audit logging are the core responsibilities of a message broker.

Building this manually means writing routing tables, implementing delivery retries with backoff for each subscriber, tracking which messages were acknowledged and which need redelivery, and maintaining an audit log of every message's journey. When a delivery fails, you need to know the message ID, topic, priority, routing decision, and delivery attempt count .  not just "something went wrong."

## The Solution

**You write the routing and delivery logic. Conductor handles the message lifecycle, retries, and audit logging.**

`MbrReceiveWorker` ingests the message and extracts its topic and priority metadata. `MbrRouteWorker` determines the delivery target based on topic routing rules and priority level. `MbrDeliverWorker` sends the payload to the routed destination. `MbrAcknowledgeWorker` confirms successful delivery and records the acknowledgment. `MbrLogWorker` writes the complete message lifecycle .  receive, route decision, delivery, acknowledgment ,  to the audit log. Conductor ensures this five-step pipeline runs in sequence, retries failed deliveries, and gives you full traceability for every message.

### What You Write: Workers

Five workers manage the brokering lifecycle: message reception, topic-based routing, payload delivery, acknowledgment, and audit logging, each owning one phase of reliable message transit.

| Worker | Task | What It Does |
|---|---|---|
| **MbrAcknowledgeWorker** | `mbr_acknowledge` | Confirms delivery was successful and acknowledges the message back to the broker |
| **MbrDeliverWorker** | `mbr_deliver` | Delivers the message to the resolved destination and reports delivery latency |
| **MbrLogWorker** | `mbr_log` | Records the message transit in an audit log with a unique log ID |
| **MbrReceiveWorker** | `mbr_receive` | Ingests an incoming message, assigning a message ID and receive timestamp |
| **MbrRouteWorker** | `mbr_route` | Resolves the destination for the message based on its topic and records routing time |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
mbr_receive
    │
    ▼
mbr_route
    │
    ▼
mbr_deliver
    │
    ▼
mbr_acknowledge
    │
    ▼
mbr_log
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
java -jar target/message-broker-1.0.0.jar
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
java -jar target/message-broker-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mbr_message_broker \
  --version 1 \
  --input '{"message": "test-value", "topic": "test-value", "priority": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mbr_message_broker -s COMPLETED -c 5
```

## How to Extend

Each worker handles one brokering responsibility .  replace the simulated topic routing with real RabbitMQ exchange bindings or Kafka partition logic and the route-deliver-acknowledge pipeline runs unchanged.

- **MbrRouteWorker** (`mbr_route`): implement real topic-based routing using a routing table in Redis or a database, or integrate with RabbitMQ exchange bindings or Kafka topic partitioning
- **MbrDeliverWorker** (`mbr_deliver`): publish to real subscriber endpoints via HTTP POST (webhooks), Kafka `producer.send()`, SQS `sendMessage()`, or gRPC streaming
- **MbrLogWorker** (`mbr_log`): write audit records to Elasticsearch for searchable message history, or CloudWatch Logs / Splunk for compliance tracking

The routing and acknowledgment contract stays fixed. Swap simulated topic resolution for real RabbitMQ exchanges or Kafka topic routing and the deliver-acknowledge-log pipeline runs unchanged.

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
message-broker/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/messagebroker/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MessageBrokerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MbrAcknowledgeWorker.java
│       ├── MbrDeliverWorker.java
│       ├── MbrLogWorker.java
│       ├── MbrReceiveWorker.java
│       └── MbrRouteWorker.java
└── src/test/java/messagebroker/workers/
    ├── MbrAcknowledgeWorkerTest.java        # 4 tests
    ├── MbrDeliverWorkerTest.java        # 4 tests
    ├── MbrLogWorkerTest.java        # 4 tests
    ├── MbrReceiveWorkerTest.java        # 4 tests
    └── MbrRouteWorkerTest.java        # 4 tests
```
