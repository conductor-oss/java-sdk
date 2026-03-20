# Event Driven Microservices in Java Using Conductor

Event-driven microservices workflow: order_service -> emit_order_created -> payment_service -> emit_payment_processed -> shipping_service -> emit_shipment_created -> notification_service -> finalize. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to coordinate an order fulfillment pipeline across four independent microservices. The order service creates the order, the payment service charges the customer, the shipping service arranges delivery, and the notification service confirms everything .  each emitting domain events between steps. Every service must be independently deployable and loosely coupled, but the end-to-end flow must be reliable and observable.

Without orchestration, you'd have each microservice publish and subscribe to event topics; but lose end-to-end visibility across the four services, have no centralized way to retry a failed payment without re-triggering the entire flow, and spend hours tracing a single order through distributed logs to find where it stalled.

## The Solution

**You just write the order, payment, shipping, notification, and event-emission workers. Conductor handles inter-service event sequencing, per-service retry with backoff, and end-to-end order lifecycle traceability.**

Each microservice is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (order, payment, shipping, notification) with event emissions between each step, retrying any service that fails, tracking the entire order lifecycle in one place, and resuming from the last successful service if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Eight workers chain four microservices with event emissions: OrderServiceWorker, PaymentServiceWorker, ShippingServiceWorker, and NotificationServiceWorker handle domain logic, while EmitOrderCreatedWorker, EmitPaymentProcessedWorker, and EmitShipmentCreatedWorker publish domain events between steps, and FinalizeWorker completes the flow.

| Worker | Task | What It Does |
|---|---|---|
| **EmitOrderCreatedWorker** | `dm_emit_order_created` | Emits the order.created event. |
| **EmitPaymentProcessedWorker** | `dm_emit_payment_processed` | Emits the payment.processed event. |
| **EmitShipmentCreatedWorker** | `dm_emit_shipment_created` | Emits the shipment.created event. |
| **FinalizeWorker** | `dm_finalize` | Finalizes the event-driven microservices workflow. |
| **NotificationServiceWorker** | `dm_notification_service` | Sends notifications to the customer about their order. |
| **OrderServiceWorker** | `dm_order_service` | Creates an order from customer and items input. |
| **PaymentServiceWorker** | `dm_payment_service` | Processes payment for an order. |
| **ShippingServiceWorker** | `dm_shipping_service` | Arranges shipping for an order. |

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
dm_order_service
    │
    ▼
dm_emit_order_created
    │
    ▼
dm_payment_service
    │
    ▼
dm_emit_payment_processed
    │
    ▼
dm_shipping_service
    │
    ▼
dm_emit_shipment_created
    │
    ▼
dm_notification_service
    │
    ▼
dm_finalize
```

## Example Output

```
=== Event-Driven Microservices Demo ===

Step 1: Registering task definitions...
  Registered: dm_order_service, dm_emit_order_created, dm_payment_service, dm_emit_payment_processed, dm_shipping_service, dm_emit_shipment_created, dm_notification_service, dm_finalize

Step 2: Registering workflow 'event_driven_microservices'...
  Workflow registered.

Step 3: Starting workers...
  8 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [event-bus] Published:
  [event-bus] Published:
  [event-bus] Published:
  [finalize] Order
  [notification-svc] Sent order confirmation to customer
  [order-svc] Created order
  [payment-svc] Charged $
  [shipping-svc] Created shipment

  Status: COMPLETED
  Output: {published=..., eventType=..., servicesCompleted=..., eventsPublished=...}

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
java -jar target/event-driven-microservices-1.0.0.jar
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
java -jar target/event-driven-microservices-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_driven_microservices \
  --version 1 \
  --input '{"customerId": "CUST-DM-100", "CUST-DM-100": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_driven_microservices -s COMPLETED -c 5
```

## How to Extend

Connect each service worker to your real order database, payment gateway, shipping provider, and notification service, and each emit worker to your real event bus (Kafka, SNS), the event-driven order pipeline workflow stays exactly the same.

- **Order Service**: create orders in your OMS and emit OrderCreated events to your event bus (Kafka, EventBridge)
- **Payment Service**: process payments via Stripe/Adyen and emit PaymentProcessed events
- **Shipping Service**: arrange shipments via carrier APIs (UPS, FedEx) and emit ShipmentCreated events
- **Notification Service**: send multi-channel notifications (email, SMS, push) via SendGrid/Twilio

Connecting each service worker to its real API and each emit worker to Kafka or SNS requires no changes to the eight-step workflow.

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
event-driven-microservices/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventdrivenmicroservices/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventDrivenMicroservicesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmitOrderCreatedWorker.java
│       ├── EmitPaymentProcessedWorker.java
│       ├── EmitShipmentCreatedWorker.java
│       ├── FinalizeWorker.java
│       ├── NotificationServiceWorker.java
│       ├── OrderServiceWorker.java
│       ├── PaymentServiceWorker.java
│       └── ShippingServiceWorker.java
└── src/test/java/eventdrivenmicroservices/workers/
    ├── EmitOrderCreatedWorkerTest.java        # 8 tests
    ├── EmitPaymentProcessedWorkerTest.java        # 8 tests
    ├── EmitShipmentCreatedWorkerTest.java        # 8 tests
    ├── FinalizeWorkerTest.java        # 9 tests
    ├── NotificationServiceWorkerTest.java        # 8 tests
    ├── OrderServiceWorkerTest.java        # 9 tests
    ├── PaymentServiceWorkerTest.java        # 8 tests
    └── ShippingServiceWorkerTest.java        # 8 tests
```
