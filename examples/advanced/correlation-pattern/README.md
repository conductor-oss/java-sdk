# Message Correlation in Java Using Conductor :  Group Related Messages by ID and Process Together

A Java Conductor workflow example for message correlation. receiving a batch of messages from different sources, matching them by a shared correlation field (order ID, session ID, transaction ID), aggregating the correlated groups, and processing each group as a unit. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Linking Messages That Belong Together

A single customer order generates events from the payment service, the inventory service, and the shipping service. Each event arrives independently with its own schema, but they all share an order ID. To build a complete order view. payment confirmed, items reserved, label printed,  you need to match these messages by their correlation field, group them, and process each group as a whole.

Without orchestration, you'd build a stateful correlator that buffers incoming messages, maintains a lookup table keyed by the correlation field, handles late-arriving messages, and decides when a group is complete enough to process. That's a lot of in-memory state management, timeout logic, and concurrency control for what's fundamentally a match-and-group operation.

## The Solution

**You write the matching and aggregation logic. Conductor handles sequencing, retries, and correlation tracking.**

`CrpReceiveMessagesWorker` ingests the batch of messages and counts them. `CrpMatchByIdWorker` groups the messages by the specified correlation field (e.g., `orderId`), producing correlated groups where each group contains all messages sharing the same ID. `CrpAggregateWorker` combines each group into a single aggregated result. merging payment status with inventory status with shipping status. `CrpProcessWorker` acts on the aggregated groups, producing a final outcome per correlated set. Conductor tracks how many messages were received, how many groups were formed, and how many were successfully processed.

### What You Write: Workers

Four workers handle the match-and-group flow: message ingestion, correlation-ID matching, per-group aggregation, and group processing, each isolated from the others' data schemas.

| Worker | Task | What It Does |
|---|---|---|
| **CrpAggregateWorker** | `crp_aggregate` | Produces aggregated results for each correlated group, reporting completeness status |
| **CrpMatchByIdWorker** | `crp_match_by_id` | Groups messages by correlation ID and counts the number of correlated groups |
| **CrpProcessWorker** | `crp_process` | Processes each correlated group and produces per-group outcome records |
| **CrpReceiveMessagesWorker** | `crp_receive_messages` | Ingests incoming messages from input and counts the total for downstream matching |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
crp_receive_messages
    │
    ▼
crp_match_by_id
    │
    ▼
crp_aggregate
    │
    ▼
crp_process

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
java -jar target/correlation-pattern-1.0.0.jar

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
java -jar target/correlation-pattern-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow crp_correlation_pattern \
  --version 1 \
  --input '{"messages": "Process this order for customer C-100", "correlationField": "sample-correlationField"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w crp_correlation_pattern -s COMPLETED -c 5

```

## How to Extend

Each worker tackles one correlation concern. replace the simulated message matching with real Kafka or event stream consumers and the group-and-process logic runs unchanged.

- **CrpReceiveMessagesWorker** (`crp_receive_messages`): consume real messages from Kafka, SQS, or a webhook endpoint instead of accepting them as workflow input
- **CrpMatchByIdWorker** (`crp_match_by_id`): look up additional context from a database while correlating, or use Redis to track cross-workflow correlation state for messages arriving in separate workflow runs
- **CrpProcessWorker** (`crp_process`): write the fully-correlated order/session/transaction view to a data warehouse (BigQuery, Redshift) or trigger downstream workflows per correlated group

The correlated-group output contract stays fixed. Swap the simulated message source for a real Kafka or SNS consumer and the match-aggregate-process pipeline runs unchanged.

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
correlation-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/correlationpattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CorrelationPatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CrpAggregateWorker.java
│       ├── CrpMatchByIdWorker.java
│       ├── CrpProcessWorker.java
│       └── CrpReceiveMessagesWorker.java
└── src/test/java/correlationpattern/workers/
    ├── CrpAggregateWorkerTest.java        # 4 tests
    ├── CrpMatchByIdWorkerTest.java        # 4 tests
    ├── CrpProcessWorkerTest.java        # 4 tests
    └── CrpReceiveMessagesWorkerTest.java        # 4 tests

```
