# Flash Sale in Java Using Conductor :  Prepare Inventory, Open Sale, Process Orders, Close, Report

Flash sale: prepare inventory, open sale, process orders, close, report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Flash Sales Need Precise Timing and Inventory Control

A 2-hour flash sale on 500 units at 60% off generates a traffic spike. The system must prepare inventory (reserve 500 units from general stock), open the sale at exactly the scheduled time (not a second early), process orders atomically (decrement inventory, no overselling), close when time expires or inventory hits zero, and produce a report showing units sold, revenue, peak order rate, and customer distribution.

Overselling is the cardinal sin .  selling 520 units when only 500 exist creates fulfillment nightmares. Underselling (closing too early) leaves money on the table. The order processing step must be atomic: check inventory, decrement, and confirm in a single operation. If the ordering system crashes mid-sale, it must resume with the correct inventory count, not start over or lose orders.

## The Solution

**You just write the inventory preparation, sale activation, order processing, and reporting logic. Conductor handles concurrent order processing, inventory locking, and sale event audit trails.**

`PrepareInventoryWorker` reserves the sale quantity from general inventory, sets quantity caps, and configures per-customer purchase limits. `OpenSaleWorker` activates the sale at the scheduled time, making the discounted items available for purchase. `ProcessOrdersWorker` handles incoming orders with atomic inventory decrement .  checking availability, reserving quantity, and confirming each order without overselling. `CloseSaleWorker` ends the sale when time expires or inventory is exhausted, returning unsold units to general inventory. `ReportWorker` generates the sales report ,  units sold, revenue, average order value, peak order rate, and customer demographics. Conductor sequences these stages and tracks the entire sale lifecycle.

### What You Write: Workers

Inventory preparation, sale activation, order processing, and reporting workers handle the lifecycle of a time-limited sale event independently.

| Worker | Task | What It Does |
|---|---|---|
| **CloseSaleWorker** | `fls_close_sale` | Flash sale closed - |
| **OpenSaleWorker** | `fls_open_sale` | Performs the open sale operation |
| **PrepareInventoryWorker** | `fls_prepare_inventory` | Reserving inventory for \ |
| **ProcessOrdersWorker** | `fls_process_orders` | Performs the process orders operation |
| **ReportWorker** | `fls_report` | Performs the report operation |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
fls_prepare_inventory
    │
    ▼
fls_open_sale
    │
    ▼
fls_process_orders
    │
    ▼
fls_close_sale
    │
    ▼
fls_report

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
java -jar target/flash-sale-1.0.0.jar

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
java -jar target/flash-sale-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow flash_sale_workflow \
  --version 1 \
  --input '{"saleId": "TEST-001", "saleName": "test", "durationMinutes": "sample-durationMinutes"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w flash_sale_workflow -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real commerce systems. Redis for atomic inventory decrement, your storefront API for sale activation, your analytics pipeline for reporting, and the workflow runs identically in production.

- **ProcessOrdersWorker** (`fls_process_orders`): use Redis `DECR` with atomic check for lock-free inventory management, or PostgreSQL `SELECT FOR UPDATE SKIP LOCKED` for high-concurrency order processing
- **OpenSaleWorker** (`fls_open_sale`): integrate with CDN cache invalidation (CloudFront, Fastly) to instantly update product pages, and push real-time updates via WebSockets for countdown timers
- **ReportWorker** (`fls_report`): push sales data to analytics dashboards (Looker, Metabase), calculate conversion funnel metrics, and compare against previous flash sale performance

Replace the inventory system or order processor and the flash sale flow keeps its structure.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
flash-sale/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/flashsale/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FlashSaleExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseSaleWorker.java
│       ├── OpenSaleWorker.java
│       ├── PrepareInventoryWorker.java
│       ├── ProcessOrdersWorker.java
│       └── ReportWorker.java
└── src/test/java/flashsale/workers/
    ├── ProcessOrdersWorkerTest.java        # 2 tests
    └── ReportWorkerTest.java        # 2 tests

```
