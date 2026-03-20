# Clickstream Analytics in Java Using Conductor :  Event Ingestion, Sessionization, and Journey Analysis

A Java Conductor workflow example for clickstream analytics: ingesting raw click events, grouping them into user sessions, analyzing navigation journeys for conversion patterns, and generating analytics reports. Uses [Conductor](https://github.## The Problem

You have a stream of raw click events. Page views, button clicks, form submissions, and you need to turn them into actionable product analytics. That means grouping events by user and time gap into sessions, tracing the page-to-page journeys users take, calculating conversion rates and drop-off points, and producing reports that product teams can act on. Each step depends on the previous one: you can't analyze journeys without sessions, and you can't build sessions without ingested events.

Without orchestration, you'd build a monolithic analytics pipeline that reads from Kafka or a click log, runs sessionization logic in-process, chains journey analysis directly after, and writes reports at the end. If the sessionization step fails on malformed events, you'd need hand-built retry logic. If the process crashes after sessionizing millions of events but before generating the report, all that computation is lost. Adding a new analysis dimension (like funnel analysis or heatmaps) means modifying deeply coupled code.

## The Solution

**You just write the event ingestion, sessionization, journey analysis, and report generation workers. Conductor handles the sequential data flow, retries on analytics query failures, and full observability across ingestion-to-report stages.**

Each stage of the analytics pipeline is a simple, independent worker. The ingestion worker parses and normalizes raw click events. The sessionization worker groups events by user ID and applies a configurable session timeout to split activity into distinct sessions. The journey analyzer traces page-to-page navigation paths and computes conversion rates. The report generator assembles session metrics, top journeys, and conversion data into a structured report. Conductor executes them in sequence, passes session data between steps, retries if an analytics query fails, and resumes from where it left off if the pipeline crashes mid-computation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the clickstream analytics pipeline: ingesting raw click events, grouping them into sessions, tracing user journeys for conversion analysis, and assembling the final analytics report.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeJourneysWorker** | `ck_analyze_journeys` | Analyzes user journeys from session data. |
| **GenerateReportWorker** | `ck_generate_report` | Generates a clickstream analytics report. |
| **IngestClicksWorker** | `ck_ingest_clicks` | Ingests click events from a tracking source. |
| **SessionizeWorker** | `ck_sessionize` | Groups click events into user sessions. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
ck_ingest_clicks
    │
    ▼
ck_sessionize
    │
    ▼
ck_analyze_journeys
    │
    ▼
ck_generate_report
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
java -jar target/clickstream-analytics-1.0.0.jar
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
java -jar target/clickstream-analytics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow clickstream_analytics \
  --version 1 \
  --input '{"clickData": "test-value", "sessionTimeout": "2026-01-01T00:00:00Z", "analysisType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w clickstream_analytics -s COMPLETED -c 5
```

## How to Extend

Connect the ingestion worker to Kafka or Kinesis, swap the sessionizer for a real event-grouping engine like ClickHouse or Flink, and the analytics workflow runs unchanged.

- **IngestClicksWorker** → consume events from Kafka, Kinesis, or a Google Analytics export instead of static input
- **SessionizeWorker** → use a real sessionization library or query engine (Apache Flink, ClickHouse) for large-scale event grouping with configurable timeout windows
- **AnalyzeJourneysWorker** → run funnel analysis, compute drop-off rates at each step, or build Sankey flow visualizations from session data
- **GenerateReportWorker** → write results to a data warehouse (BigQuery, Redshift), push to a BI tool (Looker, Metabase), or send a Slack digest to the product team

Swapping in a real sessionization engine or analytics backend requires no workflow changes, provided each worker returns the expected session and journey data structures.

**Add new analysis stages** by creating a new worker and adding a task to `workflow.json`, for example, heatmap generation, A/B test cohort splitting, or anomaly detection on session duration trends.

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
clickstream-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/clickstreamanalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ClickstreamAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeJourneysWorker.java
│       ├── GenerateReportWorker.java
│       ├── IngestClicksWorker.java
│       └── SessionizeWorker.java
└── src/test/java/clickstreamanalytics/workers/
    ├── AnalyzeJourneysWorkerTest.java        # 4 tests
    ├── GenerateReportWorkerTest.java        # 3 tests
    ├── IngestClicksWorkerTest.java        # 3 tests
    └── SessionizeWorkerTest.java        # 4 tests
```
