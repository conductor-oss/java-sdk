# Observability Pipeline in Java with Conductor

Orchestrates a full observability pipeline using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects metrics from a service, correlates distributed traces across microservices, detects anomalies (latency spikes, error rate increases), and either fires alerts or stores the data for dashboarding.

## Seeing Through the Noise

Your checkout-service generated 15,000 metrics and 3,200 traces in the last hour. Somewhere in that data, there is a latency spike and an error rate increase. Finding the signal in the noise requires collecting metrics, correlating traces to understand request flow, detecting anomalies, and deciding whether to page someone or just store it for later analysis. Doing this manually means staring at Grafana dashboards hoping to spot the pattern.

Without orchestration, you'd run a cron job that scrapes Prometheus, pipe the output into a Python script that computes z-scores, grep trace logs for correlated spans, and email yourself when something looks off. If the metrics collector fails, the anomaly detector runs on stale data and either misses the spike or fires a false alarm. There's no structured pipeline from raw telemetry to actionable alert, and no record of which anomalies were detected, when, or what the system looked like at the time.

## The Solution

**You write the metrics collection and anomaly detection logic. Conductor handles the telemetry-to-alert pipeline, correlation sequencing, and execution history.**

Each stage of the observability pipeline is a simple, independent worker. The metrics collector gathers CPU, memory, request rate, and error rate from the target service over the specified time window. The trace correlator links distributed traces across microservices to build end-to-end request flow maps, connecting a slow checkout response to a database query three services deep. The anomaly detector identifies latency spikes and error rate increases by comparing current metrics against baseline thresholds. The alert-or-store worker routes anomalies to PagerDuty or Slack, and stores clean metrics in the time-series database for dashboarding. Conductor executes them in strict sequence, ensures anomaly detection only runs after traces are correlated with metrics, retries if the metrics endpoint is temporarily unreachable, and tracks every pipeline execution with full input/output history. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers build the observability pipeline. Collecting metrics, correlating traces, detecting anomalies, and routing alerts or storing data.

| Worker | Task | What It Does |
|---|---|---|
| **AlertOrStoreWorker** | `op_alert_or_store` | Routes anomalies to alerting channels or stores clean metrics in the time-series database |
| **CollectMetricsWorker** | `op_collect_metrics` | Gathers metrics (CPU, memory, request rate, error rate) from the target service |
| **CorrelateTracesWorker** | `op_correlate_traces` | Links distributed traces across microservices to build end-to-end request flow |
| **DetectAnomaliesWorker** | `op_detect_anomalies` | Identifies anomalies in the correlated data (latency spikes, error rate increases) |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
op_collect_metrics
    │
    ▼
op_correlate_traces
    │
    ▼
op_detect_anomalies
    │
    ▼
op_alert_or_store

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
java -jar target/observability-pipeline-1.0.0.jar

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
java -jar target/observability-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow observability_pipeline_workflow \
  --version 1 \
  --input '{"service": "order-service", "timeWindow": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w observability_pipeline_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one observability concern .  plug in Prometheus, Jaeger, or PagerDuty for real metrics scraping, trace correlation, and alerting, and the pipeline runs unchanged.

- **CollectMetricsWorker** (`op_collect_metrics`): scrape Prometheus exporters for CPU/memory/request rate, query Datadog Metrics API for custom business metrics, or pull structured telemetry from OpenTelemetry collectors
- **CorrelateTracesWorker** (`op_correlate_traces`): query Jaeger, Zipkin, or AWS X-Ray to correlate distributed traces by trace ID, building end-to-end request flow maps across microservices
- **DetectAnomaliesWorker** (`op_detect_anomalies`): compute z-scores or rolling standard deviations on latency and error rate metrics, flag anomalies when values exceed configurable thresholds (e.g., P99 latency > 2x baseline or error rate > 1%)
- **AlertOrStoreWorker** (`op_alert_or_store`): fire alerts via PagerDuty Events API or Opsgenie for anomalies, post to Slack with context, and store clean metrics in InfluxDB, Prometheus, or Grafana Mimir for dashboard visualization

Wire in Prometheus and Jaeger for real telemetry; the pipeline maintains the same data contract end to end.

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
observability-pipeline-observability-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/observabilitypipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlertOrStoreWorker.java
│       ├── CollectMetricsWorker.java
│       ├── CorrelateTracesWorker.java
│       └── DetectAnomaliesWorker.java
└── src/test/java/observabilitypipeline/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
