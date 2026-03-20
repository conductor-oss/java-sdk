# Threshold Alerting in Java with Conductor :  Check Metric, Route by Severity via SWITCH

Automates threshold-based alerting using [Conductor](https://github.com/conductor-oss/conductor). This workflow checks a metric value against warning and critical thresholds, then routes to the appropriate action: logging for normal values, sending a Slack warning for elevated values, or paging the on-call engineer for critical breaches.## The Right Alert to the Right Person

CPU usage is at 87%. Is that a problem? It depends on the thresholds. Below 70% is fine: just log it. Between 70% and 90% is a warning, send a Slack message so the team is aware. Above 90% is critical, page the on-call engineer immediately. Each severity level needs a different response, and the check needs to happen reliably every time, with the right routing for each outcome.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the threshold checks and notification handlers. Conductor handles severity-based routing, delivery retries, and alerting decision tracking.**

`CheckMetricWorker` evaluates the current metric value against configured warning and critical thresholds, classifying the status. Conductor's routing directs the workflow to the appropriate handler: `LogOkWorker` records the metric value for normal readings. `SendWarningWorker` sends a notification to the team channel for warning-level breaches. `PageOncallWorker` pages the on-call engineer via PagerDuty or Opsgenie for critical breaches. Conductor records every alert decision .  metric value, threshold comparison, and action taken ,  for alerting analytics and threshold tuning.

### What You Write: Workers

Four workers handle threshold alerting. Checking the metric value, then routing to the appropriate action: logging, warning via Slack, or paging the on-call engineer.

| Worker | Task | What It Does |
|---|---|---|
| **CheckMetric** | `th_check_metric` | Checks a metric value against warning and critical thresholds, determining the severity level. |
| **LogOk** | `th_log_ok` | Logs that the metric is within normal range. |
| **PageOncall** | `th_page_oncall` | Pages the on-call engineer for critical alerts. |
| **SendWarning** | `th_send_warning` | Sends a warning alert to Slack. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
Input -> CheckMetric -> LogOk -> PageOncall -> SendWarning -> Output
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
java -jar target/threshold-alerting-1.0.0.jar
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
java -jar target/threshold-alerting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow threshold_alerting \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w threshold_alerting -s COMPLETED -c 5
```

## How to Extend

Each worker handles one severity response .  replace the simulated calls with Prometheus metric queries, Slack Webhooks, or PagerDuty Events API for real threshold monitoring and escalation, and the alerting workflow runs unchanged.

- **CheckMetric** (`ta_check_metric`): query Prometheus, Datadog, or CloudWatch for real-time metric values with configurable warning and critical thresholds per metric and per environment
- **LogOk** (`ta_log_ok`): record that the metric is within normal range for audit and trend tracking, pushing the data point to a time-series database or logging platform
- **SendWarning** (`ta_send_warning`): post to Slack channels via the Webhooks API, send email via SendGrid, or create Jira tickets for non-urgent investigation when warning thresholds are breached
- **PageOncall** (`ta_page_oncall`): integrate with PagerDuty Events API v2, Opsgenie Alert API, or VictorOps for real on-call paging with escalation policies and acknowledgment tracking when critical thresholds are breached

Plug in PagerDuty and Slack webhooks for real notifications; the alerting workflow keeps the same severity-routing interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
threshold-alerting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/thresholdalerting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ThresholdAlertingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckMetric.java
│       ├── LogOk.java
│       ├── PageOncall.java
│       └── SendWarning.java
└── src/test/java/thresholdalerting/workers/
    ├── CheckMetricTest.java        # 10 tests
    ├── LogOkTest.java        # 8 tests
    ├── PageOncallTest.java        # 7 tests
    └── SendWarningTest.java        # 8 tests
```
