# Distributed Trace Collection in Java Using Conductor :  Instrument, Collect Spans, Assemble Traces, and Store

A Java Conductor workflow example for distributed trace collection. instrumenting services, collecting spans from each service, assembling complete traces, and storing them in a trace backend for analysis.

## The Problem

Your microservices produce distributed traces. spans from each service that must be collected, assembled into complete request traces, and stored for debugging and performance analysis. Instrumentation must be configured, spans collected from each service's collector, assembled by trace ID into a full picture of the request path, and stored in a durable trace backend.

Without orchestration, trace collection relies entirely on sidecar agents and backend infrastructure (Jaeger, Zipkin). When instrumentation fails for one service, traces are incomplete. When the backend is overloaded, spans are dropped. There's no pipeline to verify instrumentation, validate span completeness, or manage trace sampling rates.

## The Solution

**You just write the span collection and trace assembly logic. Conductor handles the instrument-collect-assemble-store pipeline, retries when span collectors or trace backends are temporarily unavailable, and a complete record of every collection run with span counts and trace completeness.**

Each trace collection step is an independent worker. instrumentation, span collection, trace assembly, and storage. Conductor runs them in sequence: configure instrumentation, collect spans, assemble into traces, then store. Every collection run is tracked with span counts, trace completeness, and storage confirmation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

InstrumentWorker configures tracing on target services, CollectSpansWorker gathers individual spans, AssembleTraceWorker stitches them into complete request traces, and StoreTraceWorker persists the assembled traces to Jaeger or your preferred backend.

| Worker | Task | What It Does |
|---|---|---|
| **AssembleTraceWorker** | `trc_assemble_trace` | Assembles collected spans into a complete trace, computing trace depth and total request duration |
| **CollectSpansWorker** | `trc_collect_spans` | Collects individual spans from instrumented services and returns the total span count |
| **InstrumentWorker** | `trc_instrument` | Configures tracing instrumentation on target services with the specified sampling rate, returning the count of instrumented services |
| **StoreTraceWorker** | `trc_store_trace` | Persists assembled traces to the trace backend (e.g., Jaeger) for querying and analysis |

Workers implement scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic. the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
trc_instrument
    │
    ▼
trc_collect_spans
    │
    ▼
trc_assemble_trace
    │
    ▼
trc_store_trace

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
java -jar target/trace-collection-1.0.0.jar

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
java -jar target/trace-collection-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow trace_collection_416 \
  --version 1 \
  --input '{"serviceName": "test", "traceId": "TEST-001", "samplingRate": "sample-samplingRate"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w trace_collection_416 -s COMPLETED -c 5

```

## How to Extend

Each worker handles one tracing step. connect the span collector to your OpenTelemetry agents, the storage worker to Jaeger or Zipkin, and the instrument-collect-assemble-store workflow stays the same.

- **AssembleTraceWorker** (`trc_assemble_trace`): merge spans by trace ID into complete traces, detect missing spans, and compute trace metrics
- **CollectSpansWorker** (`trc_collect_spans`): pull spans from OpenTelemetry Collector, Jaeger Collector, or Kafka trace topics
- **InstrumentWorker** (`trc_instrument`): configure OpenTelemetry SDK, Jaeger agent, or Zipkin instrumentation on target services via API or config management

Connect to your OpenTelemetry collectors and Jaeger backend, and the trace collection pipeline operates in production with no workflow modifications.

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
trace-collection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tracecollection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TraceCollectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssembleTraceWorker.java
│       ├── CollectSpansWorker.java
│       ├── InstrumentWorker.java
│       └── StoreTraceWorker.java
└── src/test/java/tracecollection/workers/
    └── InstrumentWorkerTest.java        # 2 tests

```
