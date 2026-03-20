# Goods Receipt in Java with Conductor :  Shipment Receiving, Quality Inspection, PO Matching, Warehouse Storage, and Inventory Update

A Java Conductor workflow example for inbound goods receipt processing .  receiving a shipment at the dock (e.g., 5,000 M10 bolts and 5,000 M10 nuts against PO-654-001), inspecting items for quality and damage, matching received quantities to the purchase order, assigning storage locations in the warehouse, and updating inventory records. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to process inbound shipments at your warehouse receiving dock. When shipment SHP-2024-655 arrives, the dock team must log receipt against purchase order PO-654-001, inspect the goods for damage and spec compliance, verify that received quantities match the PO line items (did we get 5,000 bolts or only 4,800?), assign bin locations for putaway, and update the inventory management system so the stock is available for picking. If the PO match step reveals a shortage, procurement needs to be notified to arrange a replacement shipment.

Without orchestration, the dock team fills out paper receiving forms, a clerk manually enters quantities into the ERP, and inventory updates happen hours later in a batch job. Discrepancies between received and ordered quantities go unnoticed until someone tries to pick the missing items. If the ERP update fails, the warehouse shows phantom stock and downstream orders get allocated against inventory that doesn't exist.

## The Solution

**You just write the receiving workers. Dock receipt, quality inspection, PO matching, bin assignment, and inventory update. Conductor handles sequential gating, automatic retries on ERP failures, and three-way match records for audit compliance.**

Each step of the goods receipt process is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so receiving logs are created before inspection, inspection results gate PO matching, matched goods are stored before inventory is updated, and inventory records only reflect items that passed inspection. If the inventory update worker fails, Conductor retries it without re-inspecting the entire shipment. Every receiving record, inspection result, PO match, storage assignment, and inventory adjustment is tracked for three-way matching and audit compliance.

### What You Write: Workers

Five workers handle the receiving dock workflow: ReceiveWorker logs inbound shipments, InspectWorker checks quality, MatchPoWorker verifies quantities against the purchase order, StoreWorker assigns bin locations, and UpdateInventoryWorker makes stock available for picking.

| Worker | Task | What It Does |
|---|---|---|
| **InspectWorker** | `grc_inspect` | Inspects received items for damage and spec compliance. |
| **MatchPoWorker** | `grc_match_po` | Matches received quantities against the purchase order line items. |
| **ReceiveWorker** | `grc_receive` | Logs shipment receipt at the dock with item details. |
| **StoreWorker** | `grc_store` | Assigns storage bin locations and records putaway for inspected and matched goods. |
| **UpdateInventoryWorker** | `grc_update_inventory` | Updates the inventory management system so received stock is available for picking. |

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
grc_receive
    │
    ▼
grc_inspect
    │
    ▼
grc_match_po
    │
    ▼
grc_store
    │
    ▼
grc_update_inventory
```

## Example Output

```
=== Example 655: Goods Receipt ===

Step 1: Registering task definitions...
  Registered: grc_receive, grc_inspect, grc_match_po, grc_store, grc_update_inventory

Step 2: Registering workflow 'grc_goods_receipt'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [inspect] Inspected
  [match] Items matched to
  [receive] Shipment
  [store] Items stored in Warehouse A, Aisle 12
  [update] Inventory updated for

  Status: COMPLETED
  Output: {inspectedItems=..., passed=..., defectRate=..., matched=...}

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
java -jar target/goods-receipt-1.0.0.jar
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
java -jar target/goods-receipt-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow grc_goods_receipt \
  --version 1 \
  --input '{"shipmentId": "SHP-2024-655", "SHP-2024-655": "poNumber", "poNumber": "PO-654-001", "PO-654-001": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w grc_goods_receipt -s COMPLETED -c 5
```

## How to Extend

Connect ReceiveWorker to your WMS dock scanner, MatchPoWorker to your ERP purchase order module, and UpdateInventoryWorker to your inventory management system. The workflow definition stays exactly the same.

- **ReceiveWorker** (`grc_receive`): scan barcodes or ASN (Advance Ship Notice) data at the dock, log receipt in your WMS (Manhattan Associates, Blue Yonder, SAP EWM), and capture photos of delivery condition
- **InspectWorker** (`grc_inspect`): record quality inspection results (visual check, dimension measurement, sample testing) against acceptance criteria from the PO specifications
- **MatchPoWorker** (`grc_match_po`): query your ERP for the purchase order, perform three-way matching (PO, receipt, invoice), and flag quantity or specification discrepancies for procurement review
- **StoreWorker** (`grc_store`): assign optimal bin/rack locations based on WMS slotting rules (FIFO, product type, pick frequency), and generate putaway tasks for warehouse staff
- **UpdateInventoryWorker** (`grc_update_inventory`): post the goods receipt to your ERP inventory module (SAP MM, Oracle Inventory), making stock available for order allocation and MRP planning

Wire any worker to your WMS or ERP while keeping the same output contract, and the receiving workflow continues unchanged.

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
goods-receipt/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/goodsreceipt/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GoodsReceiptExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── InspectWorker.java
│       ├── MatchPoWorker.java
│       ├── ReceiveWorker.java
│       ├── StoreWorker.java
│       └── UpdateInventoryWorker.java
└── src/test/java/goodsreceipt/workers/
    ├── InspectWorkerTest.java        # 2 tests
    ├── MatchPoWorkerTest.java        # 2 tests
    ├── ReceiveWorkerTest.java        # 3 tests
    ├── StoreWorkerTest.java        # 2 tests
    └── UpdateInventoryWorkerTest.java        # 2 tests
```
