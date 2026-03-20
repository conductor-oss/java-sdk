# SLA Monitoring in Java Using Conductor :  Request Processing, WAIT Task with Start/End Timestamps, Duration Calculation, and SLA Compliance Check

A Java Conductor workflow example for measuring human approval response times against SLA targets .  processing a request, pausing at a WAIT task while Conductor automatically records the start and end timestamps, then calculating the actual wait duration in milliseconds and comparing it against the SLA threshold to determine compliance. The SLA metrics worker reads `waitStartTime` and `waitEndTime` directly from the WAIT task's metadata, computes the delta, and flags whether the approver responded within the agreed SLA window. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Human Approval Steps Need SLA Tracking and Compliance Reporting

When a workflow pauses for human approval, you need to track how long the human takes and whether the response time meets your SLA. The workflow processes the request, records the start time, pauses for human approval, then calculates the wait duration and checks SLA compliance. If the SLA metrics recording fails, you need to retry it without asking the human to re-approve.

## The Solution

**You just write the request-processing and SLA-metrics workers. Conductor handles the timestamp tracking and the durable approval wait.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

SlaProcessWorker prepares the request before the SLA clock starts, and SlaRecordMetricsWorker computes wait duration and compliance, the approval timestamp tracking happens automatically via the WAIT task metadata.

| Worker | Task | What It Does |
|---|---|---|
| **SlaProcessWorker** | `sla_process` | Processes the incoming request and marks it ready for human approval .  this is the pre-approval step that prepares the request before the SLA clock starts |
| *WAIT task* | `sla_wait` | Pauses for human approval via `POST /tasks/{taskId}`. Conductor automatically records startTime and endTime on the task, which the next step uses to calculate actual wait duration | Built-in Conductor WAIT .  no worker needed |
| **SlaRecordMetricsWorker** | `sla_record_metrics` | Calculates the approval wait duration from the WAIT task's startTime and endTime, compares it against the slaMs threshold, and outputs waitDurationMs (actual) and slaMet (true/false compliance flag) |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sla_process
    │
    ▼
sla_wait [WAIT]
    │
    ▼
sla_record_metrics
```

## Example Output

```
=== SLA Monitoring Demo: Track Time-to-Approval SLA Metrics ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'sla_monitoring_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [sla_process] Processing request...
  [sla_record_metrics] Wait duration:

  Status: COMPLETED
  Output: {processed=..., waitDurationMs=..., slaMet=..., slaMs=...}

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
  --workflow sla_monitoring_demo \
  --version 1 \
  --input '{"slaMs": "sample-slaMs"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sla_monitoring_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one side of the SLA pipeline .  connect your request system for processing and your monitoring platform (Datadog, Grafana, PagerDuty) for SLA compliance alerts, and the timing workflow stays the same.

- **SlaProcessWorker** (`sla_process`): enrich the request with SLA targets from a configuration database, and start SLA timers in your monitoring system
- **SlaRecordMetricsWorker** (`sla_record_metrics`): push SLA metrics to Datadog, Grafana, or a custom dashboard, trigger alerts on SLA breaches, and update compliance reports

Connect Datadog or Grafana for real SLA reporting and the timestamp tracking and compliance calculation flow remains unchanged.

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
sla-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/slamonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SlaMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SlaProcessWorker.java
│       └── SlaRecordMetricsWorker.java
└── src/test/java/slamonitoring/workers/
    ├── SlaProcessWorkerTest.java        # 4 tests
    └── SlaRecordMetricsWorkerTest.java        # 9 tests
```
