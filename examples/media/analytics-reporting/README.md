# Analytics Reporting Pipeline in Java Using Conductor :  Event Collection, Data Aggregation, KPI Computation, and Dashboard Generation

A Java Conductor workflow example that orchestrates an analytics reporting pipeline .  collecting raw user events from multiple data sources into Parquet files, aggregating them into session-level metrics (total sessions, unique users, page views), computing business KPIs (DAU, WAU, MAU, bounce rate, conversion rate, revenue per user), and generating interactive dashboard reports with scheduled delivery via email and Slack. Uses [Conductor](https://github.

## Why Analytics Pipelines Need Orchestration

Building an analytics report requires a strict data pipeline. You collect 1.25 million raw events from multiple sources and write them to a staging area. You aggregate those events into session-level summaries .  85K total sessions, 42K unique users, 320K page views, 245-second average session duration. You compute business-critical KPIs from the aggregated data: daily/weekly/monthly active users, 38.5% bounce rate, 4.2% conversion rate. Finally, you generate an interactive dashboard report and schedule delivery to stakeholders.

Each stage depends on clean output from the previous one .  aggregation needs complete event data, KPI computation needs accurate aggregates, and the report needs final metrics. If the event collection fails partway through, you do not want to compute KPIs on incomplete data. Without orchestration, you'd build a monolithic ETL script that mixes data ingestion, aggregation queries, metric calculations, and report rendering ,  making it impossible to rerun just the KPI computation when a formula changes, or to swap your event source without touching the report generator.

## How This Workflow Solves It

**You just write the analytics workers. Event collection, data aggregation, KPI computation, and dashboard generation. Conductor handles stage ordering, data source retries, and per-stage timing metrics for pipeline optimization.**

Each pipeline stage is an independent worker .  collect events, aggregate data, compute metrics, generate report. Conductor sequences them, passes raw data paths and aggregated metrics between stages, retries if a data source query times out, and tracks exactly how long each ETL stage takes for pipeline optimization.

### What You Write: Workers

Four workers form the analytics pipeline: CollectEventsWorker ingests raw user events, AggregateDataWorker computes session-level metrics, ComputeMetricsWorker derives business KPIs, and GenerateReportWorker builds interactive dashboards.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateDataWorker** | `anr_aggregate_data` | Aggregates data |
| **CollectEventsWorker** | `anr_collect_events` | Collects events |
| **ComputeMetricsWorker** | `anr_compute_metrics` | Computes metrics |
| **GenerateReportWorker** | `anr_generate_report` | Generates the report |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
anr_collect_events
    │
    ▼
anr_aggregate_data
    │
    ▼
anr_compute_metrics
    │
    ▼
anr_generate_report

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
java -jar target/analytics-reporting-1.0.0.jar

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
java -jar target/analytics-reporting-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow analytics_reporting_workflow \
  --version 1 \
  --input '{"reportId": "TEST-001", "dateRange": "2026-01-01T00:00:00Z", "dataSources": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w analytics_reporting_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectEventsWorker to your event stream (Kafka, Kinesis), AggregateDataWorker to your data warehouse (Snowflake, BigQuery), and GenerateReportWorker to your BI tool (Looker, Tableau). The workflow definition stays exactly the same.

- **CollectEventsWorker** (`anr_collect_events`): query real event streams from Segment, Amplitude, or your Kafka topic, writing raw events to S3/GCS as Parquet files
- **AggregateDataWorker** (`anr_aggregate_data`): run SQL aggregation queries against your data warehouse (BigQuery, Snowflake, Redshift) to compute session-level metrics
- **ComputeMetricsWorker** (`anr_compute_metrics`): calculate real KPIs (DAU/WAU/MAU, bounce rate, conversion rate, revenue per user) from the aggregated data using your metric definitions
- **GenerateReportWorker** (`anr_generate_report`): render interactive dashboards (Looker, Metabase, custom React) and schedule delivery via email to stakeholders and Slack channels

Replace any worker with a production data warehouse query or BI tool while preserving output fields, and the reporting pipeline stays the same.

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
analytics-reporting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/analyticsreporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AnalyticsReportingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateDataWorker.java
│       ├── CollectEventsWorker.java
│       ├── ComputeMetricsWorker.java
│       └── GenerateReportWorker.java
└── src/test/java/analyticsreporting/workers/
    ├── AggregateDataWorkerTest.java        # 2 tests
    └── CollectEventsWorkerTest.java        # 2 tests

```
