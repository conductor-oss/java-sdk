# Usage Analytics in Java Using Conductor

A Java Conductor workflow example that orchestrates telecom usage analytics .  collecting call detail records (CDRs) for a region and time period, processing raw records into normalized usage events, aggregating metrics and detecting anomalies, generating the usage analytics report, and raising alerts for any anomalies found. Uses [Conductor](https://github.

## Why Usage Analytics Needs Orchestration

Analyzing telecom usage data requires a pipeline where each transformation depends on the previous one. You collect raw CDRs from network switches and media gateways for a region and time period. You process each record .  normalizing formats, enriching with subscriber metadata, filtering duplicates, and converting to a standard usage event schema. You aggregate the processed records into metrics (total minutes, data volume, peak hours, geographic distribution) and detect anomalies (usage spikes, fraud patterns, revenue leakage). You generate the analytics report for business stakeholders. Finally, you raise alerts for any anomalies that need immediate attention.

If processing fails partway through, you need to know which CDRs were already processed to avoid counting them twice in the aggregation. If the report generates successfully but the alert worker fails, fraud patterns go unnotified even though they were detected. Without orchestration, you'd build a batch ETL job that mixes CDR collection, format normalization, aggregation SQL, report generation, and alerting into a single cron script .  making it impossible to reprocess a subset of CDRs, test anomaly detection rules independently, or audit which raw records contributed to which report figures.

## The Solution

**You just write the CDR collection, record processing, metric aggregation, report generation, and anomaly alerting logic. Conductor handles ingestion retries, pattern analysis sequencing, and analytics audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Data ingestion, pattern analysis, anomaly detection, and report generation workers each process one layer of subscriber usage intelligence.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `uag_aggregate` | Aggregates processed records into usage metrics and detects anomalies (spikes, fraud patterns, revenue leakage). |
| **AlertWorker** | `uag_alert` | Raises alerts for detected anomalies that need immediate attention from operations or fraud teams. |
| **CollectCdrsWorker** | `uag_collect_cdrs` | Collects raw call detail records (CDRs) from network switches for a region and time period. |
| **ProcessWorker** | `uag_process` | Processes raw CDRs .  normalizing formats, enriching with subscriber metadata, and filtering duplicates. |
| **ReportWorker** | `uag_report` | Generates the usage analytics report with aggregated metrics, trends, and anomaly summaries. |

Workers simulate telecom operations .  provisioning, activation, billing ,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
uag_collect_cdrs
    │
    ▼
uag_process
    │
    ▼
uag_aggregate
    │
    ▼
uag_report
    │
    ▼
uag_alert

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
java -jar target/usage-analytics-1.0.0.jar

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
java -jar target/usage-analytics-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow uag_usage_analytics \
  --version 1 \
  --input '{"region": "us-east-1", "period": "sample-period"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w uag_usage_analytics -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real analytics systems .  your mediation platform for CDR collection, your data warehouse for processing and aggregation, your BI tools for report generation and alerting, and the workflow runs identically in production.

- **CollectCdrsWorker** (`uag_collect_cdrs`): pull raw CDRs from your mediation platform (CSG, Redknee/Optiva) or query CDR storage (Hadoop/Hive, Snowflake, or your data lake) for the specified region and period
- **ProcessWorker** (`uag_process`): run normalization and enrichment using your ETL platform (Apache Spark, Informatica, Talend) .  deduplicating records, mapping subscriber IDs, and converting to your canonical event schema
- **AggregateWorker** (`uag_aggregate`): compute usage aggregations (peak hour analysis, geographic distribution, top-N subscribers) and run anomaly detection models (fraud scoring, Wangiri detection, revenue assurance checks)
- **ReportWorker** (`uag_report`): generate the analytics report via your BI platform (Tableau, Power BI, Looker) or produce a PDF/dashboard export for stakeholder distribution
- **AlertWorker** (`uag_alert`): push anomaly alerts to your fraud management system (Subex, TEOCO), NOC dashboard (PagerDuty, Opsgenie), or revenue assurance platform for investigation

Change data ingestion sources or analysis algorithms and the analytics pipeline operates unchanged.

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
usage-analytics-usage-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/usageanalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UsageAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AlertWorker.java
│       ├── CollectCdrsWorker.java
│       ├── ProcessWorker.java
│       └── ReportWorker.java
└── src/test/java/usageanalytics/workers/
    ├── CollectCdrsWorkerTest.java        # 1 tests
    └── ReportWorkerTest.java        # 1 tests

```
