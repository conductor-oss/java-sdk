# Anomaly Detection in Java Using Conductor: Data Collection, Baseline Computation, Detection, Classification, and Alerting

Request latency on your checkout service crept from 120ms to 450ms over six hours. Nobody noticed because it never crossed the static 500ms alert threshold. By the time it finally spiked to 800ms and paged someone, six hours of degraded checkout experience had already cost you an estimated $40K in abandoned carts. A fixed threshold can't catch gradual drift, and your on-call shouldn't have to stare at Grafana dashboards to notice that "120ms average" has quietly become "450ms average." You need detection that computes a baseline from recent history, spots deviations statistically, and pages you while the problem is still small.

## The Problem

You need to detect when a metric deviates from normal behavior. CPU spikes, latency increases, error rate jumps. Simple threshold checks miss gradual degradation and fire on expected seasonal patterns. You need to compute a baseline from historical data, detect deviations using statistical methods, classify the severity (warning vs critical), and alert only on genuine anomalies.

Without orchestration, anomaly detection is either a monolithic ML pipeline that's hard to debug or a collection of scripts that don't share state. When the baseline computation fails, detection runs against stale data. Nobody knows whether a missed alert was due to bad baseline, weak detection, or a notification failure.

## The Solution

**You just write the baseline computation and deviation detection logic. Conductor handles the collect-baseline-detect-classify-alert sequence, retries when metric sources are temporarily unavailable, and tracking of every baseline computed and anomaly detected.**

Each anomaly detection step is an independent worker: data collection, baseline computation, deviation detection, classification, and alerting. Conductor runs them in sequence, passing the baseline to the detector and the anomaly details to the classifier. Every detection run is tracked, you can see the baseline used, deviations found, and whether alerts fired. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers form the detection pipeline: CollectDataWorker gathers historical metrics, ComputeBaselineWorker calculates statistical baselines, DetectWorker computes z-scores against the baseline, ClassifyWorker categorizes anomaly severity, and AlertWorker notifies the appropriate channel.

| Worker | Task | What It Does |
|---|---|---|
| **AlertWorker** | `anom_alert` | Sends an alert with severity and classification details to the configured channel (e.g., Slack) |
| **ClassifyWorker** | `anom_classify` | Classifies anomalies by z-score magnitude into normal/moderate/significant/spike categories with corresponding severity levels |
| **CollectDataWorker** | `anom_collect_data` | Collects historical metric data over a configurable lookback window and returns the latest value and data point count |
| **ComputeBaselineWorker** | `anom_compute_baseline` | Computes a statistical baseline (mean and standard deviation) from collected data points for anomaly comparison |
| **DetectWorker** | `anom_detect` | Calculates the z-score of the latest metric value against the baseline to determine if it is anomalous |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic, the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
anom_collect_data
    │
    ▼
anom_compute_baseline
    │
    ▼
anom_detect
    │
    ▼
anom_classify
    │
    ▼
anom_alert
```

## Example Output

```
=== Example 414: Anomaly Detectio ===

Step 1: Registering task definitions...
  Registered: anom_collect_data, anom_compute_baseline, anom_detect, anom_classify, anom_alert

Step 2: Registering workflow 'anomaly_detection_414'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f928bef5-fc1f-19da-c6e4-b43144bbd0b6

  [collect] Collecting 24h of request_latency_ms data
  [baseline] Computing baseline from 720 data points
  [detect] Z-Score: 85 - anomaly: True
  [classify] request_latency_ms: normal (severity=info)
  [alert] info alert for request_latency_ms: normal


  Status: COMPLETED
  Output: {isAnomaly=true, classification=normal, severity=info, alerted=true}

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
java -jar target/anomaly-detection-1.0.0.jar
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
java -jar target/anomaly-detection-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow anomaly_detection_414 \
  --version 1 \
  --input '{"metricName": "request_latency_ms", "lookbackHours": 24, "sensitivity": "high"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w anomaly_detection_414 -s COMPLETED -c 5
```

## How to Extend

Each worker owns one detection step. Connect the data collector to Prometheus or InfluxDB, the alerting worker to PagerDuty, and the collect-baseline-detect-classify-alert workflow stays the same.

- **AlertWorker** (`anom_alert`): send severity-appropriate alerts via PagerDuty (critical), Slack (warning), or dashboard updates (info)
- **ClassifyWorker** (`anom_classify`): classify anomaly severity based on deviation magnitude, duration, and business impact
- **CollectDataWorker** (`anom_collect_data`): query real metric stores. Prometheus, InfluxDB, CloudWatch, Datadog, for historical and current values

Connect to your real time-series databases and alerting channels, and the statistical detection pipeline continues running without workflow changes.

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
anomaly-detection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/anomalydetection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AnomalyDetectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlertWorker.java
│       ├── ClassifyWorker.java
│       ├── CollectDataWorker.java
│       ├── ComputeBaselineWorker.java
│       └── DetectWorker.java
└── src/test/java/anomalydetection/workers/
    └── DetectWorkerTest.java        # 2 tests
```
