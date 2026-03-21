# Event Replay in Java Using Conductor

Event Replay Workflow .  load event history, filter by criteria, replay failed events, and generate a summary report. Uses [Conductor](https://github.

## The Problem

You need to replay failed or historical events from an event stream. The workflow loads event history from a source stream for a given time range, filters events by criteria (e.g., only failed events, specific event types), replays the filtered events through your processing pipeline, and generates a summary report of replay outcomes. Without replay capability, failed events are lost forever and historical reprocessing requires manual intervention.

Without orchestration, you'd write a one-off script to query your event store, filter events, resubmit them to the processing queue, and track which ones succeeded .  manually handling idempotency so replayed events do not cause duplicates, and logging everything to prove the replay was complete.

## The Solution

**You just write the history-load, event-filter, replay, and report-generation workers. Conductor handles replay sequencing, per-event retry during reprocessing, and a durable report of every replay operation.**

Each replay concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of loading history, filtering, replaying, and reporting ,  retrying individual event replays that fail, tracking the entire replay operation, and providing a complete audit trail. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage event replay: LoadHistoryWorker reads from the event store, FilterEventsWorker selects events by criteria, ReplayEventsWorker reprocesses the filtered set, and GenerateReportWorker summarizes replay outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **FilterEventsWorker** | `ep_filter_events` | Filters events based on the provided filter criteria (status and eventType). With status="failed" and eventType="orde... |
| **GenerateReportWorker** | `ep_generate_report` | Generates a summary report from the replay results. Returns reportId, success rate, success/fail counts, and a fixed ... |
| **LoadHistoryWorker** | `ep_load_history` | Loads event history from the specified source stream within the given time range. Returns a fixed set of 6 events rep... |
| **ReplayEventsWorker** | `ep_replay_events` | Replays filtered events. Each event is replayed with a deterministic success status. Returns replay results with repl... |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
ep_load_history
    │
    ▼
ep_filter_events
    │
    ▼
ep_replay_events
    │
    ▼
ep_generate_report

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
java -jar target/event-replay-1.0.0.jar

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
java -jar target/event-replay-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_replay_wf \
  --version 1 \
  --input '{"sourceStream": "api", "startTime": "2026-01-01T00:00:00Z", "endTime": "2026-01-01T00:00:00Z", "filterCriteria": "sample-filterCriteria"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_replay_wf -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real event store (Kafka, EventStoreDB), filtering criteria, and replay target, the load-filter-replay-report workflow stays exactly the same.

- **History loader**: read from your event store (Kafka with offset reset, EventStoreDB, S3 event archives) for the specified time range
- **Filter worker**: apply configurable filter criteria (event type, error status, customer ID, custom predicates)
- **Replay worker**: resubmit events to your processing pipeline with idempotency keys to prevent duplicate side effects
- **Report generator**: generate replay summary with success/failure counts, error categories, and comparison to original processing results

Pointing LoadHistoryWorker at a real Kafka topic or event store requires no changes to the load-filter-replay-report pipeline.

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
event-replay/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventreplay/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventReplayExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FilterEventsWorker.java
│       ├── GenerateReportWorker.java
│       ├── LoadHistoryWorker.java
│       └── ReplayEventsWorker.java
└── src/test/java/eventreplay/workers/
    ├── FilterEventsWorkerTest.java        # 9 tests
    ├── GenerateReportWorkerTest.java        # 9 tests
    ├── LoadHistoryWorkerTest.java        # 8 tests
    └── ReplayEventsWorkerTest.java        # 8 tests

```
