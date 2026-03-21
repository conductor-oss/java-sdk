# Predictive Maintenance in Java with Conductor :  Sensor Data Collection, Trend Analysis, Failure Prediction, and Maintenance Scheduling

A Java Conductor workflow example that orchestrates predictive maintenance for industrial assets. collecting operational data (temperature, vibration, operating hours), analyzing degradation trends over time, predicting failure dates with confidence scores and recommended actions, and automatically scheduling maintenance work orders with cost estimates and parts procurement. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Predictive Maintenance Needs Orchestration

Preventing unplanned downtime requires turning raw sensor data into maintenance decisions. You collect current operational data. bearing temperature at 178F, vibration at 3.8 mm/s, 18,500 operating hours since install, 2,500 hours since last maintenance. You analyze trends by computing temperature and vibration slopes over time to derive an overall health score. You feed those trends into a failure prediction model that estimates when the asset will fail (e.g., "May 25, compressor valve, 82% confidence, medium risk"). If maintenance is warranted, you schedule a work order two weeks before the predicted failure date, order the replacement parts (compressor valve CV-300), and estimate the cost.

Each step in this pipeline depends on the previous one. trend analysis needs current data, failure prediction needs trends, and scheduling needs the predicted failure date. If the sensor data pull fails, you do not want stale trend data feeding the predictor. Without orchestration, you'd build a monolithic condition monitoring system where data collection, statistical analysis, ML inference, and CMMS integration are tangled together,  making it impossible to upgrade your prediction model, test scheduling logic independently, or trace which sensor readings led to a specific work order.

## How This Workflow Solves It

**You just write the maintenance workers. Sensor data collection, degradation trend analysis, failure prediction, and work order scheduling. Conductor handles sensor-to-work-order sequencing, data collection retries, and traceable records linking predictions to maintenance actions.**

Each maintenance stage is an independent worker. collect data, analyze trends, predict failure, schedule maintenance. Conductor sequences them, passes operational metrics through trend analysis into the prediction model and scheduling logic, retries if a sensor poll times out, and maintains a complete audit trail linking every work order to the specific readings and predictions that triggered it.

### What You Write: Workers

Four workers drive the maintenance cycle: CollectDataWorker gathers sensor readings and baselines, AnalyzeTrendsWorker computes degradation slopes and health scores, PredictFailureWorker estimates failure dates with confidence levels, and ScheduleMaintenanceWorker creates work orders and triggers parts procurement.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `pmn_analyze_trends` | Computes degradation slopes, health scores, and trend analysis from operational and historical data. |
| **CollectDataWorker** | `pmn_collect_data` | Collects current operational data (temperature, vibration, operating hours) and historical baselines from sensors. |
| **PredictFailureWorker** | `pmn_predict_failure` | Predicts failure date, identifies the likely failing component, and recommends a repair action with confidence score. |
| **ScheduleMaintenanceWorker** | `pmn_schedule_maintenance` | Creates a maintenance work order, triggers parts procurement, and schedules the repair window before predicted failure. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
pmn_collect_data
    │
    ▼
pmn_analyze_trends
    │
    ▼
pmn_predict_failure
    │
    ▼
pmn_schedule_maintenance

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
java -jar target/predictive-maintenance-1.0.0.jar

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
java -jar target/predictive-maintenance-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow predictive_maintenance_workflow \
  --version 1 \
  --input '{"assetId": "TEST-001", "assetType": "standard", "siteId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w predictive_maintenance_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectDataWorker to your sensor gateway or historian, PredictFailureWorker to your ML prediction model, and ScheduleMaintenanceWorker to your CMMS for parts procurement and crew scheduling. The workflow definition stays exactly the same.

- **CollectDataWorker** (`pmn_collect_data`): pull real sensor readings from your historian (OSIsoft PI, Aveva), SCADA system, or IoT platform to return current temperature, vibration, operating hours, and time since last maintenance
- **AnalyzeTrendsWorker** (`pmn_analyze_trends`): compute degradation slopes from historical data, calculate overall health scores using weighted multi-parameter analysis, and detect anomalous trend changes
- **PredictFailureWorker** (`pmn_predict_failure`): call your ML model (scikit-learn, TensorFlow, Azure ML) with trend data to predict failure date, identify the likely failing component, assess risk level, and recommend a specific repair action
- **ScheduleMaintenanceWorker** (`pmn_schedule_maintenance`): create a work order in your CMMS (SAP PM, Maximo, Fiix), trigger parts procurement for the required components, estimate repair cost, and schedule the maintenance window before predicted failure

Point each worker at your condition monitoring sensors or CMMS while preserving output fields, and the prediction-to-maintenance pipeline stays the same.

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
predictive-maintenance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/predictivemaintenance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PredictiveMaintenanceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTrendsWorker.java
│       ├── CollectDataWorker.java
│       ├── PredictFailureWorker.java
│       └── ScheduleMaintenanceWorker.java
└── src/test/java/predictivemaintenance/workers/
    ├── AnalyzeTrendsWorkerTest.java        # 2 tests
    └── CollectDataWorkerTest.java        # 2 tests

```
