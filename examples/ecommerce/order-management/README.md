# Order Management in Java Using Conductor: Create, Validate, Fulfill, Ship, Deliver

A customer orders a laptop and a USB-C hub. The warehouse picks the laptop but grabs the wrong hub: someone else's return, repackaged with the wrong SKU label. The customer opens the box, finds a used cable they didn't order, and initiates a return. But the returns system doesn't cross-reference the original pick list, so it marks the laptop as returned too and issues a full refund for items the customer still has. One mis-pick cascades into a $2,000 inventory discrepancy that takes three departments a week to untangle. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate each order stage as independent workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Orders Have Five Stages, Each With Different Failure Modes

An order for 3 items with next-day shipping must flow through creation (assign order number, lock prices), validation (verify inventory, check payment authorization), fulfillment (pick items, pack shipment, generate packing slip), shipping (create label, schedule pickup, trigger tracking), and delivery confirmation (update status, send delivery notification). Each stage involves different systems, the order database, inventory service, warehouse management, carrier API, and notification service.

If the carrier API is down during the shipping step, the order is already packed and waiting: retrying should resume from shipping, not restart fulfillment. If delivery confirmation fails, the shipment is still in transit, the status update should be retried. Every order needs a complete audit trail showing exactly when it moved through each stage.

## The Solution

**You just write the order creation, validation, fulfillment, shipping, and delivery confirmation logic. Conductor handles fulfillment retries, status transition tracking, and order lifecycle audit trails.**

`CreateWorker` initializes the order with a unique ID, line items, pricing, and customer details. `ValidateWorker` verifies item availability, confirms pricing hasn't changed, and validates the shipping address. `FulfillWorker` triggers warehouse operations. Pick list generation, item picking, packing, and quality check. `ShipWorker` creates shipping labels, schedules carrier pickup, and generates tracking numbers. `DeliverWorker` monitors delivery status and sends confirmation notifications upon delivery. Conductor sequences these five stages, retries failed carrier calls without re-packing, and records timestamps for every stage transition.

### What You Write: Workers

Five workers track an order from creation through validation, fulfillment, shipping, and delivery, with each step owning its own state transitions.

| Worker | Task | What It Does |
|---|---|---|
| **CreateOrderWorker** | `ord_create` | Performs the create order operation |
| **DeliverOrderWorker** | `ord_deliver` | Performs the deliver order operation |
| **FulfillOrderWorker** | `ord_fulfill` | Performs the fulfill order operation |
| **ShipOrderWorker** | `ord_ship` | Ships the order |
| **ValidateOrderWorker** | `ord_validate` | Performs the validate order operation |

Workers implement e-commerce operations: payment processing, inventory checks, shipping, with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
ord_create
    │
    ▼
ord_validate
    │
    ▼
ord_fulfill
    │
    ▼
ord_ship
    │
    ▼
ord_deliver

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
java -jar target/order-management-1.0.0.jar

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
java -jar target/order-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow order_management \
  --version 1 \
  --input '{"customerId": "cust-301", "items": [{"sku": "LAPTOP-PRO", "name": "Laptop Pro 16", "price": 1999.0, "qty": 1}, {"sku": "USB-C-HUB", "name": "USB-C Hub", "price": 49.99, "qty": 1}], "shippingAddress": {"street": "742 Evergreen Terrace", "city": "Springfield", "state": "IL", "zip": "62701"}, "shippingMethod": "express"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w order_management -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real fulfillment stack. Your OMS for order creation, your WMS for warehouse picking, carrier APIs for shipping labels, and the workflow runs identically in production.

- **FulfillWorker** (`ord_fulfill`): integrate with warehouse management systems (ShipBob, ShipMonk) or send pick lists to a WMS via EDI/API for physical warehouse operations
- **ShipWorker** (`ord_ship`): use EasyPost, Shippo, or ShipStation APIs for multi-carrier rate shopping, label generation, and tracking number assignment
- **DeliverWorker** (`ord_deliver`): poll carrier tracking APIs (FedEx, UPS, USPS) for delivery status, send push notifications via Firebase Cloud Messaging, and trigger post-delivery review requests

Connect each worker to your fulfillment systems and the order lifecycle operates without pipeline changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
order-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ordermanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OrderManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateOrderWorker.java
│       ├── DeliverOrderWorker.java
│       ├── FulfillOrderWorker.java
│       ├── ShipOrderWorker.java
│       └── ValidateOrderWorker.java
└── src/test/java/ordermanagement/workers/
    ├── CreateOrderWorkerTest.java        # 5 tests
    ├── DeliverOrderWorkerTest.java        # 2 tests
    ├── FulfillOrderWorkerTest.java        # 3 tests
    ├── ShipOrderWorkerTest.java        # 4 tests
    └── ValidateOrderWorkerTest.java        # 2 tests

```
