# Inventory Optimization in Java with Conductor :  Stock Analysis, Reorder Point Calculation, Multi-SKU Optimization, and Replenishment Execution

A Java Conductor workflow example for inventory optimization. analyzing current stock levels across multiple SKUs in a warehouse (e.g., WIDGET-A through CABLE-E in WH-Central), calculating reorder points based on demand velocity and lead times, optimizing order quantities to minimize carrying costs while preventing stockouts, and executing replenishment orders. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to keep the right amount of inventory across multiple SKUs. Too much stock ties up working capital and warehouse space; too little causes stockouts and lost sales. For each SKU in WH-Central (widgets, gadgets, sensors, cables), you must analyze current on-hand quantities against consumption rates, calculate the reorder point where a new order must be placed to arrive before stock runs out, optimize order quantities across all SKUs to minimize total cost (ordering costs + holding costs + stockout penalties), and trigger purchase orders for items below their reorder threshold.

Without orchestration, inventory planners run the analysis in a spreadsheet, manually check each SKU against reorder thresholds, and create POs one at a time. The optimization step (which considers all SKUs together for volume discounts and warehouse capacity) is skipped because it requires data from the analysis step that isn't easily passed between tools. If the replenishment order fails to submit, the SKU sits below reorder point until someone notices days later.

## The Solution

**You just write the inventory workers. Stock analysis, reorder calculation, multi-SKU optimization, and replenishment execution. Conductor handles pipeline sequencing, ERP retry logic, and recorded optimization decisions for supply chain analytics.**

Each stage of the optimization pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so stock analysis feeds reorder calculation, reorder points feed the multi-SKU optimizer, and optimization results drive replenishment execution. If the ERP integration fails when placing a purchase order, Conductor retries without re-running the analysis. Every stock snapshot, reorder calculation, optimization decision, and order execution is recorded for supply chain analytics and audit.

### What You Write: Workers

Four workers optimize inventory across SKUs: AnalyzeStockWorker reads current levels, CalculateReorderWorker sets reorder points, OptimizeWorker minimizes total cost across all items, and ExecuteWorker triggers replenishment purchase orders.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeStockWorker** | `io_analyze_stock` | Analyzes current on-hand quantities and consumption rates across all SKUs. |
| **CalculateReorderWorker** | `io_calculate_reorder` | Calculates the reorder point for each SKU based on demand velocity and lead time. |
| **ExecuteWorker** | `io_execute` | Triggers replenishment purchase orders for items below their reorder threshold. |
| **OptimizeWorker** | `io_optimize` | Optimizes order quantities across all SKUs to minimize total cost (ordering + holding + stockout). |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
io_analyze_stock
    │
    ▼
io_calculate_reorder
    │
    ▼
io_optimize
    │
    ▼
io_execute

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
java -jar target/inventory-optimization-1.0.0.jar

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
java -jar target/inventory-optimization-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow io_inventory_optimization \
  --version 1 \
  --input '{"warehouse": "sample-warehouse", "skuList": "sample-skuList"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w io_inventory_optimization -s COMPLETED -c 5

```

## How to Extend

Connect AnalyzeStockWorker to your WMS inventory feeds, OptimizeWorker to your operations research solver, and ExecuteWorker to your ERP purchase order module. The workflow definition stays exactly the same.

- **AnalyzeStockWorker** (`io_analyze_stock`): query your WMS or ERP (SAP MM, Oracle Inventory) for current on-hand quantities, days-of-supply, and consumption velocity per SKU
- **CalculateReorderWorker** (`io_calculate_reorder`): compute reorder points using safety stock formulas (ROP = demand during lead time + safety stock), factoring in supplier lead times and demand variability
- **OptimizeWorker** (`io_optimize`): run multi-SKU optimization using EOQ models, volume discount tiers, and warehouse capacity constraints to determine optimal order quantities
- **ExecuteWorker** (`io_execute`): create purchase requisitions or purchase orders in your ERP for SKUs below their reorder point, routing to the appropriate buyer for approval

Connect any worker to your ERP or demand planning system while preserving its output schema, and the optimization pipeline stays intact.

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
inventory-optimization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/inventoryoptimization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InventoryOptimizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeStockWorker.java
│       ├── CalculateReorderWorker.java
│       ├── ExecuteWorker.java
│       └── OptimizeWorker.java
└── src/test/java/inventoryoptimization/workers/
    ├── AnalyzeStockWorkerTest.java        # 2 tests
    ├── CalculateReorderWorkerTest.java        # 2 tests
    ├── ExecuteWorkerTest.java        # 2 tests
    └── OptimizeWorkerTest.java        # 2 tests

```
