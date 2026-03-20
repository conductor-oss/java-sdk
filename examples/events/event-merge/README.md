# Event Merge in Java Using Conductor

Event merge workflow that collects events from three parallel streams via FORK_JOIN, merges the results, and processes the merged output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to collect events from multiple independent streams and merge them into a single unified dataset. Events from Stream A, Stream B, and Stream C must be collected in parallel (since they are independent), merged into a combined result set preserving source attribution, and then processed as a whole. Sequential collection would multiply latency by the number of streams.

Without orchestration, you'd spawn threads to poll each stream, synchronize with barriers or futures, manually merge results while handling partial failures (one stream is unavailable while others succeed), and deduplicate events that appear in multiple streams.

## The Solution

**You just write the per-stream collection, merge, and processing workers. Conductor handles parallel stream collection, per-stream timeout isolation, and automatic join before merging.**

Each stream consumer is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of collecting from all three streams in parallel via FORK_JOIN, merging the results after all complete, processing the merged output, retrying any failed stream independently, and tracking every merge operation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers merge multi-source data: CollectStreamAWorker, CollectStreamBWorker, and CollectStreamCWorker each poll their independent source in parallel via FORK_JOIN, MergeStreamsWorker combines the results, and ProcessMergedWorker handles the unified dataset.

| Worker | Task | What It Does |
|---|---|---|
| **CollectStreamAWorker** | `mg_collect_stream_a` | Collects events from stream A (API source). |
| **CollectStreamBWorker** | `mg_collect_stream_b` | Collects events from stream B (mobile source). |
| **CollectStreamCWorker** | `mg_collect_stream_c` | Collects events from stream C (IoT source). |
| **MergeStreamsWorker** | `mg_merge_streams` | Merges events from three streams into a single list. |
| **ProcessMergedWorker** | `mg_process_merged` | Processes the merged event list. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
FORK_JOIN
    ├── mg_collect_stream_a
    ├── mg_collect_stream_b
    └── mg_collect_stream_c
    │
    ▼
JOIN (wait for all branches)
mg_merge_streams
    │
    ▼
mg_process_merged
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
java -jar target/event-merge-1.0.0.jar
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
java -jar target/event-merge-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_merge_wf \
  --version 1 \
  --input '{"sourceA": "test-value", "sourceB": "test-value", "sourceC": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_merge_wf -s COMPLETED -c 5
```

## How to Extend

Point each stream collector at your real event sources (API logs, mobile analytics, IoT telemetry), the parallel-collect-merge-process workflow stays exactly the same.

- **Stream collectors**: consume from real event sources (Kafka topics, SQS queues, REST API endpoints, database change streams) for each stream
- **Merge worker**: implement deduplication across streams, conflict resolution for overlapping events, and temporal ordering of merged results
- **Processing worker**: execute business logic on the merged dataset (analytics, aggregation, or forwarding to downstream consumers)

Replacing any stream collector with a real API or message consumer preserves the parallel-collect-merge-process workflow.

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
event-merge/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventmerge/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventMergeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectStreamAWorker.java
│       ├── CollectStreamBWorker.java
│       ├── CollectStreamCWorker.java
│       ├── MergeStreamsWorker.java
│       └── ProcessMergedWorker.java
└── src/test/java/eventmerge/workers/
    ├── CollectStreamAWorkerTest.java        # 8 tests
    ├── CollectStreamBWorkerTest.java        # 8 tests
    ├── CollectStreamCWorkerTest.java        # 8 tests
    ├── MergeStreamsWorkerTest.java        # 8 tests
    └── ProcessMergedWorkerTest.java        # 8 tests
```
