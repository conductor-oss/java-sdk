# Building Automation in Java with Conductor

A Java Conductor workflow example that orchestrates building automation. monitoring HVAC, lighting, and occupancy systems on a per-floor basis, generating energy optimization recommendations based on current conditions, scheduling the optimizations for execution, and applying adjustments to building systems. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Building Automation Needs Orchestration

Optimizing a building's HVAC and lighting systems requires a pipeline where each step depends on the previous one. You monitor building systems to get current HVAC status, lighting levels, and occupancy counts for a specific floor. You feed that data into an optimizer that identifies energy savings opportunities. reducing HVAC output in unoccupied zones, dimming lights in daylit areas. You schedule those optimizations into a timed execution plan for the building management system. Finally, you apply the adjustments to the actual HVAC and lighting controllers.

Each step depends on the previous one. the optimizer needs current system states, the scheduler needs optimization recommendations, and the adjuster needs the schedule. If the BMS monitoring poll fails, you do not want stale occupancy data driving optimization decisions. Without orchestration, you'd build a monolithic building controller that mixes sensor polling, optimization algorithms, scheduling logic, and actuator control,  making it impossible to swap optimization strategies, test scheduling independently, or track which sensor readings led to which energy savings.

## The Solution

**You just write the building automation workers. System monitoring, energy optimization, schedule creation, and controller adjustment. Conductor handles sensor-to-actuator ordering, BMS polling retries, and recorded optimization decisions for energy savings verification.**

Each worker handles one IoT operation. data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Four workers optimize building energy: MonitorSystemsWorker reads HVAC, lighting, and occupancy data, OptimizeWorker identifies savings opportunities, ScheduleWorker creates timed execution plans, and AdjustWorker applies changes to building controllers.

| Worker | Task | What It Does |
|---|---|---|
| **AdjustWorker** | `bld_adjust` | Applies scheduled optimizations to HVAC and lighting controllers on the target floor. |
| **MonitorSystemsWorker** | `bld_monitor_systems` | Monitors HVAC status, lighting levels, and occupancy for a building floor. |
| **OptimizeWorker** | `bld_optimize` | Analyzes HVAC, lighting, and occupancy data to generate energy optimization recommendations and projected savings. |
| **ScheduleWorker** | `bld_schedule` | Schedules optimization recommendations into a timed execution plan for the building management system. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
bld_monitor_systems
    │
    ▼
bld_optimize
    │
    ▼
bld_schedule
    │
    ▼
bld_adjust

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
java -jar target/building-automation-1.0.0.jar

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
java -jar target/building-automation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow building_automation_demo \
  --version 1 \
  --input '{"buildingId": "TEST-001", "floor": "sample-floor"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w building_automation_demo -s COMPLETED -c 5

```

## How to Extend

Connect MonitorSystemsWorker to your BMS (Siemens, Honeywell, Johnson Controls), OptimizeWorker to your energy analytics engine, and AdjustWorker to your HVAC and lighting controllers. The workflow definition stays exactly the same.

- **MonitorSystemsWorker** (`bld_monitor_systems`): poll real BMS data via BACnet, Modbus, or your building management platform (Honeywell, Johnson Controls, Siemens Desigo) for HVAC setpoints, lighting levels, and occupancy sensor counts
- **OptimizeWorker** (`bld_optimize`): run real optimization algorithms that factor in utility rates, occupancy patterns, weather forecasts, and comfort constraints to generate energy-saving recommendations
- **ScheduleWorker** (`bld_schedule`): create scheduled commands in your BMS or building automation controller, coordinating HVAC ramp-up times and lighting scene transitions
- **AdjustWorker** (`bld_adjust`): send real setpoint changes to HVAC controllers and dimming commands to lighting systems via BACnet write or your BMS API

Connect each worker to your BMS or HVAC controllers while preserving output fields, and the optimization workflow operates without modification.

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
building-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/buildingautomation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BuildingAutomationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AdjustWorker.java
│       ├── MonitorSystemsWorker.java
│       ├── OptimizeWorker.java
│       └── ScheduleWorker.java
└── src/test/java/buildingautomation/workers/
    ├── MonitorSystemsWorkerTest.java        # 2 tests
    └── OptimizeWorkerTest.java        # 2 tests

```
