# User Analytics in Java Using Conductor

A Java Conductor workflow example demonstrating User Analytics. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Your product team needs a periodic user analytics report covering engagement and retention metrics. The pipeline must collect raw user events (logins, page views, clicks) for a date range, aggregate them by day and user segment, compute key metrics like DAU, MAU, retention rate, and churn rate, and publish an updated analytics dashboard. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the event-collection, aggregation, metrics-computation, and dashboard-publishing workers. Conductor handles the analytics pipeline and data flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

CollectEventsWorker gathers login, page view, and click events, AggregateWorker groups them by day and segment, ComputeMetricsWorker calculates DAU, retention, and churn, and AnalyticsReportWorker publishes the dashboard.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `uan_aggregate` | Aggregates raw events by day and user segment, computing unique user counts |
| **AnalyticsReportWorker** | `uan_report` | Publishes the computed metrics to the analytics dashboard with a generated report URL |
| **CollectEventsWorker** | `uan_collect_events` | Collects user events (login, page_view, click) for the specified date range, returning total event count |
| **ComputeMetricsWorker** | `uan_compute_metrics` | Computes DAU, MAU, retention rate, average session duration, and churn rate from aggregated data |

Workers implement user lifecycle operations. account creation, verification, profile setup,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
uan_collect_events
    │
    ▼
uan_aggregate
    │
    ▼
uan_compute_metrics
    │
    ▼
uan_report

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
java -jar target/user-analytics-1.0.0.jar

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
java -jar target/user-analytics-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow uan_user_analytics \
  --version 1 \
  --input '{"dateRange": "2026-01-01T00:00:00Z", "segments": "sample-segments"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w uan_user_analytics -s COMPLETED -c 5

```

## How to Extend

Each worker handles one analytics step. connect your event store (Segment, Amplitude, Mixpanel) for data collection and your dashboard tool (Looker, Metabase, Grafana) for publishing, and the analytics workflow stays the same.

- **CollectEventsWorker** (`uan_collect_events`): query user events from your analytics platform (Segment, Mixpanel, Amplitude) or data warehouse (BigQuery, Snowflake) for the specified date range
- **AggregateWorker** (`uan_aggregate`): run aggregation queries in your data warehouse or use Segment's Personas API to group events by day and user segment
- **ComputeMetricsWorker** (`uan_compute_metrics`): calculate DAU, MAU, retention cohorts, and churn metrics using SQL against your analytics database or via the Amplitude/Mixpanel analytics APIs
- **AnalyticsReportWorker** (`uan_report`): push the computed metrics to your dashboard platform (Looker, Metabase, Grafana) and store the report snapshot in S3 for historical tracking

Connect your event store and BI platform and the collect-aggregate-compute-report analytics pipeline operates as designed.

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
user-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/useranalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AnalyticsReportWorker.java
│       ├── CollectEventsWorker.java
│       └── ComputeMetricsWorker.java
└── src/test/java/useranalytics/workers/
    ├── AnalyticsReportWorkerTest.java        # 2 tests
    ├── CollectEventsWorkerTest.java        # 3 tests
    └── ComputeMetricsWorkerTest.java        # 3 tests

```
