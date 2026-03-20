# End-to-End Supply Chain Management in Java with Conductor :  Plan, Source, Make, Deliver, and Return

A Java Conductor workflow example for end-to-end supply chain management following the SCOR model .  creating a production plan based on product and quantity, sourcing raw materials from suppliers, manufacturing the product, delivering the finished batch to the destination, and configuring the return policy for the shipment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to orchestrate the complete supply chain from demand to delivery. A production plan must be created specifying what to build and how much. Raw materials must be sourced from approved suppliers with the right lead times. Manufacturing must execute the production plan using the sourced materials. The finished goods must be shipped to the customer or distribution center. Finally, the return policy and reverse logistics path must be configured for the delivery. Each step depends on the previous one .  you cannot manufacture without sourced materials, and you cannot ship without finished goods.

Without orchestration, each department runs its own silo: planning uses a spreadsheet, procurement sends emails to suppliers, manufacturing tracks jobs on a whiteboard, and logistics manages shipping in a separate TMS. When sourcing delays push back the manufacturing schedule, planning doesn't find out until the factory calls. When a delivery fails and needs a return, the return process has no context about the original production plan or sourcing decisions.

## The Solution

**You just write the SCOR workers. Planning, sourcing, manufacturing, delivery, and returns. Conductor handles cross-department sequencing, supplier retries, and end-to-end visibility from plan to delivery.**

Each stage of the SCOR supply chain model is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so the plan drives sourcing decisions, sourced materials enable manufacturing, manufacturing output triggers delivery, and delivery configuration includes return handling. If the sourcing worker fails to reach a supplier, Conductor retries without re-creating the production plan. Every plan, sourcing decision, manufacturing record, shipment, and return policy is captured end-to-end for supply chain visibility and analytics.

### What You Write: Workers

Five workers follow the SCOR model: PlanWorker creates production plans, SourceWorker procures raw materials, MakeWorker handles manufacturing, DeliverWorker ships finished goods, and ReturnWorker configures reverse logistics.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `scm_deliver` | Ships the batch to the destination. |
| **MakeWorker** | `scm_make` | Manufactures the product. |
| **PlanWorker** | `scm_plan` | Creates a production plan based on product and quantity. |
| **ReturnWorker** | `scm_return` | Configures the return policy for a delivery. |
| **SourceWorker** | `scm_source` | Sources materials from suppliers. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
scm_plan
    │
    ▼
scm_source
    │
    ▼
scm_make
    │
    ▼
scm_deliver
    │
    ▼
scm_return
```

## Example Output

```
=== Example 651: Supply Chain Management ===

Step 1: Registering task definitions...
  Registered: scm_plan, scm_source, scm_make, scm_deliver, scm_return

Step 2: Registering workflow 'scm_supply_chain'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [deliver] Shipping
  [make] Manufacturing
  [plan] Production plan for
  [return] Return policy configured for
  [source] Sourced

  Status: COMPLETED
  Output: {deliveryId=..., eta=..., batchId=..., unitsProduced=...}

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
java -jar target/supply-chain-mgmt-1.0.0.jar
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
java -jar target/supply-chain-mgmt-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow scm_supply_chain \
  --version 1 \
  --input '{"product": "sample-product", "Industrial Sensor": "sample-Industrial Sensor", "quantity": "sample-quantity", "destination": "sample-destination"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w scm_supply_chain -s COMPLETED -c 5
```

## How to Extend

Connect PlanWorker to your demand planning system, SourceWorker to your supplier management platform, and DeliverWorker to your TMS for shipment execution. The workflow definition stays exactly the same.

- **PlanWorker** (`scm_plan`): create production plans in your APS (Advanced Planning System) or ERP (SAP PP, Oracle Manufacturing), specifying BOM, quantities, and target dates
- **SourceWorker** (`scm_source`): issue purchase orders to approved suppliers via your procurement system, tracking lead times and confirming material availability
- **MakeWorker** (`scm_make`): create manufacturing work orders in your MES, track production progress, and record output quantities and quality metrics
- **DeliverWorker** (`scm_deliver`): book carriers via your TMS, generate shipping documents (BOL, packing list), and track delivery to the destination
- **ReturnWorker** (`scm_return`): configure the return policy (RMA process, return window, refurbishment eligibility) for the delivered batch in your order management system

Integrate each worker with your ERP, MES, or TMS while keeping the same return structure, and the end-to-end supply chain flow remains intact.

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
supply-chain-mgmt/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/supplychainmgmt/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SupplyChainMgmtExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeliverWorker.java
│       ├── MakeWorker.java
│       ├── PlanWorker.java
│       ├── ReturnWorker.java
│       └── SourceWorker.java
└── src/test/java/supplychainmgmt/workers/
    ├── DeliverWorkerTest.java        # 3 tests
    ├── MakeWorkerTest.java        # 3 tests
    ├── PlanWorkerTest.java        # 4 tests
    ├── ReturnWorkerTest.java        # 3 tests
    └── SourceWorkerTest.java        # 4 tests
```
