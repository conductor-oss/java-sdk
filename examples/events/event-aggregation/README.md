# Event Aggregation in Java Using Conductor

Event Aggregation Pipeline: collect events from a time window, aggregate metrics, generate a summary report, and publish the batch. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to aggregate events from a time window into summary metrics. The pipeline must collect all events within a specified window, compute aggregate statistics (counts, sums, averages, percentiles), generate a human-readable summary report, and publish the aggregated batch downstream. Without aggregation, downstream systems are overwhelmed by high-volume raw events; without windowing, you lose temporal context.

Without orchestration, you'd build a stateful aggregation service with in-memory buffers, manual window management, and ad-hoc metric calculations .  handling buffer overflows when event volume spikes, recovering lost state after crashes, and debugging why a window's metrics do not add up.

## The Solution

**You just write the event-collection, metrics-aggregation, summary-generation, and batch-publish workers. Conductor handles window lifecycle management, retry on publish failure, and a durable record of every aggregation window.**

Each aggregation concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (collect, aggregate, summarize, publish), retrying if the downstream publish fails, tracking every aggregation window with full input/output details, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers drive the aggregation pipeline: CollectEventsWorker gathers events from a time window, AggregateMetricsWorker computes totals and averages, GenerateSummaryWorker produces a human-readable report, and PublishBatchWorker sends the aggregated result downstream.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetricsWorker** | `eg_aggregate_metrics` | Aggregates metrics from collected events. Produces fixed totals: totalRevenue=478.47, avgOrderValue=100.70, purchaseC... |
| **CollectEventsWorker** | `eg_collect_events` | Collects events from a time window. Returns a fixed set of 6 transaction events (purchases and refunds) along with th... |
| **GenerateSummaryWorker** | `eg_generate_summary` | Generates a human-readable summary report from the aggregated metrics, including highlights and a destination for the... |
| **PublishBatchWorker** | `eg_publish_batch` | Publishes the aggregated summary batch to the configured destination. Returns a fixed batch ID and publish timestamp. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
eg_collect_events
    │
    ▼
eg_aggregate_metrics
    │
    ▼
eg_generate_summary
    │
    ▼
eg_publish_batch
```

## Example Output

```
=== Event Aggregation Demo ===

Step 1: Registering task definitions...
  Registered: eg_collect_events, eg_aggregate_metrics, eg_generate_summary, eg_publish_batch

Step 2: Registering workflow 'event_aggregation_wf'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [eg_aggregate_metrics] Aggregating
  [eg_collect_events] Collecting events from window:
  [eg_generate_summary] Generating summary for window:
  [eg_publish_batch] Publishing batch to:

  Status: COMPLETED
  Output: {aggregation=..., events=..., eventCount=..., summary=...}

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
java -jar target/event-aggregation-1.0.0.jar
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
java -jar target/event-aggregation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_aggregation_wf \
  --version 1 \
  --input '{"windowId": "win-fixed-001", "win-fixed-001": "windowDurationSec", "windowDurationSec": 60, "transaction-stream": "sample-transaction-stream"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_aggregation_wf -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event store, metrics computation engine, and downstream publish target (Kafka, data warehouse), the collect-aggregate-summarize-publish pipeline workflow stays exactly the same.

- **EgCollectEventsWorker** (`eg_collect_events`): query your event store (Kafka, Elasticsearch, InfluxDB) for events within the specified time window
- **EgAggregateMetricsWorker** (`eg_aggregate_metrics`): compute real-time metrics using streaming libraries (Apache Flink, Spark Structured Streaming) or in-process calculations
- **EgGenerateSummaryWorker** (`eg_generate_summary`): render summary reports as JSON, HTML, or PDF for dashboards and stakeholder consumption
- **EgPublishBatchWorker** (`eg_publish_batch`): publish aggregated results to downstream systems (data warehouse, Kafka topic, REST API, S3)

Pointing CollectEventsWorker at a real event store or PublishBatchWorker at Kafka preserves the collect-aggregate-summarize-publish pipeline.

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
event-aggregation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventaggregation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventAggregationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateMetricsWorker.java
│       ├── CollectEventsWorker.java
│       ├── GenerateSummaryWorker.java
│       └── PublishBatchWorker.java
└── src/test/java/eventaggregation/workers/
    ├── AggregateMetricsWorkerTest.java        # 9 tests
    ├── CollectEventsWorkerTest.java        # 9 tests
    ├── GenerateSummaryWorkerTest.java        # 9 tests
    └── PublishBatchWorkerTest.java        # 9 tests
```
