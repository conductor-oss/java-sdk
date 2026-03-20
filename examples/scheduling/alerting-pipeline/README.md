# Alerting Pipeline in Java Using Conductor :  Metric Evaluation, Anomaly Detection, Suppression, and Alert Dispatch

A Java Conductor workflow example for building an alerting pipeline .  evaluating metric rules against thresholds, detecting anomalies, suppressing duplicate or flapping alerts, and dispatching notifications to the right channels.

## The Problem

You need to evaluate incoming metrics against alerting rules. When a metric exceeds its threshold, you need to detect whether it's a genuine anomaly (not just noise), suppress duplicate alerts if the same condition was already flagged recently, and send notifications to the appropriate channel. A naive threshold check fires too many alerts; a proper pipeline requires anomaly detection and suppression logic.

Without orchestration, alerting logic is scattered .  threshold checks in one script, suppression in another, notification dispatch in a third. Alerts fire for every metric sample above threshold instead of once per incident. Engineers suffer alert fatigue from duplicates and false positives.

## The Solution

**You just write the threshold evaluation and notification dispatch logic. Conductor handles sequential evaluation with conditional routing, retries on notification delivery failures, and a full record of every alert evaluated, suppressed, or dispatched.**

Each alerting concern is an independent worker .  rule evaluation, anomaly detection, suppression checking, and alert dispatch. Conductor runs them in sequence, ensuring suppression is checked before any alert is sent. Every alert evaluation is tracked with full context ,  you can see which metrics triggered, which were suppressed, and which actually fired. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

The alerting pipeline chains DetectAnomalyWorker to flag metric deviations, EvaluateRulesWorker to apply severity-based firing criteria, and then routes to either SendAlertWorker for dispatch or SuppressAlertWorker for duplicate suppression.

| Worker | Task | What It Does |
|---|---|---|
| **DetectAnomalyWorker** | `alt_detect_anomaly` | Compares a metric's current value against its threshold and flags anomalies, returning severity and a deviation score |
| **EvaluateRulesWorker** | `alt_evaluate_rules` | Evaluates alerting rules based on severity .  decides whether to fire or suppress the alert and selects the notification channel |
| **SendAlertWorker** | `alt_send_alert` | Dispatches the alert to the selected channel (e.g., PagerDuty) and returns an alert ID for tracking |
| **SuppressAlertWorker** | `alt_suppress_alert` | Suppresses alerts that don't meet firing criteria, logging the suppression reason for audit |

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
alt_detect_anomaly
    │
    ▼
alt_evaluate_rules
    │
    ▼
SWITCH (alt_switch_ref)
    ├── fire: alt_send_alert
    └── default: alt_suppress_alert
```

## Example Output

```
=== Example 413: Alerting Pipeline ===

Step 1: Registering task definitions...
  Registered: alt_detect_anomaly, alt_evaluate_rules, alt_send_alert, alt_suppress_alert

Step 2: Registering workflow 'alerting_pipeline_413'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [detect]
  [evaluate] Evaluating rules for
  [alert] Firing
  [suppress] Suppressing alert for

  Status: COMPLETED
  Output: {isAnomaly=..., severity=..., anomalyScore=..., deviation=...}

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
java -jar target/alerting-pipeline-1.0.0.jar
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
java -jar target/alerting-pipeline-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow alerting_pipeline_413 \
  --version 1 \
  --input '{"metricName": "sample-name", "cpu_usage_percent": "sample-cpu-usage-percent", "currentValue": "sample-currentValue", "threshold": "sample-threshold"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w alerting_pipeline_413 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one alerting stage .  connect the rule evaluator to Prometheus or Datadog metrics, the dispatch worker to PagerDuty or Slack, and the evaluate-detect-suppress-dispatch workflow stays the same.

- **DetectAnomalyWorker** (`alt_detect_anomaly`): implement statistical anomaly detection (z-score, MAD, DBSCAN) or call an ML-based anomaly detection service
- **EvaluateRulesWorker** (`alt_evaluate_rules`): check metrics against real thresholds from your alerting config (Prometheus alerting rules, Datadog monitors, PagerDuty thresholds)
- **SendAlertWorker** (`alt_send_alert`): dispatch to real channels. PagerDuty, Slack, OpsGenie, email .  with severity-based routing

Plug in Prometheus metrics and PagerDuty dispatch, and the detect-evaluate-route orchestration transfers directly to production.

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
alerting-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/alertingpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AlertingPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectAnomalyWorker.java
│       ├── EvaluateRulesWorker.java
│       ├── SendAlertWorker.java
│       └── SuppressAlertWorker.java
└── src/test/java/alertingpipeline/workers/
    └── DetectAnomalyWorkerTest.java        # 3 tests
```
