# Event Driven Saga in Java Using Conductor

A customer places an order. Your service creates the order record, charges their credit card, and then, the shipping service is down. The payment went through, the order shows "confirmed," but nothing ships. Three days later the customer calls asking where their package is. You check the logs: the shipping call threw a connection timeout, the catch block logged a warning, and nobody ever refunded the charge or cancelled the order. You now have a paid, confirmed order that will never ship, and no automated way to unwind it. This workflow implements the saga pattern: if any step fails, compensation runs automatically to cancel the order and refund the payment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to coordinate a distributed transaction across order creation, payment processing, and shipping. If payment succeeds, the order ships. If payment fails, you must run compensation logic to cancel the order and release any reserved resources. This is the saga pattern, a sequence of local transactions with compensating actions for failures. Without proper compensation, you end up with paid orders that never ship or shipped orders that were never paid for.

Without orchestration, you'd implement the saga with nested try/catch blocks, manually calling compensation methods on failure, hoping the compensation itself does not fail, and logging every branch to debug inconsistent state across services.

## The Solution

**You just write the order-creation, payment, shipping, and compensation workers. Conductor handles SWITCH-based compensation routing, guaranteed saga completion, and a full audit trail of every saga step and rollback.**

Each saga step is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing the happy path (order, payment, shipping), routing via a SWITCH task to compensation on payment failure, retrying transient failures before triggering compensation, and tracking the entire saga with full audit trail. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement the saga: CreateOrderWorker starts the order, ProcessPaymentWorker charges the customer, ShipOrderWorker dispatches the shipment, while CancelOrderWorker and CompensatePaymentWorker handle rollback when payment fails.

| Worker | Task | What It Does |
|---|---|---|
| **CancelOrderWorker** | `ds_cancel_order` | Cancels an order as part of saga compensation. |
| **CompensatePaymentWorker** | `ds_compensate_payment` | Compensates (refunds) a payment when the saga needs to roll back. |
| **CreateOrderWorker** | `ds_create_order` | Creates an order in the saga. |
| **ProcessPaymentWorker** | `ds_process_payment` | Processes payment for an order. |
| **ShipOrderWorker** | `ds_ship_order` | Ships an order after successful payment. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
ds_create_order
    │
    ▼
ds_process_payment
    │
    ▼
SWITCH (switch_ref)
    ├── success: ds_ship_order
    ├── failed: ds_compensate_payment -> ds_cancel_order

```

## Example Output

```
=== Event-Driven Saga Demo ===

Step 1: Registering task definitions...
  Registered: ds_create_order, ds_process_payment, ds_ship_order, ds_compensate_payment, ds_cancel_order

Step 2: Registering workflow 'event_driven_saga'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 922af98f-a213-9f82-1996-b67e8d99c54e

  [ds_create_order] Created order ORD-2001 for $149.99
  [ds_process_payment] Processing $149.99 for order UNKNOWN
  [ds_ship_order] Shipping order UNKNOWN to 123 Main St, Springfield, IL


  Status: COMPLETED
  Output: {orderId=UNKNOWN, paymentStatus=success, sagaOutcome=success}

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
java -jar target/event-driven-saga-1.0.0.jar

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
java -jar target/event-driven-saga-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_driven_saga \
  --version 1 \
  --input '{"orderId": "ORD-2001", "amount": 149.99, "shippingAddress": "123 Main St, Springfield, IL"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_driven_saga -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real order database, payment gateway (Stripe, Adyen), and shipping API, with real refund logic for compensation, the saga with payment-routing and rollback workflow stays exactly the same.

- **Order creation**: insert orders into your database and reserve inventory in your warehouse management system
- **Payment processing**: charge via Stripe/Adyen with idempotency keys to prevent double charges during retries
- **Shipping**: create shipments via carrier APIs and generate tracking numbers
- **Compensation**: implement rollback logic: refund payment, release inventory holds, cancel shipment, and notify the customer

Wiring ProcessPaymentWorker to a real payment gateway or CompensatePaymentWorker to a refund API preserves the saga routing logic.

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
event-driven-saga/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventdrivensaga/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventDrivenSagaExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CancelOrderWorker.java
│       ├── CompensatePaymentWorker.java
│       ├── CreateOrderWorker.java
│       ├── ProcessPaymentWorker.java
│       └── ShipOrderWorker.java
└── src/test/java/eventdrivensaga/workers/
    ├── CancelOrderWorkerTest.java        # 8 tests
    ├── CompensatePaymentWorkerTest.java        # 8 tests
    ├── CreateOrderWorkerTest.java        # 8 tests
    ├── ProcessPaymentWorkerTest.java        # 8 tests
    └── ShipOrderWorkerTest.java        # 8 tests

```
