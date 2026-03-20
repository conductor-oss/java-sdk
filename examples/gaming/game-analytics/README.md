# Game Analytics in Java Using Conductor

Runs a game analytics pipeline: collecting raw event data, processing it into structured records, aggregating time-series metrics, computing KPIs (DAU, retention, ARPU), and generating a report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to analyze game performance metrics over a date range. The workflow collects raw event data (sessions, purchases, achievements, crashes), processes it into structured records, aggregates it into time-series summaries, computes key performance indicators (DAU, retention, ARPU, session length), and generates an analytics report. Without analytics, you are flying blind .  you do not know which features drive engagement, where players churn, or whether a new update improved retention.

Without orchestration, you'd build an analytics pipeline that queries event logs, runs ETL transforms, computes KPIs in SQL or code, and renders reports .  manually handling schema changes in event data, retrying failed queries on large datasets, and managing the compute resources for expensive aggregations.

## The Solution

**You just write the event collection, data processing, metric aggregation, KPI calculation, and report generation logic. Conductor handles ingestion retries, metric aggregation sequencing, and analytics pipeline tracking.**

Each analytics concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (collect, process, aggregate, compute KPIs, report), retrying if the data warehouse is temporarily unavailable, tracking every analytics run, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Event ingestion, metric aggregation, trend analysis, and dashboard update workers each process one layer of game telemetry data.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `gan_aggregate` | Aggregates processed events into time-series summaries (DAU, average session length, 7-day retention) |
| **CollectEventsWorker** | `gan_collect_events` | Collects raw events (logins, matches, purchases, achievements) for a game over a date range |
| **ComputeKpisWorker** | `gan_compute_kpis` | Computes key performance indicators: DAU, ARPDAU, 7-day retention, average session length, and conversion rate |
| **ProcessWorker** | `gan_process` | Processes raw events into structured records: sessions, matches, and purchases |
| **ReportWorker** | `gan_report` | Generates the final analytics report with all KPIs and publishes it |

Workers simulate game backend operations .  matchmaking, score processing, reward distribution ,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
gan_collect_events
    │
    ▼
gan_process
    │
    ▼
gan_aggregate
    │
    ▼
gan_compute_kpis
    │
    ▼
gan_report
```

## Example Output

```
=== Example 747: Game Analytics ===

Step 1: Registering task definitions...
  Registered: gan_collect_events, gan_process, gan_aggregate, gan_compute_kpis, gan_report

Step 2: Registering workflow 'game_analytics_747'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [aggregate] Aggregating metrics
  [collect] Collecting events for
  [kpis] Computing KPIs
  [process] Processing
  [report] Analytics report for

  Status: COMPLETED

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
java -jar target/game-analytics-1.0.0.jar
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
java -jar target/game-analytics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow game_analytics_747 \
  --version 1 \
  --input '{"gameId": "GAME-01", "GAME-01": "dateRange", "dateRange": "2026-03-01 to 2026-03-07", "2026-03-01 to 2026-03-07": "sample-2026-03-01 to 2026-03-07"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w game_analytics_747 -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real analytics stack .  your event pipeline for data collection, your data warehouse for aggregation, your BI platform for KPI dashboards and reporting, and the workflow runs identically in production.

- **Event collector**: pull raw events from your analytics platform (Unity Analytics, GameAnalytics, Firebase, custom event store)
- **Event processor**: clean, deduplicate, and enrich raw events with player profiles and session context
- **Aggregator**: compute time-series summaries using your data warehouse (BigQuery, Snowflake, Redshift) or streaming engine
- **KPI calculator**: compute DAU/MAU, D1/D7/D30 retention, ARPU, ARPPU, session length, and conversion rates
- **Report generator**: render dashboards in Looker, Tableau, or custom HTML reports; send daily KPI summaries to Slack

Change your event store or dashboard tool and the analytics pipeline processes data identically.

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
game-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gameanalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GameAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── CollectEventsWorker.java
│       ├── ComputeKpisWorker.java
│       ├── ProcessWorker.java
│       └── ReportWorker.java
└── src/test/java/gameanalytics/workers/
    ├── CollectEventsWorkerTest.java
    └── ReportWorkerTest.java
```
