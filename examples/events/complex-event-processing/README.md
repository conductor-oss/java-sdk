# Complex Event Processing in Java Using Conductor

Complex event processing workflow that ingests events, detects sequences, absences, and timing violations, then routes via SWITCH to trigger alerts or log normal activity. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to analyze streams of events for patterns that signal anomalies. This means ingesting a batch of events, checking whether expected sequences occurred (e.g., login before purchase), detecting the absence of required events (e.g., missing confirmation), and identifying timing violations where gaps between events exceed acceptable thresholds. When any pattern is anomalous, the system must trigger an alert; otherwise, it logs normal activity. Each detection pass depends on the ingested events, and the final routing depends on the combined results.

Without orchestration, you'd build a monolithic event processor that reads from a stream, runs sequence/absence/timing checks in a single loop, and manually routes to alerting or logging with if/else chains .  handling timeouts when the event store is slow, catching exceptions from individual detectors without crashing the whole pipeline, and logging every detection result to investigate false positives.

## The Solution

**You just write the event-ingestion, sequence-detection, absence-detection, timing-detection, and alert workers. Conductor handles multi-detector sequencing, SWITCH-based alert routing, and a complete record of every analysis run.**

Each detection concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (ingest, detect sequence, detect absence, detect timing), then routing via a SWITCH task to either trigger an alert or log normal activity ,  retrying if a detector fails, tracking every analysis run, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Six workers analyze event streams: IngestEventsWorker accepts a batch, DetectSequenceWorker checks for expected ordering, DetectAbsenceWorker flags missing events, DetectTimingWorker catches gap violations, and TriggerAlertWorker or LogNormalWorker handles the outcome.

| Worker | Task | What It Does |
|---|---|---|
| **DetectAbsenceWorker** | `cp_detect_absence` | Detects the absence of a "confirmation" event in the event list. |
| **DetectSequenceWorker** | `cp_detect_sequence` | Detects whether "login" appears before "purchase" in the event sequence. |
| **DetectTimingWorker** | `cp_detect_timing` | Checks if any gap between consecutive event timestamps exceeds maxGapMs. |
| **IngestEventsWorker** | `cp_ingest_events` | Ingests a list of events and passes them through with a count. |
| **LogNormalWorker** | `cp_log_normal` | Logs normal activity when no anomalous patterns are detected. |
| **TriggerAlertWorker** | `cp_trigger_alert` | Triggers an alert when anomalous patterns are detected. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
cp_ingest_events
    │
    ▼
cp_detect_sequence
    │
    ▼
cp_detect_absence
    │
    ▼
cp_detect_timing
    │
    ▼
SWITCH (switch_ref)
    ├── pattern_found: cp_trigger_alert
    └── default: cp_log_normal

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
java -jar target/complex-event-processing-1.0.0.jar

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
java -jar target/complex-event-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow complex_event_processing \
  --version 1 \
  --input '{"events": "sample-events", "patternRules": "sample-patternRules"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w complex_event_processing -s COMPLETED -c 5

```

## How to Extend

Wire each detector worker to your real event stream (Kafka, Kinesis) and alerting system (PagerDuty, OpsGenie), the ingest-detect-alert CEP workflow stays exactly the same.

- **IngestEventsWorker** (`cp_ingest_events`): consume events from Kafka, Kinesis, or Pub/Sub instead of accepting them as workflow input
- **DetectSequenceWorker** (`cp_detect_sequence`): implement configurable sequence rules using a CEP engine (Esper, Apache Flink CEP) for example-grade pattern matching
- **DetectAbsenceWorker** (`cp_detect_absence`): check for missing events against configurable expected-event lists with time-window constraints
- **DetectTimingWorker** (`cp_detect_timing`): enforce SLA-based timing constraints with configurable thresholds per event type
- **TriggerAlertWorker** (`cp_trigger_alert`): send alerts via PagerDuty, OpsGenie, or Slack webhook based on anomaly severity

Replacing the detection algorithms or adding new pattern checks leaves the ingest-detect-route workflow unchanged.

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
complex-event-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/complexeventprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplexEventProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectAbsenceWorker.java
│       ├── DetectSequenceWorker.java
│       ├── DetectTimingWorker.java
│       ├── IngestEventsWorker.java
│       ├── LogNormalWorker.java
│       └── TriggerAlertWorker.java
└── src/test/java/complexeventprocessing/workers/
    ├── DetectAbsenceWorkerTest.java        # 9 tests
    ├── DetectSequenceWorkerTest.java        # 9 tests
    ├── DetectTimingWorkerTest.java        # 9 tests
    ├── IngestEventsWorkerTest.java        # 8 tests
    ├── LogNormalWorkerTest.java        # 8 tests
    └── TriggerAlertWorkerTest.java        # 8 tests

```
