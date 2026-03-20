# Stream Processing in Java Using Conductor :  Event Ingestion, Time Windowing, Window Aggregation, Anomaly Detection, and Result Emission

A Java Conductor workflow example for stream processing with windowed analytics: ingesting a batch of timestamped events, grouping them into configurable time windows (e.g., 5-second or 1-minute tumbling windows), computing per-window aggregates (event counts, averages, distributions), detecting anomalies where window metrics deviate significantly from the norm, and emitting the combined aggregates and anomaly alerts as the final output. Uses [Conductor](https://github.## The Problem

Events are arriving continuously. API requests, sensor readings, user actions, financial transactions, and you need to analyze them in near-real-time using time windows. A configurable window size (say, 60,000ms for 1-minute windows) groups events by time, and each window needs aggregate statistics: how many events occurred, what was the average value, what was the distribution. Then anomaly detection needs to run across windows: a window with 10x the normal event count might indicate a traffic spike or DDoS, while a window with zero events might indicate a service outage. The results: both normal aggregates and anomaly alerts, need to be emitted to downstream consumers.

Without orchestration, you'd write a single stream processor that ingests, windows, aggregates, detects anomalies, and emits in one tight loop. If the anomaly detection algorithm needs tuning, you'd re-ingest and re-window everything. There's no visibility into how many events were ingested vs, how many windows were created vs, how many anomalies were detected. If the process crashes after windowing but before anomaly detection, the windowed results are lost and the entire batch must be reprocessed from scratch.

## The Solution

**You just write the event ingestion, time windowing, per-window aggregation, anomaly detection, and result emission workers. Conductor handles the sequential stream pipeline, per-stage retries, and tracking of event counts, window counts, and anomaly counts at every stage.**

Each stage of the stream pipeline is a simple, independent worker. The stream ingester validates and normalizes the incoming event batch, counting the total events. The windower groups events into time windows based on the configured window size in milliseconds, producing a list of windows each containing their events. The aggregator computes per-window statistics: event count, sum, average, min, max, across all windows. The anomaly detector scans the window aggregates for outliers, flagging windows where metrics deviate significantly from expected ranges (sudden spikes, unexpected drops, unusual distributions). The emitter combines aggregates and anomaly alerts into a final summary for downstream consumption. Conductor executes them in strict sequence, passes the evolving event data between stages, retries if any stage fails, and tracks event counts, window counts, and anomaly counts at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle windowed stream analytics: ingesting timestamped events, grouping them into configurable time windows, computing per-window aggregates (count, average, distribution), detecting anomalies across windows, and emitting the combined results.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWindowsWorker** | `st_aggregate_windows` | Aggregate Windows. Computes and returns aggregates |
| **DetectAnomaliesWorker** | `st_detect_anomalies` | Detect Anomalies. Computes and returns anomalies, anomaly count, global avg |
| **EmitResultsWorker** | `st_emit_results` | Handles emit results |
| **IngestStreamWorker** | `st_ingest_stream` | Ingest Stream. Computes and returns events, event count |
| **WindowEventsWorker** | `st_window_events` | Window Events. Computes and returns windows, window count |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
st_ingest_stream
    │
    ▼
st_window_events
    │
    ▼
st_aggregate_windows
    │
    ▼
st_detect_anomalies
    │
    ▼
st_emit_results
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
java -jar target/stream-processing-1.0.0.jar
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
java -jar target/stream-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow stream_processing \
  --version 1 \
  --input '{"events": "test-value", "windowSizeMs": 10}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w stream_processing -s COMPLETED -c 5
```

## How to Extend

Connect the ingester to Kafka or Kinesis event streams, implement real anomaly detection algorithms for spike and outage detection, and the windowed stream processing workflow runs unchanged.

- **IngestStreamWorker** → consume from real event sources: Kafka consumer groups, AWS Kinesis shards, Google Pub/Sub subscriptions, Apache Pulsar topics, or Redis Streams, with exactly-once processing guarantees
- **WindowEventsWorker** → implement real windowing strategies: tumbling windows (fixed, non-overlapping), sliding windows (overlapping for smoothed metrics), session windows (gap-based for user activity), or custom window functions with late-event handling and watermarks
- **AggregateWindowsWorker** → compute real per-window aggregates: percentiles (P50/P95/P99) using t-digest or HDR histogram, cardinality estimation using HyperLogLog, or custom statistical functions for domain-specific metrics
- **DetectAnomaliesWorker** → implement real anomaly detection: z-score or IQR-based outlier detection, exponential weighted moving average (EWMA) for trend-aware detection, or ML-based anomaly models (Isolation Forest, autoencoders) for complex pattern recognition
- **EmitResultsWorker** → publish results to real sinks: Kafka topics for downstream consumers, Elasticsearch for searchable event analytics, Prometheus/Datadog for metrics dashboards, or webhook endpoints for real-time notifications

Tuning the anomaly detection algorithm or adjusting the window size inside any worker does not affect the ingest-window-aggregate-detect-emit pipeline, as long as window metrics and alert structures are preserved.

**Add new stages** by inserting tasks in `workflow.json`, for example, an event enrichment step that joins events with user/device metadata before windowing, a deduplication step that removes duplicate events by ID, or a compaction step that merges small windows into larger ones for long-term storage.

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
stream-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/streamprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── StreamProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWindowsWorker.java
│       ├── DetectAnomaliesWorker.java
│       ├── EmitResultsWorker.java
│       ├── IngestStreamWorker.java
│       └── WindowEventsWorker.java
└── src/test/java/streamprocessing/workers/
    ├── AggregateWindowsWorkerTest.java        # 8 tests
    ├── DetectAnomaliesWorkerTest.java        # 8 tests
    ├── EmitResultsWorkerTest.java        # 8 tests
    ├── IngestStreamWorkerTest.java        # 8 tests
    └── WindowEventsWorkerTest.java        # 8 tests
```
