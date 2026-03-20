# Uptime Monitoring in Java Using Conductor :  Endpoint Checks, Result Logging, SLA Calculation, and Reporting

A Java Conductor workflow example for uptime monitoring .  checking endpoint availability, logging results, calculating SLA compliance against targets, and generating uptime reports.

## The Problem

You need to monitor whether your endpoints are up and meeting SLA commitments. Each endpoint must be checked for availability (is it returning the expected status code?), results must be logged for historical analysis, SLA compliance must be calculated (are you meeting your 99.9% uptime guarantee?), and reports must be generated for stakeholders and customers.

Without orchestration, uptime monitoring is a simple ping script that checks URLs and sends alerts. Historical data is not preserved, SLA calculations are done manually in spreadsheets, and uptime reports are created ad hoc when a customer asks. There's no automated pipeline from check to report.

## The Solution

**You just write the availability checks and SLA compliance calculations. Conductor handles the check-log-calculate-report pipeline, retries when endpoints are unreachable or metric stores are slow, and a full history of every monitoring cycle with response times and SLA standings.**

Each monitoring concern is an independent worker .  endpoint checking, result logging, SLA calculation, and report generation. Conductor runs them in sequence: check the endpoint, log the result, calculate SLA compliance, then generate the report. Every monitoring run is tracked with the check result, response time, and SLA standing. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

CheckEndpointWorker probes each endpoint for availability and response time, LogResultWorker records the outcome for trending, CalculateSlaWorker computes uptime percentages against your SLA target, and UmReportWorker generates the stakeholder-facing report.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateSlaWorker** | `um_calculate_sla` | Calculates current SLA percentage from total checks, determining whether the SLA target is met |
| **CheckEndpointWorker** | `um_check_endpoint` | Checks an endpoint's availability, returning HTTP status, response time in milliseconds, and up/down status |
| **LogResultWorker** | `um_log_result` | Logs the endpoint check result (endpoint, status) for historical trending |
| **UmReportWorker** | `um_report` | Generates an uptime/SLA report summarizing availability metrics for stakeholders |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
um_check_endpoint
    │
    ▼
um_log_result
    │
    ▼
um_calculate_sla
    │
    ▼
um_report
```

## Example Output

```
=== Example 420: Uptime Monitoring ===

Step 1: Registering task definitions...
  Registered: um_check_endpoint, um_log_result, um_calculate_sla, um_report

Step 2: Registering workflow 'uptime_monitoring_420'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [sla] Calculating SLA
  [check] Checking
  [log] Logging:
  [report] Generating SLA report

  Status: COMPLETED
  Output: {currentSla=..., slaMet=..., totalChecks=..., httpStatus=...}

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
java -jar target/uptime-monitoring-1.0.0.jar
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
java -jar target/uptime-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow uptime_monitoring_420 \
  --version 1 \
  --input '{"endpoint": "/api/v1/resource", "https://api.example.com/health": "sample-https://api.example.com/health", "expectedStatus": "pending", "slaTarget": "sample-slaTarget"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w uptime_monitoring_420 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one monitoring step .  connect the endpoint checker to make real HTTP requests, the SLA calculator to compute against your 99.9% target, and the check-log-calculate-report workflow stays the same.

- **CalculateSlaWorker** (`um_calculate_sla`): compute real SLA percentages over rolling windows, accounting for planned maintenance exclusions
- **CheckEndpointWorker** (`um_check_endpoint`): make real HTTP/TCP/DNS checks against your endpoints, measuring response time and validating response content
- **LogResultWorker** (`um_log_result`): persist check results to InfluxDB/TimescaleDB for time-series analysis and trending

Swap in real HTTP probes and your SLA dashboard, and the monitoring pipeline runs continuously without any orchestration changes.

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
uptime-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/uptimemonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UptimeMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateSlaWorker.java
│       ├── CheckEndpointWorker.java
│       ├── LogResultWorker.java
│       └── UmReportWorker.java
└── src/test/java/uptimemonitoring/workers/
    └── CheckEndpointWorkerTest.java        # 2 tests
```
