# Choreography Vs Orchestration in Java with Conductor

Service A publishes an `order.created` event. Service B picks it up and reserves inventory. Service C is supposed to process the payment; but someone on the payments team renamed the topic from `order.created` to `orders.new` three sprints ago, and nobody updated the documentation. So Service C never fires. The order sits in "reserved" limbo. There's no central view of the flow, no shared transaction ID, and debugging means correlating logs across four services to discover that the event your payment service is listening for simply doesn't exist anymore. This example replaces invisible choreography with explicit Conductor orchestration: place order, reserve inventory, process payment, ship: each step visible, traceable, and retriable from a single execution view. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

An e-commerce order involves placing the order, reserving inventory, processing payment, and shipping. Four services that must coordinate. In a choreography approach, each service emits events and the next service reacts, but the overall flow is invisible and failures are hard to trace. Orchestration makes the flow explicit: Conductor drives each step, passes data between services, and provides a single execution view.

Without orchestration (pure choreography), you lose visibility into the end-to-end order flow. If the payment service silently drops an event, the order is stuck with no alert. Debugging requires correlating logs across four services with no shared transaction ID.

## The Solution

**You just write the order, inventory, payment, and shipping workers. Conductor handles cross-service sequencing, compensating retries, and end-to-end order traceability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

The order flow chains four domain workers: PlaceOrderWorker creates the order, ReserveInventoryWorker holds stock, ProcessPaymentWorker charges the customer, and ShipOrderWorker dispatches the shipment.

| Worker | Task | What It Does |
|---|---|---|
| **PlaceOrderWorker** | `cvo_place_order` | Creates the order record with items and computes the order total. |
| **ProcessPaymentWorker** | `cvo_process_payment` | Charges the order total to the customer's payment method. |
| **ReserveInventoryWorker** | `cvo_reserve_inventory` | Reserves the ordered items in the warehouse and returns the warehouse ID for shipping. |
| **ShipOrderWorker** | `cvo_ship_order` | Creates a shipment from the assigned warehouse and returns a tracking ID. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients, the workflow coordination stays the same.

### The Workflow

```
cvo_place_order
    │
    ▼
cvo_reserve_inventory
    │
    ▼
cvo_process_payment
    │
    ▼
cvo_ship_order

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
java -jar target/choreography-vs-orchestration-1.0.0.jar

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
java -jar target/choreography-vs-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow orchestrated_order_flow \
  --version 1 \
  --input '{"orderId": "ORD-900", "items": ["widget"], "customerId": "CUST-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w orchestrated_order_flow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real order database, Stripe or Braintree payment gateway, warehouse API, and shipping provider, the orchestrated order flow stays exactly the same.

- **PlaceOrderWorker** (`cvo_place_order`): write to your order database (Postgres, DynamoDB) and compute real pricing with tax
- **ProcessPaymentWorker** (`cvo_process_payment`): integrate with a payment gateway (Stripe, Braintree, Adyen) to charge the customer
- **ReserveInventoryWorker** (`cvo_reserve_inventory`): query your inventory management system (ERP, warehouse API) for real stock reservation

Swapping from a simulated payment gateway to Stripe or Braintree leaves the orchestrated order flow unchanged.

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
choreography-vs-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/choreographyvsorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChoreographyVsOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PlaceOrderWorker.java
│       ├── ProcessPaymentWorker.java
│       ├── ReserveInventoryWorker.java
│       └── ShipOrderWorker.java
└── src/test/java/choreographyvsorchestration/workers/
    ├── PlaceOrderWorkerTest.java        # 2 tests
    ├── ProcessPaymentWorkerTest.java        # 2 tests
    ├── ReserveInventoryWorkerTest.java        # 2 tests
    └── ShipOrderWorkerTest.java        # 2 tests

```
