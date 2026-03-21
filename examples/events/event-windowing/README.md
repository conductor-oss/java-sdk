# Event Windowing in Java Using Conductor

Event Windowing. collect events into a time window, compute aggregate statistics, and emit the windowed result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to group events into time-based windows and compute aggregate statistics per window. Events arrive continuously, but analysis requires bounded windows. computing event count, sum, average, min, max, and standard deviation across all events within a configurable time window (e.g., 5 seconds, 1 minute). Without windowing, you either process events one at a time (losing temporal context) or accumulate unbounded state.

Without orchestration, you'd manage window boundaries with timers, buffer events in memory, trigger window closure on timeout, compute aggregates inline, and handle late-arriving events that fall into an already-closed window.

## The Solution

**You just write the window-collection, stats-computation, and result-emission workers. Conductor handles window lifecycle management, retry on emission failure, and a durable record of every windowed computation.**

Each windowing concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting events into the window, computing aggregates, and emitting the windowed result,  retrying if aggregation fails, tracking every window computation, and resuming if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers implement time-based windowing: CollectWindowWorker gathers events within a configurable window, ComputeStatsWorker calculates aggregate statistics (count, sum, average, min, max), and EmitResultWorker publishes the windowed output.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWindowWorker** | `ew_collect_window` | Collects incoming events into a fixed time window. Passes through all events that fall within the configured window, ... |
| **ComputeStatsWorker** | `ew_compute_stats` | Computes aggregate statistics (count, min, max, sum, avg) over the value field of windowed events. Returns fixed dete... |
| **EmitResultWorker** | `ew_emit_result` | Emits the final windowed result, confirming the window was successfully processed and published. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ew_collect_window
    │
    ▼
ew_compute_stats
    │
    ▼
ew_emit_result

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
java -jar target/event-windowing-1.0.0.jar

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
java -jar target/event-windowing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_windowing \
  --version 1 \
  --input '{"events": "sample-events", "windowSizeMs": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_windowing -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real event stream (Kafka, Kinesis), aggregation engine, and time-series output (InfluxDB, Grafana), the collect-compute-emit windowing workflow stays exactly the same.

- **Window collector**: consume events from Kafka/Kinesis with watermark-based window closure and late-event policies
- **Aggregator**: compute real-time aggregates using streaming libraries (Flink, Spark) or optimized in-process calculations with reservoir sampling
- **Result emitter**: publish windowed aggregates to time-series databases (InfluxDB, TimescaleDB), dashboards (Grafana), or downstream analytics pipelines

Changing the window duration or statistics computation preserves the collect-compute-emit windowing pipeline.

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
event-windowing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventwindowing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventWindowingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWindowWorker.java
│       ├── ComputeStatsWorker.java
│       └── EmitResultWorker.java
└── src/test/java/eventwindowing/workers/
    ├── CollectWindowWorkerTest.java        # 9 tests
    ├── ComputeStatsWorkerTest.java        # 10 tests
    └── EmitResultWorkerTest.java        # 8 tests

```
