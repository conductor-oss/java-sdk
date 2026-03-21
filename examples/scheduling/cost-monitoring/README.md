# Cloud Cost Monitoring in Java Using Conductor :  Billing Collection, Trend Analysis, and Budget Alerts

A Java Conductor workflow example for cloud cost monitoring. collecting billing data across accounts, analyzing spending trends against historical patterns, and alerting when costs exceed budget limits or show unexpected growth.

## The Problem

You need to track cloud spending across accounts and services. Billing data must be collected from cloud providers, analyzed for trends (is spending growing faster than expected?), and alerts must fire when costs exceed budget limits or show anomalous spikes (someone left GPU instances running). By the time the monthly bill arrives, it's too late to act.

Without orchestration, cost monitoring is checking the AWS/GCP billing dashboard manually. Trend analysis runs in a separate spreadsheet, alerts are set up in a different tool, and there's no automated pipeline connecting billing data to budget enforcement. Cost overruns are discovered weeks after they start.

## The Solution

**You just write the billing data collection and budget threshold rules. Conductor handles the billing-to-alert pipeline, retries when cloud billing APIs are rate-limited, and a historical record of every cost check and budget alert.**

Each cost concern is an independent worker. billing collection, trend analysis, and budget alerting. Conductor runs them in sequence: collect current costs, analyze trends, then alert if thresholds are breached. Every cost check is tracked with billing data, trend analysis, and alert decisions. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers form the cost pipeline: CollectBillingWorker pulls spending data by service, AnalyzeTrendsWorker compares against budgets and flags anomalies, and CosAlertWorker fires when utilization exceeds budget thresholds.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `cos_analyze_trends` | Analyzes spending trends (increasing/decreasing), calculates budget utilization percentage, and flags cost anomalies by service |
| **CollectBillingWorker** | `cos_collect_billing` | Collects billing data for an account, returning total spend and a breakdown by service (compute, storage, network) |
| **CosAlertWorker** | `cos_alert_anomalies` | Sends a budget alert if spending exceeds 80% of budget, with critical severity above 90% |

Workers implement scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic. the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
cos_collect_billing
    │
    ▼
cos_analyze_trends
    │
    ▼
cos_alert_anomalies

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
java -jar target/cost-monitoring-1.0.0.jar

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
java -jar target/cost-monitoring-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cost_monitoring_419 \
  --version 1 \
  --input '{"accountId": "TEST-001", "billingPeriod": "sample-billingPeriod", "budgetLimit": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cost_monitoring_419 -s COMPLETED -c 5

```

## How to Extend

Each worker manages one cost concern. connect the billing collector to AWS Cost Explorer or GCP Billing API, the alerting worker to Slack or email, and the collect-analyze-alert workflow stays the same.

- **AnalyzeTrendsWorker** (`cos_analyze_trends`): compute daily/weekly spend trends, identify top cost drivers, and detect anomalous spending patterns
- **CollectBillingWorker** (`cos_collect_billing`): query AWS Cost Explorer, GCP Billing, or Azure Cost Management APIs for real billing data by service and tag
- **CosAlertWorker** (`cos_alert_anomalies`): send budget alerts via Slack/email to engineering leads, auto-create Jira tickets for cost optimization, or trigger auto-scaling policies

Hook up your cloud billing APIs and Slack notifications, and the cost monitoring pipeline operates in production unmodified.

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
cost-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/costmonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CostMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTrendsWorker.java
│       ├── CollectBillingWorker.java
│       └── CosAlertWorker.java
└── src/test/java/costmonitoring/workers/
    └── CosAlertWorkerTest.java        # 2 tests

```
