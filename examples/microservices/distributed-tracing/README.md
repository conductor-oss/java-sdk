# Distributed Tracing in Java with Conductor

Distributed tracing with end-to-end request tracking. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Tracing a request across multiple microservices requires creating a trace context, propagating span IDs through each service call, recording timing for database operations, and exporting the complete trace to a backend like Jaeger or Zipkin. Each span must reference its parent to form a proper trace tree.

Without orchestration, each service must manually propagate trace headers, and if one service forgets, the trace is broken. There is no centralized way to ensure every service participates in tracing, and correlating spans across services requires manual effort.

## The Solution

**You just write the trace-creation, service-span, db-span, and trace-export workers. Conductor handles span sequencing, durable trace assembly, and automatic retry if the export step fails.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers build a trace: CreateTraceWorker generates the root context, ServiceSpanWorker records a service call, DbSpanWorker captures database latency, and ExportTraceWorker sends the completed trace to a backend.

| Worker | Task | What It Does |
|---|---|---|
| **CreateTraceWorker** | `dt_create_trace` | Creates a new root trace with a unique traceId and root spanId for the operation. |
| **DbSpanWorker** | `dt_db_span` | Records a child span for a database operation, capturing query latency. |
| **ExportTraceWorker** | `dt_export_trace` | Exports the completed trace (all spans) to a tracing backend like Jaeger. |
| **ServiceSpanWorker** | `dt_service_span` | Records a child span for a service-to-service call, linking to the parent span. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
dt_create_trace
    │
    ▼
dt_service_span
    │
    ▼
dt_db_span
    │
    ▼
dt_export_trace
```

## Example Output

```
=== Example 316: Distributed Tracing ===

Step 1: Registering task definitions...
  Registered: dt_create_trace, dt_service_span, dt_db_span, dt_export_trace

Step 2: Registering workflow 'distributed_tracing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [trace] Created:
  [span] DB span under
  [export] Exported
  [span] Service span under

  Status: COMPLETED
  Output: {traceId=..., spanId=..., durationMs=..., exported=...}

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
java -jar target/distributed-tracing-1.0.0.jar
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
java -jar target/distributed-tracing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow distributed_tracing_workflow \
  --version 1 \
  --input '{"requestId": "REQ-800", "REQ-800": "operation", "operation": "get-user-profile", "get-user-profile": "sample-get-user-profile"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w distributed_tracing_workflow -s COMPLETED -c 5
```

## How to Extend

Wire each worker to the OpenTelemetry SDK and your tracing backend (Jaeger, Zipkin, Datadog), the trace-creation and span-export workflow stays exactly the same.

- **CreateTraceWorker** (`dt_create_trace`): integrate with OpenTelemetry SDK to create W3C-compliant trace contexts
- **DbSpanWorker** (`dt_db_span`): use OpenTelemetry JDBC instrumentation to automatically capture database query spans
- **ExportTraceWorker** (`dt_export_trace`): export via OTLP to Jaeger, Zipkin, Datadog, or AWS X-Ray

Switching the trace exporter from Jaeger to Zipkin or Datadog requires no changes to the trace-building workflow.

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
distributed-tracing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/distributedtracing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DistributedTracingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateTraceWorker.java
│       ├── DbSpanWorker.java
│       ├── ExportTraceWorker.java
│       └── ServiceSpanWorker.java
└── src/test/java/distributedtracing/workers/
    ├── CreateTraceWorkerTest.java        # 2 tests
    ├── DbSpanWorkerTest.java        # 2 tests
    ├── ExportTraceWorkerTest.java        # 2 tests
    └── ServiceSpanWorkerTest.java        # 2 tests
```
