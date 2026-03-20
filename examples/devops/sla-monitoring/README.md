# SLA Monitoring in Java with Conductor

Automates SLA/SLO monitoring using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects service level indicators (availability, latency), calculates error budget burn rate, evaluates compliance against SLO targets, and generates stakeholder reports.## Burning Through Your Error Budget

Your payment-api promises 99.95% availability and p99 latency under 200ms. How much error budget do you have left this month? Are you burning it faster than expected? This workflow answers those questions by collecting SLIs, computing the remaining error budget (e.g., 21.6 minutes of allowed downtime left), and flagging when you are at risk of violating your SLA before the month ends.

Without orchestration, you'd query Prometheus manually, open a spreadsheet to calculate remaining error budget, eyeball whether the burn rate is accelerating, and send a monthly email to stakeholders with numbers you copied by hand. If the SLI collection fails or returns stale data, the budget calculation is wrong, and you either miss a breach or cry wolf. There's no audit trail of which SLOs were evaluated, what the burn rate looked like over time, or when the team was notified.

## The Solution

**You write the SLI collection and budget calculation logic. Conductor handles the measurement-to-report pipeline and compliance audit history.**

Each stage of the SLA monitoring pipeline is a simple, independent worker. The SLI collector gathers availability and latency measurements for the target service over the monitoring window. Uptime percentage, request success rate, P50/P95/P99 latency. The budget calculator computes how much error budget remains (e.g., 73% remaining, 21.6 minutes of allowed downtime left this month) and whether the current burn rate will exhaust it before the period ends. The compliance evaluator compares actual SLIs against contractual SLO targets and flags violations or at-risk services. The reporter generates a stakeholder-ready summary with per-SLO compliance status, budget remaining, and trend indicators. Conductor executes them in strict sequence, ensures budget calculations only run on fresh SLI data, retries if the metrics backend is temporarily unavailable, and tracks every monitoring run so you can audit SLA compliance history. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers run the SLA monitoring cycle. Measuring service level indicators, computing error budget burn rate, evaluating compliance, and generating stakeholder reports.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateBudgetWorker** | `sla_calculate_budget` | Computes remaining error budget and burn rate (e.g., 73% remaining, 21.6 min left this month) |
| **CollectSlisWorker** | `sla_collect_slis` | Measures current service level indicators: availability percentage and p99 latency |
| **EvaluateComplianceWorker** | `sla_evaluate_compliance` | Determines whether the service is currently within SLA compliance based on error budget |
| **ReportWorker** | `sla_report` | Generates an SLA compliance report for stakeholders with budget status and trend data |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
sla_collect_slis
    │
    ▼
sla_calculate_budget
    │
    ▼
sla_evaluate_compliance
    │
    ▼
sla_report
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
java -jar target/sla-monitoring-1.0.0.jar
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
java -jar target/sla-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sla_monitoring_workflow \
  --version 1 \
  --input '{"service": "test-value", "sloTarget": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sla_monitoring_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one SLA monitoring step .  plug in Prometheus for SLI collection, Datadog for error budget tracking, or Grafana for stakeholder dashboards, and the monitoring workflow runs unchanged.

- **CollectSlisWorker** (`sla_collect_slis`): query Prometheus for availability and latency SLIs, pull from Datadog SLI API, or aggregate CloudWatch metrics for uptime and response time measurements
- **CalculateBudgetWorker** (`sla_calculate_budget`): compute error budgets using Prometheus recording rules, Datadog SLO widgets, or Google Cloud SLO Monitoring, tracking burn rate against the monthly/quarterly budget window
- **EvaluateComplianceWorker** (`sla_evaluate_compliance`): compare actual SLIs against contractual SLA thresholds stored in a config file or database, flagging breaches and at-risk services based on current burn rate projections
- **ReportWorker** (`sla_report`): generate stakeholder reports via email, post SLA compliance summaries to Slack or Confluence, or push metrics to a Grafana dashboard for executive-level SLO visibility

Connect to your metrics backend for live SLI data; the compliance pipeline preserves the same reporting interface.

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
sla-monitoring-sla-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/slamonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateBudgetWorker.java
│       ├── CollectSlisWorker.java
│       ├── EvaluateComplianceWorker.java
│       └── ReportWorker.java
└── src/test/java/slamonitoring/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
