# Capacity Mgmt Telecom in Java Using Conductor

A Java Conductor workflow example that orchestrates telecom network capacity management. monitoring current utilization and growth rate for a region's network infrastructure, forecasting when capacity will be exhausted, planning the capacity expansion, provisioning new network resources, and verifying the expanded capacity is live. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Capacity Management Needs Orchestration

Managing network capacity requires a proactive pipeline from measurement through expansion. You monitor current utilization and subscriber growth rates for a region's network type (RAN, transport, core). You forecast when existing capacity will be exhausted based on current utilization and growth trends. You plan the expansion. determining what equipment, spectrum, or backhaul capacity to add and where. You provision the planned resources by deploying and configuring new network elements. Finally, you verify the provisioned capacity is live and the region's utilization has dropped to acceptable levels.

If provisioning fails partway through, you need to know exactly which resources were already deployed so you can commission them or roll back cleanly. If forecasting underestimates growth, the plan is insufficient and subscribers experience congestion before the next planning cycle. Without orchestration, you'd build a capacity planning spreadsheet process that mixes performance data collection, trend analysis, vendor PO workflows, and deployment scripts. making it impossible to run what-if forecasts, track which growth assumptions drove which expansions, or automate the provisioning step.

## The Solution

**You just write the utilization monitoring, capacity forecasting, expansion planning, resource provisioning, and verification logic. Conductor handles forecast retries, upgrade planning sequencing, and capacity audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Demand forecasting, capacity analysis, upgrade planning, and implementation tracking workers each address one dimension of network capacity management.

| Worker | Task | What It Does |
|---|---|---|
| **ForecastWorker** | `cmt_forecast` | Forecasts capacity exhaustion date based on current utilization and subscriber growth rate. |
| **MonitorWorker** | `cmt_monitor` | Monitors current network utilization and growth rate for a region and network type. |
| **PlanWorker** | `cmt_plan` | Plans the capacity expansion. equipment, spectrum, or backhaul to add in the region. |
| **ProvisionWorker** | `cmt_provision` | Provisions new network resources by deploying and configuring the planned equipment. |
| **VerifyWorker** | `cmt_verify` | Verifies the provisioned capacity is live and the region's utilization is within target. |

Workers implement telecom operations. provisioning, activation, billing,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
cmt_monitor
    │
    ▼
cmt_forecast
    │
    ▼
cmt_plan
    │
    ▼
cmt_provision
    │
    ▼
cmt_verify

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
java -jar target/capacity-mgmt-telecom-1.0.0.jar

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
java -jar target/capacity-mgmt-telecom-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cmt_capacity_mgmt_telecom \
  --version 1 \
  --input '{"region": "us-east-1", "networkType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cmt_capacity_mgmt_telecom -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real capacity tools. your performance management system for utilization data, your planning platform for growth forecasting, your OSS for resource provisioning, and the workflow runs identically in production.

- **MonitorWorker** (`cmt_monitor`): pull utilization KPIs from your network performance management system (Nokia NetAct, Ericsson ENM, Huawei U2000) or query counters via the NMS northbound API
- **ForecastWorker** (`cmt_forecast`): run capacity forecasting models using your planning tools (Atoll, Planet, ASSET) or feed historical data into a time-series forecasting service
- **PlanWorker** (`cmt_plan`): generate the capacity expansion plan in your network planning system, determining site counts, equipment BOMs, and spectrum assignments for the region
- **ProvisionWorker** (`cmt_provision`): deploy the planned resources by pushing configurations to network elements via NETCONF/YANG or triggering deployment workflows in your OSS activation platform
- **VerifyWorker** (`cmt_verify`): re-poll KPIs from the NMS after provisioning and confirm utilization is within target thresholds, updating the capacity inventory database

Update demand models or upgrade criteria and the capacity pipeline adjusts without modification.

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
capacity-mgmt-telecom-capacity-mgmt-telecom/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/capacitymgmttelecom/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CapacityMgmtTelecomExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ForecastWorker.java
│       ├── MonitorWorker.java
│       ├── PlanWorker.java
│       ├── ProvisionWorker.java
│       └── VerifyWorker.java
└── src/test/java/capacitymgmttelecom/workers/
    ├── MonitorWorkerTest.java        # 1 tests
    └── ProvisionWorkerTest.java        # 1 tests

```
