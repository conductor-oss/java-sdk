# Monitoring AI in Java with Conductor :  Collect Metrics, Detect Anomalies, Diagnose, and Recommend

A Java Conductor workflow that provides intelligent monitoring .  collecting system metrics from a service, detecting anomalies in those metrics, diagnosing the root cause of any anomalies, and recommending actions to resolve the issue. Given a `serviceName` and `timeWindow`, the pipeline produces metric summaries, anomaly detections, root cause diagnoses, and actionable recommendations. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step monitoring intelligence pipeline.

## Going Beyond Dashboards to Actionable Intelligence

Traditional monitoring shows you metrics on a dashboard. You still have to notice the anomaly, figure out what caused it, and decide what to do. AI-powered monitoring automates that reasoning chain: collect the metrics, detect what is abnormal, diagnose why, and recommend what to do about it. Each step builds on the previous .  you cannot diagnose without anomalies, and you cannot recommend without a diagnosis.

This workflow runs one monitoring analysis cycle. The metrics collector pulls system data (CPU, memory, latency, error rates) for the service over the specified time window. The anomaly detector scans the metrics for deviations from normal behavior. The diagnoser analyzes detected anomalies to identify the root cause (memory leak, traffic spike, degraded dependency). The recommender produces specific actions (scale up, restart, roll back, investigate further) based on the diagnosis.

## The Solution

**You just write the metrics-collection, anomaly-detection, diagnosis, and recommendation workers. Conductor handles the monitoring intelligence pipeline.**

Four workers form the monitoring pipeline .  metrics collection, anomaly detection, diagnosis, and recommendation. The collector gathers system metrics. The detector identifies anomalous patterns. The diagnoser determines root causes. The recommender suggests specific actions. Conductor sequences the four steps and passes metrics, anomalies, and diagnoses between them via JSONPath.

### What You Write: Workers

CollectMetricsWorker pulls CPU, memory, and latency data, DetectAnomaliesWorker flags deviations, DiagnoseWorker identifies root causes, and RecommendWorker suggests actions like scaling or rolling back.

| Worker | Task | What It Does |
|---|---|---|
| **CollectMetricsWorker** | `mai_collect_metrics` | Pulls system metrics (CPU, memory, latency, error rates) for the service over the time window. |
| **DetectAnomaliesWorker** | `mai_detect_anomalies` | Scans collected metrics for deviations from normal behavior and flags anomalies. |
| **DiagnoseWorker** | `mai_diagnose` | Identifies the root cause of detected anomalies (memory leak, traffic spike, degraded dependency). |
| **RecommendWorker** | `mai_recommend` | Produces specific actionable recommendations (scale up, restart, roll back) based on the diagnosis. |

Workers simulate CRM operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
mai_collect_metrics
    │
    ▼
mai_detect_anomalies
    │
    ▼
mai_diagnose
    │
    ▼
mai_recommend
```

## Example Output

```
=== Example 649: Monitoring AI ===

Step 1: Registering task definitions...
  Registered: mai_collect_metrics, mai_detect_anomalies, mai_diagnose, mai_recommend

Step 2: Registering workflow 'mai_monitoring_ai'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Collected 4 metric types for
  [detect] Found
  [diagnose] Root cause:
  [recommend]

  Status: COMPLETED
  Output: {metrics=..., metricCount=..., anomalies=..., anomalyCount=...}

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
java -jar target/monitoring-ai-1.0.0.jar
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
java -jar target/monitoring-ai-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mai_monitoring_ai \
  --version 1 \
  --input '{"serviceName": "sample-name", "order-service": "sample-order-service", "timeWindow": "2025-01-15T10:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mai_monitoring_ai -s COMPLETED -c 5
```

## How to Extend

Each worker handles one monitoring step .  connect your observability platform (Datadog, Prometheus, New Relic) for metrics and your incident management system (PagerDuty, OpsGenie) for action recommendations, and the monitoring workflow stays the same.

- **CollectMetricsWorker** (`mai_collect_metrics`): connect to Prometheus, Datadog, or CloudWatch for real metrics collection
- **DetectAnomaliesWorker** (`mai_detect_anomalies`): use ML-based anomaly detection (Prophet, Isolation Forest) for more accurate alerting
- **DiagnoseWorker** (`mai_diagnose`): integrate with logging systems (ELK, Splunk) and tracing (Jaeger) for real root cause analysis

Connect Prometheus or Datadog for real metrics collection and the anomaly-diagnosis-recommendation intelligence pipeline keeps working as configured.

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
monitoring-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/monitoringai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MonitoringAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectMetricsWorker.java
│       ├── DetectAnomaliesWorker.java
│       ├── DiagnoseWorker.java
│       └── RecommendWorker.java
└── src/test/java/monitoringai/workers/
    ├── CollectMetricsWorkerTest.java        # 2 tests
    ├── DetectAnomaliesWorkerTest.java        # 2 tests
    ├── DiagnoseWorkerTest.java        # 2 tests
    └── RecommendWorkerTest.java        # 2 tests
```
