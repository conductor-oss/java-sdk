# Message Aggregation in Java Using Conductor :  Collect, Combine, and Forward Correlated Messages

A Java Conductor workflow example for message aggregation. collecting related messages that arrive independently, checking for completeness, computing a combined result (totals, counts, summaries), and forwarding the aggregated payload downstream. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Message Aggregation Needs Orchestration

In distributed systems, a single business event often produces multiple messages. an order generates a payment confirmation, an inventory reservation, and a shipping request. These messages arrive at different times from different services, and you need to collect all of them before you can compute a meaningful aggregate (total order value, item count, combined status) and forward it to the next stage.

Without orchestration, you'd build a stateful collector service that tracks which messages have arrived, implements timeout logic for stragglers, handles duplicates, computes the aggregate once the set is complete, and retries the downstream forwarding if it fails. That's a lot of state management, concurrency control, and failure handling bolted onto what should be a straightforward collect-and-combine operation.

## The Solution

**You write the collection and aggregation logic. Conductor handles sequencing, retries, and execution tracking.**

Each stage of the aggregation pipeline is a simple, independent worker. `AgpCollectWorker` gathers incoming messages and counts them. `AgpCheckCompleteWorker` compares the collected count against the expected total to determine if the full set has arrived. `AgpAggregateWorker` computes the combined result. totals, summaries, timestamps. `AgpForwardWorker` delivers the aggregated payload to the downstream consumer. Conductor sequences them, retries any that fail, and tracks every execution so you can see exactly which messages were collected and what aggregate was produced.

### What You Write: Workers

Four workers form the collect-and-combine pipeline: collection, completeness check, aggregation, and forwarding, each handling one stage of the message lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AgpAggregateWorker** | `agp_aggregate` | Computes combined totals (message count, total amount) from collected messages and produces the aggregated result |
| **AgpCheckCompleteWorker** | `agp_check_complete` | Compares collected message count against expected total to determine if the full set has arrived |
| **AgpCollectWorker** | `agp_collect` | Gathers incoming messages from input and counts them for completeness checking |
| **AgpForwardWorker** | `agp_forward` | Delivers the aggregated payload to the downstream processor with a forwarding timestamp |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
agp_collect
    │
    ▼
agp_check_complete
    │
    ▼
agp_aggregate
    │
    ▼
agp_forward

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
java -jar target/aggregator-pattern-1.0.0.jar

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
java -jar target/aggregator-pattern-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow agp_aggregator_pattern \
  --version 1 \
  --input '{"messages": [{"source": "sensor-1", "value": 23.5}, {"source": "sensor-2", "value": 24.1}], "aggregationType": "average", "windowSize": 5}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agp_aggregator_pattern -s COMPLETED -c 5

```

## How to Extend

Each worker handles one stage of the collect-and-combine pipeline. replace the simulated message ingestion with a real Kafka consumer or SQS reader and the aggregation logic runs unchanged.

- **AgpCollectWorker** (`agp_collect`): read messages from a real message broker (Kafka consumer, SQS `ReceiveMessage`, RabbitMQ) instead of accepting them as workflow input
- **AgpAggregateWorker** (`agp_aggregate`): replace the hardcoded totals with real aggregation logic: summing transaction amounts from a database, computing averages across sensor readings, or merging partial search results
- **AgpForwardWorker** (`agp_forward`): publish the aggregated result to a downstream Kafka topic, POST it to a webhook, or write it to a data warehouse (BigQuery, Redshift)

Each worker preserves the same output contract, so swapping simulated ingestion for a real Kafka consumer or SQS reader requires zero workflow changes.

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
aggregator-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aggregatorpattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AggregatorPatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AgpAggregateWorker.java
│       ├── AgpCheckCompleteWorker.java
│       ├── AgpCollectWorker.java
│       └── AgpForwardWorker.java
└── src/test/java/aggregatorpattern/workers/
    ├── AgpAggregateWorkerTest.java        # 4 tests
    ├── AgpCheckCompleteWorkerTest.java        # 4 tests
    ├── AgpCollectWorkerTest.java        # 4 tests
    └── AgpForwardWorkerTest.java        # 4 tests

```
