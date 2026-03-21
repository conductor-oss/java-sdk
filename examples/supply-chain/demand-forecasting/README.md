# Demand Forecasting in Java with Conductor :  Data Collection, Trend Analysis, Forecast Generation, and Procurement Planning

A Java Conductor workflow example for demand forecasting. collecting historical sales and market data for a product category (e.g., consumer electronics in North America), analyzing seasonal trends and growth patterns, generating a 6-month demand forecast, and creating procurement plans based on predicted volumes. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to forecast demand for consumer electronics across North America over the next 6 months. This requires pulling historical sales data from your ERP, POS systems, and market intelligence feeds, analyzing the data for seasonal patterns (holiday spikes, back-to-school), growth trends, and market shifts, running forecasting models to predict future demand by SKU and region, and translating those forecasts into procurement plans with order quantities and timing. If the forecast over-predicts, you carry excess inventory; if it under-predicts, you stock out during peak demand.

Without orchestration, the data scientist runs a Jupyter notebook that pulls data manually, the supply planner copies forecast numbers into a spreadsheet, and procurement creates POs based on gut feel plus the spreadsheet. There is no reproducibility. nobody knows which data was used for last quarter's forecast. When the model needs retraining, the entire pipeline is re-run from scratch because there is no checkpoint between data collection and forecasting.

## The Solution

**You just write the forecasting workers. Data collection, trend analysis, demand prediction, and procurement planning. Conductor handles data checkpointing, automatic retries on model failures, and versioned records for forecast reproducibility.**

Each stage of the forecasting pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so clean historical data feeds trend analysis, trend outputs parameterize the forecast model, and forecast results drive procurement planning. If the forecasting model worker fails (out-of-memory, timeout), Conductor retries it using the already-collected and analyzed data,  no need to re-pull months of sales history. Every data snapshot, trend analysis, forecast output, and procurement plan is versioned and recorded for audit and model improvement.

### What You Write: Workers

Four workers form the forecasting pipeline: CollectDataWorker pulls historical sales data, AnalyzeTrendsWorker identifies seasonal patterns, ForecastWorker generates demand predictions, and PlanWorker translates forecasts into procurement actions.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `df_analyze_trends` | Analyzes historical data for seasonal patterns, growth trends, and market shifts. |
| **CollectDataWorker** | `df_collect_data` | Collects historical sales and market data for the product category and region. |
| **ForecastWorker** | `df_forecast` | Generates a demand forecast over the specified horizon using trend analysis results. |
| **PlanWorker** | `df_plan` | Translates demand forecasts into procurement plans with order quantities and timing. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
df_collect_data
    │
    ▼
df_analyze_trends
    │
    ▼
df_forecast
    │
    ▼
df_plan

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
java -jar target/demand-forecasting-1.0.0.jar

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
java -jar target/demand-forecasting-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow df_demand_forecasting \
  --version 1 \
  --input '{"productCategory": "general", "horizon": "sample-horizon", "region": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w df_demand_forecasting -s COMPLETED -c 5

```

## How to Extend

Connect CollectDataWorker to your ERP and POS feeds, ForecastWorker to your ML forecasting model (Prophet, ARIMA), and PlanWorker to your procurement system. The workflow definition stays exactly the same.

- **CollectDataWorker** (`df_collect_data`): pull historical sales data from your ERP (SAP, Oracle), POS aggregation layer, and external market feeds (Nielsen, IRI) into a unified dataset
- **AnalyzeTrendsWorker** (`df_analyze_trends`): run time-series decomposition to extract seasonality, trend, and cyclical components using libraries like Apache Commons Math or a Python microservice via REST
- **ForecastWorker** (`df_forecast`): invoke your ML forecasting model (Prophet, ARIMA, or a custom deep learning model hosted on SageMaker/Vertex AI) to generate SKU-level demand predictions
- **PlanWorker** (`df_plan`): translate forecast volumes into procurement plans with order quantities, lead-time-adjusted order dates, and safety stock levels, then push them to your planning system (SAP IBP, Kinaxis)

Point any worker at a real data source or ML model while maintaining the output schema, and the pipeline requires no reconfiguration.

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
demand-forecasting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/demandforecasting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DemandForecastingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTrendsWorker.java
│       ├── CollectDataWorker.java
│       ├── ForecastWorker.java
│       └── PlanWorker.java
└── src/test/java/demandforecasting/workers/
    ├── AnalyzeTrendsWorkerTest.java        # 2 tests
    ├── CollectDataWorkerTest.java        # 2 tests
    ├── ForecastWorkerTest.java        # 2 tests
    └── PlanWorkerTest.java        # 2 tests

```
