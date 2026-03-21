# Inventory Management in Java Using Conductor: Check Stock, Reserve, Update, Reorder

It's Black Friday. Your product page shows 500 units of the hot new headphones "in stock"; but that number is a lie. The website sees 500, the Amazon channel sees 500, and the outlet app sees 500, because all three read from a cache that hasn't synced with the warehouse. Physically, 200 units sit on the shelf. By noon, you've sold 800 headphones you don't have, customer service is fielding cancellation calls, and your supplier can't restock for three weeks. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate atomic stock checks, reservations, updates, and reorders as independent workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Inventory Must Be Accurate, Atomic, and Proactive

A customer orders 5 units of SKU-12345. The warehouse has 12 in stock with a reorder threshold of 10. The system must check that 5 are available (not already reserved by other orders), reserve exactly 5 (atomically.; no double-reserving), update the available quantity to 7, and trigger a reorder because 7 is below the threshold of 10.

Inventory operations must be atomic: if two orders for 8 units arrive simultaneously against 12 in stock, only one should succeed. . Not both. If the update step fails after reservation, the reserved quantity must be rolled back. And reorder decisions must be based on the updated quantity, not the pre-reservation quantity. Without orchestration, these race conditions and consistency requirements lead to overselling, stockouts, and phantom inventory.

## The Solution

**You just write the stock check, reservation, inventory update, and reorder logic. Conductor handles reservation retries, reorder sequencing, and stock level tracking across warehouses.**

`CheckStockWorker` queries the current available quantity for the SKU at the specified warehouse, accounting for existing reservations. `ReserveWorker` atomically reserves the requested quantity. Failing if insufficient stock is available. `UpdateWorker` decrements the available inventory by the reserved amount and records the transaction. `ReorderWorker` checks the updated quantity against the reorder threshold and triggers a purchase order if stock is low, calculating the reorder quantity based on demand forecasts. Conductor chains these four steps, retries failed updates without double-reserving, and records every inventory movement for audit.

### What You Write: Workers

Stock checking, reservation, inventory updates, and reorder workers manage warehouse state through discrete, independently testable operations.

| Worker | Task | What It Does |
|---|---|---|
| **CheckStockWorker** | `inv_check_stock` | Performs the check stock operation |
| **ReorderWorker** | `inv_reorder` | Performs the reorder operation |
| **ReserveStockWorker** | `inv_reserve` | Performs the reserve stock operation |
| **UpdateInventoryWorker** | `inv_update` | Performs the update inventory operation |

Workers simulate e-commerce operations: payment processing, inventory checks, shipping, with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
inv_check_stock
    │
    ▼
inv_reserve
    │
    ▼
inv_update
    │
    ▼
inv_reorder

```

## Example Output

```
=== Example 456: Inventory Management ===

Step 1: Registering task definitions...
  Registered: inv_check_stock, inv_reserve, inv_update, inv_reorder

Step 2: Registering workflow 'inventory_management'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 008cc633-7314-c926-0420-acfccb06e325

  [check] SKU WH-1000XM5 at WH-EAST-01: 45 units available
  [reserve] SKU WH-1000XM5: requested=requested-value, reserved=reserved-value
  [update] SKU WH-1000XM5 at WH-EAST-01: previous-value -> remainingQty-value units
  [reorder] SKU WH-1000XM5: remaining-value <= threshold-value threshold -> reorder reorderQty-value units
  [reorder] SKU WH-1000XM5: remaining-value > threshold-value threshold -> no reorder needed


  Status: COMPLETED
  Output: {availableQty=45, reserved=reserved-value, remainingQty=remainingQty-value, reorderPlaced=true}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/inventory-management-1.0.0.jar

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
java -jar target/inventory-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow inventory_management \
  --version 1 \
  --input '{"sku": "WH-1000XM5", "requestedQty": 20, "warehouseId": "WH-EAST-01", "reorderThreshold": 30}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w inventory_management -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real inventory systems. Your WMS for stock queries, Redis for atomic reservations, your ERP for reorder triggers, and the workflow runs identically in production.

- **CheckStockWorker** (`inv_check_stock`): query real inventory systems: Shopify Inventory API, NetSuite, SAP, or a PostgreSQL inventory table with row-level locking for concurrent access
- **ReserveWorker** (`inv_reserve`): use Redis `DECRBY` with atomic check for high-concurrency reservation, or PostgreSQL `SELECT FOR UPDATE` for transactional consistency
- **ReorderWorker** (`inv_reorder`): integrate with supplier APIs for automated purchase orders, or use demand forecasting (Prophet, ARIMA) to calculate optimal reorder quantities

Switch warehouse management systems and the inventory workflow continues with no definition changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
inventory-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/inventorymanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InventoryManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckStockWorker.java
│       ├── ReorderWorker.java
│       ├── ReserveStockWorker.java
│       └── UpdateInventoryWorker.java
└── src/test/java/inventorymanagement/workers/
    ├── CheckStockWorkerTest.java        # 3 tests
    ├── ReorderWorkerTest.java        # 3 tests
    ├── ReserveStockWorkerTest.java        # 4 tests
    └── UpdateInventoryWorkerTest.java        # 3 tests

```
