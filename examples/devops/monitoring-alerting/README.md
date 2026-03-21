# Monitoring Alerting in Java with Conductor

Orchestrates a monitoring and alerting pipeline using [Conductor](https://github.com/conductor-oss/conductor). Incoming alerts are evaluated for severity, deduplicated against recent history, enriched with deployment context, and routed to the appropriate on-call channel.

## When Every Alert Counts

A metric threshold is breached and a raw alert fires. Before anyone gets paged, the system needs to decide whether this alert is real or a duplicate, determine its severity, pull in deployment context (was there a recent deploy?), and route the notification to the right Slack channel or PagerDuty escalation. Doing this manually means flapping alerts wake people up at 3 AM for problems that already have open incidents.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the alert evaluation and routing logic. Conductor handles deduplication sequencing, severity-based routing, and delivery tracking.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging .  your workers call the infrastructure APIs.

### What You Write: Workers

These four workers form the alert-processing pipeline, from severity evaluation through deduplication, enrichment, and channel routing.

| Worker | Task | What It Does |
|---|---|---|
| **DeduplicateWorker** | `ma_deduplicate` | Checks whether this alert is a duplicate of a recently-fired alert and suppresses it if so |
| **EnrichWorker** | `ma_enrich` | Adds deployment context (recent deploys, config changes) to the alert payload |
| **EvaluateWorker** | `ma_evaluate` | Evaluates the incoming alert name and metric value to determine severity (info/warning/critical) |
| **RouteWorker** | `ma_route` | Routes the enriched alert to the correct notification channel based on severity (e.g., Slack for warnings, PagerDuty for critical) |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
ma_evaluate
    │
    ▼
ma_deduplicate
    │
    ▼
ma_enrich
    │
    ▼
ma_route

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
java -jar target/monitoring-alerting-1.0.0.jar

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
java -jar target/monitoring-alerting-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow monitoring_alerting_workflow \
  --version 1 \
  --input '{"alertName": "test", "metric": "sample-metric", "value": "sample-value"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w monitoring_alerting_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one alerting concern .  plug in Datadog, PagerDuty, or Prometheus Alertmanager for real evaluation and routing, and the alerting pipeline runs unchanged.

- **DeduplicateWorker** (`ma_deduplicate`): query a Redis sorted set or Elasticsearch index of recent alerts to check for duplicates within a time window
- **EnrichWorker** (`ma_enrich`): call your deployment tracker API (e.g., Argo CD, Spinnaker, or a custom deploy log) to attach recent change context
- **EvaluateWorker** (`ma_evaluate`): integrate with Datadog Monitors API or Prometheus Alertmanager to pull threshold definitions and compute severity

Swap in your real monitoring APIs and the alert pipeline continues to operate with the same contract.

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
monitoring-alerting-monitoring-alerting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/monitoringalerting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeduplicateWorker.java
│       ├── EnrichWorker.java
│       ├── EvaluateWorker.java
│       └── RouteWorker.java
└── src/test/java/monitoringalerting/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
