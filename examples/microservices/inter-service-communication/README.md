# Inter Service Communication in Java with Conductor

Orchestrates request-response communication between microservices. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Fulfilling a customer order requires coordinating four microservices in sequence: the order service validates the order, the inventory service reserves stock, the shipping service creates a shipment, and the notification service sends the customer a tracking email. Each service depends on the output of the previous one.

Without orchestration, the calling service makes four sequential HTTP calls with bespoke error handling around each one. If the shipping service is down, the order and inventory changes are already committed with no automatic compensation, and there is no single view of the order fulfillment pipeline.

## The Solution

**You just write the order, inventory, shipping, and notification service workers. Conductor handles sequential service coordination, per-call retries with backoff, and end-to-end order traceability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four service workers chain together for order fulfillment: OrderServiceWorker validates the order, InventoryServiceWorker reserves stock, ShippingServiceWorker creates a shipment, and NotificationServiceWorker sends tracking to the customer.

| Worker | Task | What It Does |
|---|---|---|
| **InventoryServiceWorker** | `isc_inventory_service` | Reserves items for the order in the nearest warehouse, returning the warehouse ID. |
| **NotificationServiceWorker** | `isc_notification_service` | Sends the tracking information to the customer via email. |
| **OrderServiceWorker** | `isc_order_service` | Validates and processes the incoming order, returning an order reference number. |
| **ShippingServiceWorker** | `isc_shipping_service` | Creates a shipment from the assigned warehouse and returns a tracking ID and ETA. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
isc_order_service
    │
    ▼
isc_inventory_service
    │
    ▼
isc_shipping_service
    │
    ▼
isc_notification_service
```

## Example Output

```
=== Example 305: Inter-Service Communicatio ===

Step 1: Registering task definitions...
  Registered: isc_order_service, isc_inventory_service, isc_shipping_service, isc_notification_service

Step 2: Registering workflow 'inter_service_comm_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [inventory] Reserving items for
  [notify] Sent tracking
  [order] Processing order
  [shipping] Creating shipment from

  Status: COMPLETED
  Output: {reserved=..., warehouse=..., sent=..., channel=...}

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
java -jar target/inter-service-communication-1.0.0.jar
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
java -jar target/inter-service-communication-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow inter_service_comm_workflow \
  --version 1 \
  --input '{"orderId": "ORD-500", "ORD-500": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}], "CUST-42": "sample-CUST-42"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w inter_service_comm_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real order management API, warehouse system, shipping provider (FedEx, UPS), and notification service (SES, Twilio), the order-fulfillment workflow stays exactly the same.

- **InventoryServiceWorker** (`isc_inventory_service`): call your warehouse management system or ERP API for real stock reservation
- **NotificationServiceWorker** (`isc_notification_service`): send real notifications via email (SES, SendGrid), SMS (Twilio), or push notification services
- **OrderServiceWorker** (`isc_order_service`): call your order management API or write directly to the orders database

Connecting each worker to your real warehouse API, shipping provider, or email service preserves the fulfillment pipeline.

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
inter-service-communication/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/interservicecommunication/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InterServiceCommunicationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── InventoryServiceWorker.java
│       ├── NotificationServiceWorker.java
│       ├── OrderServiceWorker.java
│       └── ShippingServiceWorker.java
└── src/test/java/interservicecommunication/workers/
    ├── InventoryServiceWorkerTest.java        # 3 tests
    ├── NotificationServiceWorkerTest.java        # 3 tests
    ├── OrderServiceWorkerTest.java        # 3 tests
    └── ShippingServiceWorkerTest.java        # 3 tests
```
