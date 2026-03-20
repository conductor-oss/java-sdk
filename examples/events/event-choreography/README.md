# Event Choreography in Java Using Conductor

Choreography pattern: services communicate through events with no central orchestrator. Each service emits an event that triggers the next service. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to coordinate an order fulfillment flow where each service communicates through events. The order service creates an order and emits an event, the payment service processes payment and emits a confirmation event, the inventory service reserves stock and emits a shipment event, and the notification service sends a confirmation to the customer. Each service reacts to the previous service's event rather than being called directly.

Without orchestration, you'd implement this as a pure event-driven choreography with each service subscribing to topics; but then you lose visibility into the end-to-end flow, cannot easily trace a single order across services, have no centralized retry mechanism when payment fails, and must build custom tooling to debug stuck orders.

## The Solution

**You just write the order, payment, inventory, notification, and event-emission workers. Conductor handles event-driven sequencing with full end-to-end traceability, per-service retries, and centralized monitoring of the entire order flow.**

Each service and its event emission is a simple, independent worker .  a plain Java class that does one thing. Conductor gives you choreography-style decoupling (each worker only knows its own concern) with orchestration benefits (full traceability, automatic retries, centralized monitoring). You get the best of both patterns without writing event routing code.

### What You Write: Workers

Seven workers model choreography with traceability: OrderServiceWorker, PaymentServiceWorker, and InventoryServiceWorker handle domain logic, while EmitOrderEventWorker, EmitPaymentEventWorker, and EmitInventoryEventWorker publish domain events, and NotificationServiceWorker sends the customer confirmation.

| Worker | Task | What It Does |
|---|---|---|
| **EmitInventoryEventWorker** | `ch_emit_inventory_event` | Emits an inventory event to the event bus. |
| **EmitOrderEventWorker** | `ch_emit_order_event` | Emits an order event to the event bus. |
| **EmitPaymentEventWorker** | `ch_emit_payment_event` | Emits a payment event to the event bus. |
| **InventoryServiceWorker** | `ch_inventory_service` | Reserves inventory items for an order. |
| **NotificationServiceWorker** | `ch_notification_service` | Sends a notification to the customer. |
| **OrderServiceWorker** | `ch_order_service` | Processes an incoming order and calculates the total amount. |
| **PaymentServiceWorker** | `ch_payment_service` | Processes payment for an order. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ch_order_service
    │
    ▼
ch_emit_order_event
    │
    ▼
ch_payment_service
    │
    ▼
ch_emit_payment_event
    │
    ▼
ch_inventory_service
    │
    ▼
ch_emit_inventory_event
    │
    ▼
ch_notification_service
```

## Example Output

```
=== Event Choreography Demo ===

Step 1: Registering task definitions...
  Registered: ch_order_service, ch_emit_order_event, ch_payment_service, ch_emit_payment_event, ch_inventory_service, ch_emit_inventory_event, ch_notification_service

Step 2: Registering workflow 'event_choreography'...
  Workflow registered.

Step 3: Starting workers...
  7 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [ch_emit_inventory_event] Event:
  [ch_emit_order_event] Event:
  [ch_emit_payment_event] Event:
  [ch_inventory_service] Reserved items for order
  [ch_notification_service] Sending
  [ch_order_service] Order
  [ch_payment_service] Charged

  Status: COMPLETED
  Output: {emitted=..., eventType=..., reserved=..., itemCount=...}

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
java -jar target/event-choreography-1.0.0.jar
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
java -jar target/event-choreography-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_choreography \
  --version 1 \
  --input '{"orderId": "ORD-CHOR-100", "ORD-CHOR-100": "customerId", "customerId": "CUST-42", "CUST-42": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_choreography -s COMPLETED -c 5
```

## How to Extend

Connect each service worker to your real order database, payment gateway (Stripe, Braintree), warehouse API, and notification service (SES, Twilio), the event-driven order fulfillment workflow stays exactly the same.

- **ChOrderServiceWorker** (`ch_order_service`): create orders in your order management system (Shopify, custom OMS) and persist them to your database
- **ChPaymentServiceWorker** (`ch_payment_service`): process payments via Stripe, Braintree, or Adyen and handle declined cards with retry logic
- **ChInventoryServiceWorker** (`ch_inventory_service`): reserve stock in your warehouse management system and trigger pick/pack/ship workflows
- **ChNotificationServiceWorker** (`ch_notification_service`): send order confirmation via email (SendGrid), SMS (Twilio), or push notification

Connecting each service worker to real APIs and each emit worker to Kafka or RabbitMQ leaves the choreography workflow unchanged.

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
event-choreography/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventchoreography/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventChoreographyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmitInventoryEventWorker.java
│       ├── EmitOrderEventWorker.java
│       ├── EmitPaymentEventWorker.java
│       ├── InventoryServiceWorker.java
│       ├── NotificationServiceWorker.java
│       ├── OrderServiceWorker.java
│       └── PaymentServiceWorker.java
└── src/test/java/eventchoreography/workers/
    ├── EmitInventoryEventWorkerTest.java        # 8 tests
    ├── EmitOrderEventWorkerTest.java        # 8 tests
    ├── EmitPaymentEventWorkerTest.java        # 8 tests
    ├── InventoryServiceWorkerTest.java        # 8 tests
    ├── NotificationServiceWorkerTest.java        # 8 tests
    ├── OrderServiceWorkerTest.java        # 8 tests
    └── PaymentServiceWorkerTest.java        # 8 tests
```
