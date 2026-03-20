# Logistics Optimization in Java with Conductor :  Demand Analysis, Route Optimization, Vehicle Scheduling, and Fleet Dispatch

A Java Conductor workflow example for logistics optimization .  analyzing demand across 40+ orders distributed by ZIP code, computing optimal delivery routes, scheduling vehicles based on capacity and availability, and dispatching the fleet with optimized assignments. Uses [Conductor](https://github.## The Problem

You need to optimize logistics for a batch of 40+ orders spread across different ZIP codes. Demand must be analyzed to identify geographic clusters and volume patterns. Routes must be optimized across all delivery points to minimize total mileage. Vehicles must be scheduled based on load capacity, driver hours-of-service, and delivery time windows. The fleet must then be dispatched with each driver receiving their optimized stop list.

Without orchestration, the logistics planner manually groups orders by region, estimates routes on a map, and assigns trucks by intuition. If the route optimizer finds a better solution after vehicles have been scheduled, there is no easy way to propagate the change. When demand spikes unexpectedly, re-running the entire pipeline wastes the demand analysis already computed.

## The Solution

**You just write the logistics workers. Demand analysis, route computation, vehicle scheduling, and fleet dispatch. Conductor handles data flow between stages, optimizer retries on timeout, and recorded route solutions for continuous improvement.**

Each phase of the logistics optimization pipeline is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so demand analysis feeds route optimization, optimized routes drive vehicle scheduling, and scheduling results determine dispatch assignments. If the route optimizer times out on a large order set, Conductor retries without re-analyzing demand. Every demand cluster, route solution, schedule assignment, and dispatch confirmation is recorded for cost analysis and continuous improvement.

### What You Write: Workers

Four workers optimize the logistics pipeline: AnalyzeDemandWorker clusters orders by geography, OptimizeRoutesWorker minimizes total mileage, ScheduleWorker assigns vehicles by capacity and hours, and DispatchWorker sends drivers their stop lists.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDemandWorker** | `lo_analyze_demand` | Analyzes order demand to identify geographic clusters and volume patterns by ZIP code. |
| **DispatchWorker** | `lo_dispatch` | Dispatches the fleet with each driver receiving their optimized stop list. |
| **OptimizeRoutesWorker** | `lo_optimize_routes` | Computes optimal delivery routes across all delivery points to minimize total mileage. |
| **ScheduleWorker** | `lo_schedule` | Schedules vehicles based on load capacity, driver hours-of-service, and time windows. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
lo_analyze_demand
    │
    ▼
lo_optimize_routes
    │
    ▼
lo_schedule
    │
    ▼
lo_dispatch
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
java -jar target/logistics-optimization-1.0.0.jar
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
java -jar target/logistics-optimization-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lo_logistics_optimization \
  --version 1 \
  --input '{"region": "test-value", "date": "2026-01-01T00:00:00Z", "orders": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lo_logistics_optimization -s COMPLETED -c 5
```

## How to Extend

Connect AnalyzeDemandWorker to your order management system, OptimizeRoutesWorker to your vehicle routing solver (OR-Tools, Vroom), and DispatchWorker to your fleet dispatch platform. The workflow definition stays exactly the same.

- **AnalyzeDemandWorker** (`lo_analyze_demand`): pull order data from your OMS/WMS, cluster by geography, and compute volume/weight per cluster for vehicle sizing
- **OptimizeRoutesWorker** (`lo_optimize_routes`): solve the vehicle routing problem using Google OR-Tools, OptaPlanner, or a commercial solver (Routific, OptimoRoute)
- **ScheduleWorker** (`lo_schedule`): assign vehicles from your fleet management system based on optimized route requirements, driver availability, and HOS compliance
- **DispatchWorker** (`lo_dispatch`): push route assignments to drivers via telematics (Samsara, Geotab) and update the TMS with dispatch confirmations

Replace any worker with a production route solver or fleet system while keeping the same return structure, and the pipeline adapts seamlessly.

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
logistics-optimization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/logisticsoptimization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LogisticsOptimizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeDemandWorker.java
│       ├── DispatchWorker.java
│       ├── OptimizeRoutesWorker.java
│       └── ScheduleWorker.java
└── src/test/java/logisticsoptimization/workers/
    ├── AnalyzeDemandWorkerTest.java        # 2 tests
    ├── DispatchWorkerTest.java        # 2 tests
    ├── OptimizeRoutesWorkerTest.java        # 2 tests
    └── ScheduleWorkerTest.java        # 2 tests
```
