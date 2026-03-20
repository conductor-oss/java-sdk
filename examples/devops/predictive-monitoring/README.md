# Predictive Monitoring in Java with Conductor :  Collect History, Train Model, Predict, Alert

Automates predictive monitoring using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects historical metric data, trains a forecasting model (e.g., Prophet), predicts future metric values and breach likelihood, and sends proactive alerts before thresholds are actually crossed. You write the prediction logic, Conductor handles retries, failure routing, durability, and observability for free.

## Fixing Problems Before They Happen

Your CPU usage has been climbing steadily for 30 days. Will it breach 90% this week? Instead of waiting for an alert to fire at 3 AM, predictive monitoring analyzes 30 days of historical data (43,200 data points at 1-minute granularity), trains a forecasting model, and tells you there is a 72.3% chance of breach with a predicted peak of 88.5% by tomorrow afternoon. You get a warning alert now, while you can still scale up or optimize. Not a critical page when it is already too late.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the forecasting model and alert logic. Conductor handles the data-collection-to-prediction pipeline and tracks every forecast for accuracy validation.**

`CollectHistoryWorker` gathers historical metric data for the monitoring target .  time-series values with timestamps spanning the training window. `TrainModelWorker` fits a prediction model to the historical data, learning trends, seasonality, and growth patterns. `PredictWorker` uses the trained model to forecast future metric values over the prediction horizon, with confidence intervals. `AlertWorker` evaluates the predictions against thresholds and sends early warnings if a breach is forecasted, including the predicted breach date and recommended actions. Conductor records every prediction for model accuracy tracking over time.

### What You Write: Workers

Four workers power predictive monitoring. Collecting historical metrics, training a forecasting model, predicting future values, and alerting before thresholds are breached.

| Worker | Task | What It Does |
|---|---|---|
| **CollectHistory** | `pdm_collect_history` | Collects historical metric data points for the specified metric name and time range |
| **PdmAlert** | `pdm_alert` | Evaluates breach likelihood and sends a proactive warning or critical alert if the threshold is likely to be crossed |
| **Predict** | `pdm_predict` | Forecasts future metric values using the trained model, outputting predicted peak, timing, and breach likelihood with confidence intervals |
| **TrainModel** | `pdm_train_model` | Trains a time-series forecasting model (e.g., Prophet) on the collected historical data points |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> CollectHistory -> PdmAlert -> Predict -> TrainModel -> Output
```

## Example Output

```
=== Example 424: Predictive Monitoring ===

Step 1: Registering task definitions...
  Registered: pdm_collect_history, pdm_alert, pdm_predict, pdm_train_model

Step 2: Registering workflow 'predictive_monitoring_424'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [pdm_collect_history] Collecting
  [pdm_alert] Breach likelihood for
  [pdm_predict] Forecasting next
  [pdm_train_model] Training prediction model on

  Status: COMPLETED
  Output: {dataPoints=..., metricName=..., granularity=..., oldest=...}

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
  --workflow predictive_monitoring \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w predictive_monitoring -s COMPLETED -c 5
```

## How to Extend

Each worker handles one prediction stage .  replace the simulated calls with Prometheus range queries, Prophet time-series forecasting, or PagerDuty proactive alerts, and the monitoring workflow runs unchanged.

- **CollectHistory** (`pdm_collect_history`): query Prometheus range queries, CloudWatch GetMetricData, or InfluxDB for historical metric data with proper aggregation and gap handling
- **TrainModel** (`pdm_train_model`): use Prophet for time-series forecasting with automatic seasonality detection, scikit-learn for linear models, or AWS Forecast for managed ML-based predictions
- **Predict** (`pdm_predict`): generate future metric value forecasts using the trained model, identifying when thresholds will be breached and the estimated time-to-breach
- **PdmAlert** (`pdm_alert`): send predictive alerts to PagerDuty with estimated breach date and severity, post to Slack with trend charts, or create Jira tickets for capacity planning

Swap in Prophet or a real ML model; the forecast pipeline operates with the same prediction interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

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
│       ├── CollectHistory.java
│       ├── PdmAlert.java
│       ├── Predict.java
│       └── TrainModel.java
```
