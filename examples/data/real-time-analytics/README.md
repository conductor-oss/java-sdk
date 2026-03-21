# Real-Time Analytics in Java Using Conductor :  Event Ingestion, Windowed Stream Processing, Aggregate Updates, and Alert Checking

A Java Conductor workflow example for real-time analytics: ingesting a batch of events, processing them within a configurable time window to compute windowed metrics and flag anomalies, updating running aggregates with the latest window results, and evaluating alert rules against the current aggregates to trigger notifications when thresholds are breached. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Events are arriving in batches from your application: page views, API calls, purchases, error occurrences, and you need analytics that reflect what's happening right now, not what happened yesterday. Each batch of events needs to be processed within a time window (last 5 minutes, last hour) to compute windowed metrics like requests per second, error rate, and 95th percentile latency. Those window metrics need to update running aggregates so dashboards show cumulative trends. And when an aggregate crosses a threshold, error rate exceeds 5%, latency spikes above 500ms, purchase volume drops below normal, alert rules need to fire immediately, not after a nightly batch job.

Without orchestration, you'd write a single event processor that ingests, windows, aggregates, and alerts in one loop. If the aggregate store is temporarily unavailable, the entire pipeline stops. Events back up, window metrics are lost, and alerts never fire. There's no visibility into how many events were ingested vs: processed, whether the window computation is the bottleneck, or which alert rules were evaluated. Adding a new alert rule or changing the window size means modifying the same tightly coupled loop that handles ingestion.

## The Solution

**You just write the event ingestion, windowed processing, aggregate updating, and alert checking workers. Conductor handles the ingest-window-aggregate-alert pipeline, retries when aggregate stores are temporarily unavailable, and event count tracking at every stage.**

Each stage of the analytics pipeline is a simple, independent worker. The event ingester validates and normalizes the incoming event batch, counting the events for tracking. The stream processor groups events by the configured window size, computes windowed metrics (event rate, error rate, distribution statistics), and flags anomalous windows. The aggregate updater merges the latest window metrics into the running aggregates, maintaining cumulative counters and moving averages. The alert checker evaluates the configured alert rules against current aggregates and triggers alerts when thresholds are breached. Conductor executes them in strict sequence, passes events through the pipeline, retries if the aggregate store is temporarily unavailable, and tracks event counts, processed counts, and alert counts at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers power the real-time analytics pipeline: ingesting and normalizing event batches, processing events within configurable time windows, updating running aggregates, and evaluating alert rules to trigger notifications when thresholds are breached.

| Worker | Task | What It Does |
|---|---|---|
| **CheckAlertsWorker** | `ry_check_alerts` | Checks alert rules against current aggregates and triggers alerts. |
| **IngestEventsWorker** | `ry_ingest_events` | Ingests a batch of events for real-time analytics processing. |
| **ProcessStreamWorker** | `ry_process_stream` | Processes the event stream: computes window metrics, flags anomalies. |
| **UpdateAggregatesWorker** | `ry_update_aggregates` | Updates running aggregates with the latest window metrics. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
ry_ingest_events
    │
    ▼
ry_process_stream
    │
    ▼
ry_update_aggregates
    │
    ▼
ry_check_alerts

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
java -jar target/real-time-analytics-1.0.0.jar

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
java -jar target/real-time-analytics-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow real_time_analytics \
  --version 1 \
  --input '{"eventBatch": "sample-eventBatch", "windowSize": 10, "alertRules": "sample-alertRules"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w real_time_analytics -s COMPLETED -c 5

```

## How to Extend

Connect the ingester to a real event stream from Kafka or Kinesis, update running aggregates in Redis, and fire alerts via PagerDuty or Slack, the real-time analytics workflow runs unchanged.

- **IngestEventsWorker** → consume from real event sources: Kafka topic consumer, AWS Kinesis stream reader, Google Pub/Sub subscriber, or webhook receiver, with schema validation and deduplication
- **ProcessStreamWorker** → implement real windowed processing: tumbling/sliding/session windows with Apache Flink-style semantics, percentile computation (P50, P95, P99), anomaly detection using z-score or exponential moving average deviation
- **UpdateAggregatesWorker** → write to real aggregate stores: Redis for low-latency counters, TimescaleDB for time-series aggregates, ClickHouse for analytical queries, or Prometheus for metrics exposure
- **CheckAlertsWorker** → evaluate rules against real alerting systems: send Slack/PagerDuty/OpsGenie notifications, support complex alert conditions (sustained threshold breaches, rate-of-change alerts, composite alerts across multiple metrics)

Changing the window size, adding new alert rules, or connecting to a real aggregate store requires no workflow changes, provided each worker outputs the expected metric and alert structures.

**Add new stages** by inserting tasks in `workflow.json`, for example, an enrichment step that adds user/session context to raw events before processing, a dashboard push step that updates Grafana or Datadog dashboards with the latest aggregates, or a replay step that reprocesses historical events when alert rules change.

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
real-time-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/realtimeanalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RealTimeAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckAlertsWorker.java
│       ├── IngestEventsWorker.java
│       ├── ProcessStreamWorker.java
│       └── UpdateAggregatesWorker.java
└── src/test/java/realtimeanalytics/workers/
    ├── CheckAlertsWorkerTest.java        # 6 tests
    ├── IngestEventsWorkerTest.java        # 5 tests
    ├── ProcessStreamWorkerTest.java        # 5 tests
    └── UpdateAggregatesWorkerTest.java        # 4 tests

```
