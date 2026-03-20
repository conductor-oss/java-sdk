# Resource Allocation Automation in Java with Conductor :  Demand Assessment, Capacity Checking, Resource Assignment, and Allocation Confirmation

A Java Conductor workflow example that automates resource allocation for projects .  assessing demand by hours needed with priority and start date, checking available capacity by resource type to find team members with free hours, allocating the best-fit resource to the project, and confirming the allocation with a locked commitment. Uses [Conductor](https://github.## Why Resource Allocation Needs Orchestration

Allocating people to projects requires a pipeline where each step narrows the decision based on real data. You assess demand .  the project needs a specific number of hours (e.g., 30h), has a priority level (high), and a start date (2026-03-10). You check capacity for the requested resource type ,  querying which team members have free hours, producing a ranked list (Alice with 30 free hours, Bob with 20). You allocate by matching the demand to the best available resource ,  selecting Alice because her 30 free hours exactly cover the project's 30-hour need. Finally, you confirm the allocation ,  locking Alice's hours against the project so no other allocation can double-book her.

Each step depends on the previous one .  capacity checking needs the demand assessment to know how many hours to look for, allocation needs the available resource list to select from, and confirmation needs the specific allocation to lock. If the capacity check reveals that no single resource has enough free hours, the allocation step needs that information to split across multiple people ,  not silently assign to someone who is already overbooked. Without orchestration, you'd build a monolithic allocator that mixes demand calculations, calendar queries, assignment logic, and booking confirmations ,  making it impossible to swap your capacity data source (spreadsheet to resource management tool), add approval gates for high-cost allocations, or audit why a specific resource was allocated to a specific project over competing requests.

## How This Workflow Solves It

**You just write the demand assessment, capacity checking, resource assignment, and allocation confirmation logic. Conductor handles availability retries, conflict resolution, and allocation audit trails.**

Each allocation stage is an independent worker .  assess demand, check capacity, allocate, confirm. Conductor sequences them, passes the demand profile into capacity checking, feeds the available resource list into allocation, hands the allocation to confirmation for locking, retries if your resource management system is temporarily unavailable during the capacity check, and records every allocation decision for capacity planning analysis.

### What You Write: Workers

Demand forecasting, availability checking, assignment optimization, and conflict resolution workers each solve one piece of resource planning.

| Worker | Task | What It Does |
|---|---|---|
| **AllocateWorker** | `ral_allocate` | Allocates team members and resources to the project based on availability and skills |
| **AssessDemandWorker** | `ral_assess_demand` | Assess Demand. Computes and returns demand |
| **CheckCapacityWorker** | `ral_check_capacity` | Check Capacity. Computes and returns available |
| **ConfirmWorker** | `ral_confirm` | Allocation confirmed |

Workers simulate project management operations .  task creation, status updates, notifications ,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
ral_assess_demand
    │
    ▼
ral_check_capacity
    │
    ▼
ral_allocate
    │
    ▼
ral_confirm
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
java -jar target/resource-allocation-1.0.0.jar
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
java -jar target/resource-allocation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow resource_allocation_resource-allocation \
  --version 1 \
  --input '{"projectId": "PROJ-42", "PROJ-42": "resourceType", "resourceType": "developer", "developer": "hoursNeeded", "hoursNeeded": 40}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w resource_allocation_resource-allocation -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real resource systems .  your capacity planning tool for availability data, your HR platform for skill profiles, your booking system for allocation locking, and the workflow runs identically in production.

- **AssessDemandWorker** (`ral_assess_demand`): pull demand from your project intake system or PM tool, calculate required hours from story point estimates and historical velocity, and determine priority and start date constraints
- **CheckCapacityWorker** (`ral_check_capacity`): query your resource management tool (Resource Guru, Float, Productive) or HR system to find team members with the requested skill type and available hours in the target time window
- **AllocateWorker** (`ral_allocate`): select the best-fit resource using your allocation algorithm (closest capacity match, lowest cost, least context-switching), create the booking in your resource management system, and block the hours on their calendar
- **ConfirmWorker** (`ral_confirm`): lock the allocation so competing requests cannot double-book, notify the resource and project manager via Slack or email, and update the project's resource plan in your PM tool

Integrate new scheduling tools or demand models and the allocation pipeline handles them seamlessly.

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
resource-allocation-resource-allocation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/resourceallocation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ResourceAllocationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AllocateWorker.java
│       ├── AssessDemandWorker.java
│       ├── CheckCapacityWorker.java
│       └── ConfirmWorker.java
└── src/test/java/resourceallocation/workers/
    ├── AllocateWorkerTest.java        # 2 tests
    ├── AssessDemandWorkerTest.java        # 2 tests
    ├── CheckCapacityWorkerTest.java        # 2 tests
    └── ConfirmWorkerTest.java        # 2 tests
```
