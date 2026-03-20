# APM Workflow in Java Using Conductor :  Trace Collection, Latency Analysis, Bottleneck Detection, and Reporting

A Java Conductor workflow example for application performance monitoring (APM) .  collecting distributed traces, analyzing latency percentiles, detecting performance bottlenecks, and generating APM reports.

## The Problem

You need to monitor application performance across microservices. Traces must be collected from each service, latency distributions analyzed (p50, p95, p99), bottlenecks identified (slow database queries, N+1 API calls, serialization overhead), and results compiled into a report. Each step depends on the previous one .  you can't detect bottlenecks without latency data, and the report needs both.

Without orchestration, APM analysis is either a real-time system (expensive, complex) or a batch of scripts that don't coordinate. Trace collection from one service finishes before another starts, latency analysis runs against incomplete data, and bottleneck reports are stale by the time they're generated.

## The Solution

**You just write the trace collection and latency analysis logic. Conductor handles the sequential trace-to-report pipeline, retries when tracing backends are slow, and timing data for every analysis step.**

Each APM concern is an independent worker .  trace collection, latency analysis, bottleneck detection, and report generation. Conductor runs them in sequence, ensuring each step has the complete data from the previous one. Every APM run is tracked with timing ,  you can measure the pipeline itself. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the APM cycle: CollectTracesWorker gathers distributed traces, AnalyzeLatencyWorker computes p50/p95/p99 percentiles, DetectBottlenecksWorker identifies slow queries and N+1 patterns, and ApmReportWorker compiles findings into an actionable report.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeLatencyWorker** | `apm_analyze_latency` | Computes latency percentiles (p50, p95, p99) from collected trace data |
| **ApmReportWorker** | `apm_report` | Generates a consolidated APM report combining latency analysis and bottleneck findings |
| **CollectTracesWorker** | `apm_collect_traces` | Collects distributed traces for a specified service, returning the total trace count |
| **DetectBottlenecksWorker** | `apm_detect_bottlenecks` | Identifies performance bottlenecks (e.g., N+1 queries, slow endpoints) from trace and latency data |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
apm_collect_traces
    │
    ▼
apm_analyze_latency
    │
    ▼
apm_detect_bottlenecks
    │
    ▼
apm_report
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
java -jar target/apm-workflow-1.0.0.jar
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
java -jar target/apm-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow apm_workflow_421 \
  --version 1 \
  --input '{"serviceName": "test", "timeRange": "2026-01-01T00:00:00Z", "percentile": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w apm_workflow_421 -s COMPLETED -c 5
```

## How to Extend

Each worker runs one APM phase .  connect the trace collector to Jaeger or OpenTelemetry, the bottleneck detector to analyze real latency distributions, and the collect-analyze-detect-report workflow stays the same.

- **AnalyzeLatencyWorker** (`apm_analyze_latency`): compute real percentile distributions, identify latency outliers, and compare against SLA targets
- **ApmReportWorker** (`apm_report`): generate APM dashboards in Grafana, create Confluence reports, or push summaries to Slack
- **CollectTracesWorker** (`apm_collect_traces`): query real distributed tracing systems. Jaeger, Zipkin, AWS X-Ray, Datadog APM .  for trace data

Integrate with your distributed tracing backend, and the trace-to-report analysis orchestration carries over to production intact.

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
apm-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apmworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApmWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeLatencyWorker.java
│       ├── ApmReportWorker.java
│       ├── CollectTracesWorker.java
│       └── DetectBottlenecksWorker.java
└── src/test/java/apmworkflow/workers/
    └── CollectTracesWorkerTest.java        # 2 tests
```
