# Threshold Alerting in Java Using Conductor :  Metric Check with Warning, Critical, and OK Routing

A Java Conductor workflow example for threshold alerting .  checking a metric against warning and critical thresholds, then routing to the appropriate action (log OK, send warning, page on-call) based on which threshold is breached.

## The Problem

You monitor a metric (CPU usage, error rate, response time) against two thresholds .  warning (80%) and critical (95%). Below warning: everything is fine, log and move on. Between warning and critical: send a warning alert to Slack so the team is aware. Above critical: page the on-call engineer immediately. The routing must be automatic and the thresholds must be configurable per metric.

Without orchestration, threshold alerting is implemented with if/else chains in monitoring scripts. Each metric has its own alerting code, thresholds are hardcoded, and adding a new severity level means modifying every alerting script. There's no unified view of which metrics are in warning vs critical state.

## The Solution

**You just write the threshold checks and severity-specific handlers. Conductor handles severity-based SWITCH routing, retries on notification delivery failures, and a record of every metric evaluation with the value, thresholds, and routing decision.**

A metric checker worker evaluates the current value against both thresholds. Conductor's SWITCH task routes to the appropriate handler .  log OK, send warning, or page on-call. Every metric evaluation is tracked with the value, thresholds, and routing decision. Changing thresholds is a workflow input change, not a code change. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

CheckMetricWorker evaluates a metric against warning and critical thresholds, then Conductor routes to LogOkWorker for normal values, SendWarningWorker for warning-level breaches, or PageOncallWorker to page the on-call engineer for critical conditions.

| Worker | Task | What It Does |
|---|---|---|
| **CheckMetricWorker** | `th_check_metric` | Compares a metric's current value against warning and critical thresholds, returning a severity level (ok/warning/critical) |
| **LogOkWorker** | `th_log_ok` | Logs that the metric is within normal range with a healthy status |
| **PageOncallWorker** | `th_page_oncall` | Pages the on-call engineer via PagerDuty for critical threshold breaches, creating an incident with a tracking ID |
| **SendWarningWorker** | `th_send_warning` | Sends a warning notification via Slack when the metric exceeds the warning threshold |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
th_check_metric
    │
    ▼
SWITCH (th_switch_ref)
    ├── critical: th_page_oncall
    ├── warning: th_send_warning
    └── default: th_log_ok
```

## Example Output

```
=== Example 423: Threshold Alerting ===

Step 1: Registering task definitions...
  Registered: th_check_metric, th_page_oncall, th_send_warning, th_log_ok

Step 2: Registering workflow 'threshold_alerting_423'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [check]
  [ok]
  [page] CRITICAL: Paging on-call for
  [warning] Sending warning for

  Status: COMPLETED
  Output: {severity=..., value=..., logged=..., paged=...}

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
  --workflow threshold_alerting_423 \
  --version 1 \
  --input '{"metricName": "sample-name", "error_rate_percent": "sample-error-rate-percent", "currentValue": "sample-currentValue", "warningThreshold": "sample-warningThreshold", "criticalThreshold": "sample-criticalThreshold"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w threshold_alerting_423 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one alerting path .  connect the metric checker to Prometheus or CloudWatch, the critical handler to PagerDuty, the warning handler to Slack, and the check-route-respond workflow stays the same.

- **CheckMetricWorker** (`th_check_metric`): query real metrics from Prometheus, CloudWatch, Datadog, or InfluxDB and evaluate against configured thresholds
- **LogOkWorker** (`th_log_ok`): record healthy metric samples to your time-series database for baseline computation
- **PageOncallWorker** (`th_page_oncall`): create PagerDuty/OpsGenie incidents for critical alerts with severity and runbook links

Connect to Prometheus for metrics and PagerDuty for paging, and the threshold routing workflow adapts to production with no changes.

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
│       ├── CheckMetricWorker.java
│       ├── LogOkWorker.java
│       ├── PageOncallWorker.java
│       └── SendWarningWorker.java
└── src/test/java/thresholdalerting/workers/
    └── CheckMetricWorkerTest.java        # 2 tests
```
