# Event Monitoring in Java Using Conductor

Sequential event monitoring workflow that collects metrics, analyzes throughput, latency, and errors, then generates a report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to monitor the health of your event processing pipeline. This means collecting throughput, latency, and error rate metrics over a specified time range, analyzing those metrics for anomalies (throughput drops, latency spikes, error rate increases), and generating a monitoring report. Without monitoring, you only learn about pipeline problems when downstream systems start failing.

Without orchestration, you'd build a monitoring script that queries multiple metrics sources, runs analysis logic inline, generates reports, and sends alerts .  manually handling metrics API timeouts, correlating data across different monitoring systems, and scheduling the monitoring job itself.

## The Solution

**You just write the metrics-collection, throughput/latency/error analysis, and report-generation workers. Conductor handles sequential metric analysis, retry on metrics API timeouts, and a historical record of every monitoring run.**

Each monitoring concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of collecting metrics, analyzing them for anomalies, and generating the report ,  retrying if a metrics API times out, tracking every monitoring run, and providing a complete history of pipeline health assessments. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers power the monitoring pipeline: CollectMetricsWorker gathers raw data, AnalyzeThroughputWorker, AnalyzeLatencyWorker, and AnalyzeErrorsWorker each assess a different dimension, and GenerateReportWorker produces the final health assessment.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeErrorsWorker** | `em_analyze_errors` | Analyzes error rate from raw metrics. |
| **AnalyzeLatencyWorker** | `em_analyze_latency` | Analyzes latency from raw metrics. |
| **AnalyzeThroughputWorker** | `em_analyze_throughput` | Analyzes throughput from raw metrics. |
| **CollectMetricsWorker** | `em_collect_metrics` | Collects raw metrics for a given pipeline and time range. |
| **GenerateReportWorker** | `em_generate_report` | Generates a monitoring report from throughput, latency, and error analysis. |

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
em_collect_metrics
    │
    ▼
em_analyze_throughput
    │
    ▼
em_analyze_latency
    │
    ▼
em_analyze_errors
    │
    ▼
em_generate_report
```

## Example Output

```
=== Event Monitoring Demo ===

Step 1: Registering task definitions...
  Registered: em_collect_metrics, em_analyze_throughput, em_analyze_latency, em_analyze_errors, em_generate_report

Step 2: Registering workflow 'event_monitoring_wf'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [em_analyze_errors] Error rate:
  [em_analyze_latency] Latency avg=
  [em_analyze_throughput] Analyzing throughput for
  [em_collect_metrics] Collecting metrics for pipeline:
  [em_generate_report] Report for pipeline '

  Status: COMPLETED
  Output: {errorRate=..., latency=..., throughput=..., rawMetrics=...}

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
java -jar target/event-monitoring-1.0.0.jar
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
java -jar target/event-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_monitoring_wf \
  --version 1 \
  --input '{"pipelineName": "sample-name", "order-events-pipeline": "sample-order-events-pipeline", "timeRange": "2025-01-15T10:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_monitoring_wf -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real metrics source (Prometheus, CloudWatch), analysis logic, and reporting dashboard (Grafana, Datadog), the collect-analyze-report monitoring workflow stays exactly the same.

- **Metrics collector**: query real metrics from Prometheus, Datadog, CloudWatch, or your event broker's monitoring APIs
- **Analyzer**: implement statistical anomaly detection (z-score, rolling averages, trend analysis) or integrate with ML-based anomaly detection services
- **Report generator**: render dashboards as HTML/PDF, push to Grafana, or send Slack summaries with key metrics and alerts

Pointing CollectMetricsWorker at Prometheus or Datadog instead of simulated data requires no changes to the analysis workflow.

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
event-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventmonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeErrorsWorker.java
│       ├── AnalyzeLatencyWorker.java
│       ├── AnalyzeThroughputWorker.java
│       ├── CollectMetricsWorker.java
│       └── GenerateReportWorker.java
└── src/test/java/eventmonitoring/workers/
    ├── AnalyzeErrorsWorkerTest.java        # 8 tests
    ├── AnalyzeLatencyWorkerTest.java        # 8 tests
    ├── AnalyzeThroughputWorkerTest.java        # 8 tests
    ├── CollectMetricsWorkerTest.java        # 8 tests
    └── GenerateReportWorkerTest.java        # 8 tests
```
