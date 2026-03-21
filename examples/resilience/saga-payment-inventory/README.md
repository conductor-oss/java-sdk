# Implementing E-Commerce Order Saga in Java with Conductor :  Inventory, Payment, and Shipping with Compensation

A Java Conductor workflow example demonstrating the saga pattern for e-commerce .  reserving inventory, charging payment, and shipping the order in sequence, with compensating transactions (release inventory, refund payment) that execute when shipping fails.

## The Problem

You process an e-commerce order: reserve the inventory so it's not sold to someone else, charge the customer's payment method, and ship the order. If shipping fails (item damaged, carrier unavailable), the payment must be refunded and the inventory must be released back to stock. Each step depends on the previous one .  you can't charge without reserved inventory, and you can't ship without payment.

Without orchestration, order processing is a monolithic transaction attempt. A shipping failure after successful payment leaves the customer charged with no shipment. A payment failure after inventory reservation leaves stock locked. Compensating each combination of partial success requires tangled error-handling code.

## The Solution

**You just write the order processing and compensation logic. Conductor handles the reserve-charge-ship sequence, SWITCH-based failure detection, reverse-order compensation (refund then release), retries on each step, and a complete audit trail of every order showing which steps completed and which compensations ran.**

Each forward step (reserve inventory, charge payment, ship order) and its compensation (release inventory, refund payment) are independent workers. Conductor runs the forward steps in sequence. When shipping fails, the failure workflow triggers compensation .  refund payment, then release inventory ,  in the correct reverse order. Every step is tracked, so you can see exactly where the order failed and which compensations ran. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ReserveInventoryWorker locks stock, ChargePaymentWorker processes the charge, and ShipOrderWorker dispatches the package, with ReleaseInventoryWorker and RefundPaymentWorker as compensating transactions that execute in reverse order when shipping fails.

| Worker | Task | What It Does |
|---|---|---|
| **ChargePaymentWorker** | `spi_charge_payment` | Worker for spi_charge_payment. Charges payment for an order. Produces a deterministic payment ID "PAY-001" and mark.. |
| **RefundPaymentWorker** | `spi_refund_payment` | Worker for spi_refund_payment. Compensation worker that refunds a payment. This is called as part of the saga compe.. |
| **ReleaseInventoryWorker** | `spi_release_inventory` | Worker for spi_release_inventory. Compensation worker that releases reserved inventory. This is called as part of t.. |
| **ReserveInventoryWorker** | `spi_reserve_inventory` | Worker for spi_reserve_inventory. Reserves inventory for an order. Produces a deterministic reservation ID "INV-001.. |
| **ShipOrderWorker** | `spi_ship_order` | Worker for spi_ship_order. Ships an order. If the input parameter "shouldFail" is true, the worker returns shipStat.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
spi_reserve_inventory
    │
    ▼
spi_charge_payment
    │
    ▼
spi_ship_order
    │
    ▼
SWITCH (check_ship_ref)
    ├── failed: spi_refund_payment -> spi_release_inventory

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
java -jar target/saga-payment-inventory-1.0.0.jar

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
java -jar target/saga-payment-inventory-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow saga_payment_inventory \
  --version 1 \
  --input '{"orderId": "TEST-001", "amount": 100, "shouldFail": "sample-shouldFail"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w saga_payment_inventory -s COMPLETED -c 5

```

## How to Extend

Each worker handles one order step .  connect the inventory worker to your warehouse system, the payment worker to Stripe or Braintree, the shipping worker to your carrier API, and the reserve-charge-ship saga with compensation stays the same.

- **ChargePaymentWorker** (`spi_charge_payment`): charge via Stripe/PayPal/Braintree, returning a charge ID for potential refund
- **RefundPaymentWorker** (`spi_refund_payment`): issue a refund via Stripe/PayPal/Braintree using the charge ID from the payment step
- **ReleaseInventoryWorker** (`spi_release_inventory`): release reserved inventory back to available stock in your warehouse management or ERP system using the reservation ID

Wire the inventory worker to your warehouse system and the payment worker to Stripe, and the order saga with compensation operates in production without any workflow changes.

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
saga-payment-inventory/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sagapaymentinventory/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SagaPaymentInventoryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChargePaymentWorker.java
│       ├── RefundPaymentWorker.java
│       ├── ReleaseInventoryWorker.java
│       ├── ReserveInventoryWorker.java
│       └── ShipOrderWorker.java
└── src/test/java/sagapaymentinventory/workers/
    └── WorkersTest.java        # 20 tests

```
