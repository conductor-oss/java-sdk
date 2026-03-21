# Warehouse Management in Java with Conductor :  Receiving, Putaway, Picking, Packing, and Shipping

A Java Conductor workflow example for warehouse management. receiving inbound goods at the dock, putting away items to assigned storage locations, picking items from bins to fulfill outbound orders, packing them for shipment, and shipping via the selected carrier. Uses [Conductor](https://github.

## The Problem

You need to manage the flow of goods through your warehouse from dock to door. Inbound shipments must be received, inspected, and logged. Received items must be put away to optimal storage locations based on product type, pick frequency, and available capacity. When outbound orders come in, items must be picked from the correct bins in the right sequence to minimize picker travel distance. Picked items must be packed into the right box size with appropriate dunnage. Finally, packed orders must be shipped via the customer's selected method with correct labels and documentation.

Without orchestration, each step is managed by a different warehouse associate with clipboards or handheld scanners that don't talk to each other. If putaway assigns a bin that's already full, the picker doesn't find the item and the order ships late. If packing completes but the shipping label printer is offline, packed boxes sit on the dock without tracking numbers. There is no end-to-end visibility into where an order is in the fulfillment process.

## The Solution

**You just write the warehouse workers. Receiving, putaway, picking, packing, and shipping. Conductor handles step sequencing, shipping label retries, and full order visibility for warehouse performance analytics.**

Each step of the warehouse flow is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so goods are received before putaway, putaway completes before items are available for picking, picking drives packing, and packing drives shipping. If the shipping label API times out, Conductor retries without re-packing the order. Every receiving record, putaway location, pick confirmation, pack verification, and ship confirmation is tracked for order visibility and warehouse performance analytics.

### What You Write: Workers

Five workers manage dock-to-door operations: ReceiveWorker logs inbound goods, PutAwayWorker assigns optimal bins, PickWorker retrieves items for orders, PackWorker prepares shipments, and ShipWorker generates tracking labels.

| Worker | Task | What It Does |
|---|---|---|
| **PackWorker** | `wm_pack` | Packs picked items into the appropriate box size with dunnage and labels. |
| **PickWorker** | `wm_pick` | Picks items from storage bins in optimized sequence to fulfill outbound orders. |
| **PutAwayWorker** | `wm_put_away` | Assigns optimal storage locations and puts away received items based on product type and pick frequency. |
| **ReceiveWorker** | `wm_receive` | Receives inbound goods at the dock, inspects, and logs the receipt. |
| **ShipWorker** | `wm_ship` | Ships packed orders via the selected carrier with tracking numbers and documentation. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
wm_receive
    │
    ▼
wm_put_away
    │
    ▼
wm_pick
    │
    ▼
wm_pack
    │
    ▼
wm_ship

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
java -jar target/warehouse-management-1.0.0.jar

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
java -jar target/warehouse-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wm_warehouse_management \
  --version 1 \
  --input '{"orderId": "TEST-001", "items": [{"id": "ITEM-001", "quantity": 2}], "shippingMethod": "sample-shippingMethod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wm_warehouse_management -s COMPLETED -c 5

```

## How to Extend

Connect ReceiveWorker to your dock scanner system, PickWorker to your WMS pick-path optimizer, and ShipWorker to your carrier label API (FedEx, UPS, USPS). The workflow definition stays exactly the same.

- **ReceiveWorker** (`wm_receive`): scan inbound shipments at the dock via your WMS (Manhattan Associates, Blue Yonder, SAP EWM), verify against ASN, and log receipt
- **PutAwayWorker** (`wm_put_away`): assign optimal bin/rack locations using WMS slotting algorithms, generate putaway tasks for forklift operators, and confirm placement via barcode scan
- **PickWorker** (`wm_pick`): generate pick lists optimized for travel distance (wave picking, batch picking, zone picking), direct pickers via RF scanners or pick-to-light systems
- **PackWorker** (`wm_pack`): select appropriate carton size (cartonization), verify contents against the pick list, add dunnage, and weigh the package for shipping rate calculation
- **ShipWorker** (`wm_ship`): rate-shop across carriers (UPS, FedEx, USPS), generate shipping labels, transmit ASN to the customer, and hand off to the carrier for pickup

Replace any worker with your WMS integration while keeping the same output fields, and the warehouse flow operates seamlessly.

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
warehouse-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/warehousemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WarehouseManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PackWorker.java
│       ├── PickWorker.java
│       ├── PutAwayWorker.java
│       ├── ReceiveWorker.java
│       └── ShipWorker.java
└── src/test/java/warehousemanagement/workers/
    ├── PackWorkerTest.java        # 2 tests
    ├── PickWorkerTest.java        # 2 tests
    ├── PutAwayWorkerTest.java        # 2 tests
    ├── ReceiveWorkerTest.java        # 2 tests
    └── ShipWorkerTest.java        # 2 tests

```
