# Reverse Logistics in Java with Conductor :  Return Receipt, Condition Inspection, Refurbish/Recycle/Dispose Routing, and Processing

A Java Conductor workflow example for reverse logistics .  receiving returned products (e.g., defective wireless headphones), inspecting their condition, routing to refurbishment, recycling, or disposal based on the inspection outcome, and processing the return for inventory adjustment and customer refund. Uses [Conductor](https://github.## The Problem

You need to handle product returns efficiently and recover maximum value. When a customer returns wireless headphones with a defective speaker (return ID RET-2024-669), the item must be received at the returns center, inspected to determine condition (cosmetic damage only? functional defect? beyond repair?), and routed to the appropriate disposition: refurbishment if repairable, recycling if materials can be recovered, or disposal if neither is viable. The final processing step updates inventory, triggers the customer refund, and records the disposition for sustainability reporting.

Without orchestration, returns pile up at the dock waiting for inspection. Inspectors make disposition decisions with no consistent criteria, and refurbishable items end up in the recycle bin because there is no routing logic. Processing refunds is disconnected from the disposition decision, so customers wait weeks for refunds on items that were scrapped on day one. There is no visibility into recovery rates or disposition mix.

## The Solution

**You just write the returns workers. Receipt logging, condition inspection, refurbish/recycle/dispose routing, and processing. Conductor handles SWITCH-based disposition routing, refurbishment retries, and disposition records for sustainability reporting.**

Each step of the reverse logistics process is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so returns are received before inspection, inspection results drive the SWITCH task that routes to refurbish, recycle, or dispose, and final processing runs regardless of which disposition path was taken. If the refurbishment worker fails, Conductor retries without re-inspecting the item. Every receipt, inspection result, disposition decision, and processing action is recorded for return analytics and sustainability reporting.

### What You Write: Workers

Six workers handle returns end-to-end: ReceiveReturnWorker logs the return, InspectWorker assesses condition, RefurbishWorker repairs salvageable items, RecycleWorker recovers materials, DisposeWorker handles waste, and ProcessWorker triggers refunds and inventory updates.

| Worker | Task | What It Does |
|---|---|---|
| **DisposeWorker** | `rvl_dispose` | Disposes of items that cannot be refurbished or recycled, with proper waste handling. |
| **InspectWorker** | `rvl_inspect` | Inspects the returned product's condition .  cosmetic damage, functional defect, or beyond repair. |
| **ProcessWorker** | `rvl_process` | Processes the return .  updates inventory, triggers the customer refund, and records the disposition. |
| **ReceiveReturnWorker** | `rvl_receive_return` | Receives the returned product at the returns center and logs the return ID. |
| **RecycleWorker** | `rvl_recycle` | Recovers materials from items that cannot be refurbished but have recyclable components. |
| **RefurbishWorker** | `rvl_refurbish` | Refurbishes repairable items for resale or warranty replacement. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
rvl_receive_return
    │
    ▼
rvl_inspect
    │
    ▼
SWITCH (rvl_switch_ref)
    ├── refurbish: rvl_refurbish
    ├── recycle: rvl_recycle
    └── default: rvl_dispose
    │
    ▼
rvl_process
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
java -jar target/reverse-logistics-1.0.0.jar
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
java -jar target/reverse-logistics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rvl_reverse_logistics \
  --version 1 \
  --input '{"returnId": "TEST-001", "product": "test-value", "reason": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rvl_reverse_logistics -s COMPLETED -c 5
```

## How to Extend

Connect InspectWorker to your inspection grading system, RefurbishWorker to your repair center workflow, and ProcessWorker to your refund and inventory adjustment system. The workflow definition stays exactly the same.

- **ReceiveReturnWorker** (`rvl_receive_return`): scan the return label/RMA number, log receipt in your returns management system, and associate with the original order
- **InspectWorker** (`rvl_inspect`): record condition assessment (functional test results, cosmetic grading) and output a disposition recommendation (refurbish/recycle/dispose)
- **RefurbishWorker** (`rvl_refurbish`): create a refurbishment work order in your MES, track repair steps, and return the item to sellable inventory as refurbished/open-box
- **RecycleWorker** (`rvl_recycle`): route the item to your e-waste recycling partner (e.g., via their intake API), recording material recovery estimates for ESG reporting
- **DisposeWorker** (`rvl_dispose`): schedule disposal with a certified waste handler, recording hazardous material classifications if applicable
- **ProcessWorker** (`rvl_process`): trigger the customer refund via your payment gateway, update inventory counts, and record the disposition outcome for return analytics

Point any worker at your returns management system while maintaining the same output contract, and the disposition routing continues unmodified.

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
reverse-logistics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reverselogistics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReverseLogisticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DisposeWorker.java
│       ├── InspectWorker.java
│       ├── ProcessWorker.java
│       ├── ReceiveReturnWorker.java
│       ├── RecycleWorker.java
│       └── RefurbishWorker.java
└── src/test/java/reverselogistics/workers/
    ├── DisposeWorkerTest.java        # 2 tests
    ├── InspectWorkerTest.java        # 2 tests
    ├── ProcessWorkerTest.java        # 2 tests
    ├── ReceiveReturnWorkerTest.java        # 2 tests
    ├── RecycleWorkerTest.java        # 2 tests
    └── RefurbishWorkerTest.java        # 2 tests
```
