# Predictive Monitoring in Java Using Conductor :  History Collection, Model Training, Forecasting, and Proactive Alerts

A Java Conductor workflow example for predictive monitoring .  collecting historical metric data, training a forecasting model, predicting future values, and alerting proactively before thresholds are breached.

## The Problem

Reactive monitoring catches problems after they happen. You need predictive monitoring .  using historical data to forecast when disk will fill up, when latency will breach SLA, or when error rates will spike. This requires collecting historical data, training a time-series model, generating predictions, and alerting before the predicted breach occurs.

Without orchestration, predictive monitoring is an ML project that never leaves the notebook. Model training runs manually, predictions are one-off, and the connection between prediction and alerting is a TODO. The model becomes stale because nobody reruns the training pipeline.

## The Solution

**You just write the forecasting model and proactive alert rules. Conductor handles the collect-train-predict-alert cycle, retries when metric backends are slow, and a record of every model trained, prediction made, and proactive alert fired.**

Each prediction step is an independent worker .  history collection, model training, prediction, and alerting. Conductor runs them in sequence: collect history, train the model, forecast future values, then alert if breaches are predicted. Every prediction run is tracked with the model used, predicted values, and alert decisions. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The predictive pipeline uses CollectHistoryWorker to gather historical metric data, a TrainModelWorker to build the forecasting model, PredictWorker to forecast future values and breach likelihood, and PdmAlertWorker to fire proactive alerts before thresholds are actually breached.

| Worker | Task | What It Does |
|---|---|---|
| **CollectHistoryWorker** | `pdm_collect_history` | Collects historical metric data over a configurable number of days, returning the total data point count |
| **PdmAlertWorker** | `pdm_alert` | Sends a proactive alert if the predicted breach likelihood exceeds 50%, preventing threshold violations before they occur |
| **PredictWorker** | `pdm_predict` | Forecasts metric values over a configurable horizon, returning predicted peak value and breach likelihood percentage |
| **TrainModelWorker** | `pdm_train_model` | Trains a time-series forecasting model (e.g., Prophet) on historical data, returning model ID and accuracy score |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
pdm_collect_history
    │
    ▼
pdm_train_model
    │
    ▼
pdm_predict
    │
    ▼
pdm_alert
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
java -jar target/predictive-monitoring-1.0.0.jar
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
java -jar target/predictive-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow predictive_monitoring_424 \
  --version 1 \
  --input '{"metricName": "test", "historyDays": "test-value", "forecastHours": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w predictive_monitoring_424 -s COMPLETED -c 5
```

## How to Extend

Each worker runs one prediction step .  connect the history collector to Prometheus or InfluxDB, the model trainer to your time-series forecasting library, and the collect-train-predict-alert workflow stays the same.

- **CollectHistoryWorker** (`pdm_collect_history`): query time-series databases (InfluxDB, Prometheus, TimescaleDB) for historical metric data
- **PdmAlertWorker** (`pdm_alert`): send proactive alerts via PagerDuty/Slack when predicted values will breach thresholds within the forecast window
- **PredictWorker** (`pdm_predict`): generate predictions with confidence intervals for the configured forecast horizon

Plug in your real time-series database and ML model, and the predictive pipeline carries forward to production without workflow modifications.

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
predictive-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/predictivemonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PredictiveMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectHistoryWorker.java
│       ├── PdmAlertWorker.java
│       ├── PredictWorker.java
│       └── TrainModelWorker.java
└── src/test/java/predictivemonitoring/workers/
    └── PdmAlertWorkerTest.java        # 2 tests
```
