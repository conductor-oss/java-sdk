# Building Energy Management in Java with Conductor :  Consumption Monitoring, Pattern Analysis, and Optimization

A Java Conductor workflow example that orchestrates building energy management .  collecting kWh consumption readings across time intervals, identifying usage patterns like peak-hour HVAC loads, generating optimization recommendations with projected savings, and producing energy reports. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Energy Optimization Needs Orchestration

Managing energy consumption in a building involves a pipeline where each step depends on what came before. You pull meter readings over a time period to get per-hour kW consumption and total kWh. You feed those readings into pattern analysis to identify when demand spikes .  midday HVAC peaks, overnight baseline loads, equipment-dominant periods. Those patterns drive optimization recommendations: shift HVAC schedules, reduce lighting during low-occupancy hours, project dollar savings. Finally, you compile everything into a report for facilities management.

If the meter data pull fails partway through, you need to know exactly where to resume. If the pattern analysis discovers an anomaly, that needs to propagate to the optimizer. Without orchestration, you'd build a monolithic energy analysis script that mixes data fetching, statistical analysis, optimization logic, and report generation .  making it impossible to swap out your meter data source, test pattern detection independently, or observe which step is the bottleneck.

## How This Workflow Solves It

**You just write the energy management workers. Consumption monitoring, pattern analysis, optimization recommendation, and reporting. Conductor handles meter-to-report sequencing, data fetch retries, and per-stage timing metrics for pipeline performance analysis.**

Each stage of the energy analysis pipeline is an independent worker .  monitor consumption, analyze patterns, generate optimizations, produce reports. Conductor sequences them, threads consumption readings and pattern data between steps, retries if a meter data fetch times out, and tracks exactly how long each analysis stage takes. You get a reliable energy management pipeline without writing state-passing or retry logic.

### What You Write: Workers

Four workers analyze building energy: MonitorConsumptionWorker reads kWh meter data, AnalyzePatternsWorker identifies peak-hour loads, OptimizeWorker generates savings recommendations, and ReportWorker produces the facilities management summary.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzePatternsWorker** | `erg_analyze_patterns` | Analyzes energy usage patterns and identifies peak hours. |
| **MonitorConsumptionWorker** | `erg_monitor_consumption` | Monitors energy consumption and returns readings with total kWh. |
| **OptimizeWorker** | `erg_optimize` | Generates optimization recommendations and projected savings. |
| **ReportWorker** | `erg_report` | Generates an energy report. |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs .  the workflow and alerting logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
erg_monitor_consumption
    │
    ▼
erg_analyze_patterns
    │
    ▼
erg_optimize
    │
    ▼
erg_report
```

## Example Output

```
=== Energy Management Demo ===

Step 1: Registering task definitions...
  Registered: erg_monitor_consumption, erg_analyze_patterns, erg_optimize, erg_report

Step 2: Registering workflow 'energy_management_demo'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze] Identifying usage patterns for
  [monitor] Building
  [optimize] Generating optimization plan for
  [report] Generated report

  Status: COMPLETED
  Output: {patterns=..., peakHours=..., baselineKw=..., consumption=...}

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
java -jar target/energy-management-1.0.0.jar
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
java -jar target/energy-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow energy_management_demo \
  --version 1 \
  --input '{"buildingId": "BLDG-A1", "BLDG-A1": "period", "period": "2024-01", "2024-01": "sample-2024-01"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w energy_management_demo -s COMPLETED -c 5
```

## How to Extend

Connect MonitorConsumptionWorker to your smart meter data feeds, AnalyzePatternsWorker to your energy analytics platform, and OptimizeWorker to your building management system. The workflow definition stays exactly the same.

- **MonitorConsumptionWorker** (`erg_monitor_consumption`): pull real meter readings from your BMS (Modbus, BACnet), smart meter API, or time-series database to return per-interval kW readings and total kWh
- **AnalyzePatternsWorker** (`erg_analyze_patterns`): run actual statistical analysis or ML models to detect HVAC-dominant periods, peak demand windows, and baseline load profiles
- **OptimizeWorker** (`erg_optimize`): integrate with building control systems to generate actionable recommendations (HVAC schedule shifts, demand response participation) with projected cost savings
- **ReportWorker** (`erg_report`): generate PDF or HTML energy reports via a templating engine and distribute to facilities managers via email or a building management dashboard

Connect each worker to your smart meters or energy analytics platform while preserving output fields, and the analysis pipeline stays the same.

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
energy-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/energymanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EnergyManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzePatternsWorker.java
│       ├── MonitorConsumptionWorker.java
│       ├── OptimizeWorker.java
│       └── ReportWorker.java
└── src/test/java/energymanagement/workers/
    ├── AnalyzePatternsWorkerTest.java        # 8 tests
    ├── MonitorConsumptionWorkerTest.java        # 8 tests
    ├── OptimizeWorkerTest.java        # 8 tests
    └── ReportWorkerTest.java        # 7 tests
```
