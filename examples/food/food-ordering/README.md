# Food Ordering in Java with Conductor

Processes a food order from menu browsing through payment, kitchen preparation, and delivery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to process a food order from browsing to delivery. The customer browses the restaurant's menu, places an order with selected items, pays for the order, the kitchen prepares the food, and it is delivered (or picked up). Each step depends on the previous .  you cannot prepare food without a confirmed, paid order; you cannot deliver without prepared food.

Without orchestration, you'd build a monolithic ordering app that handles menu display, cart management, payment processing, kitchen dispatch, and delivery coordination in one service .  manually managing order state through each phase, retrying failed payment attempts, and handling the handoff between payment confirmation and kitchen preparation.

## The Solution

**You just write the menu selection, payment processing, kitchen preparation, and delivery dispatch logic. Conductor handles payment retries, kitchen dispatch sequencing, and order lifecycle tracking.**

Each ordering concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (browse, order, pay, prepare, deliver), retrying if the payment gateway is temporarily unavailable, tracking every order from menu to doorstep, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Menu browsing, cart assembly, payment, and kitchen dispatch workers handle the ordering flow as independent, loosely coupled services.

| Worker | Task | What It Does |
|---|---|---|
| **BrowseWorker** | `fod_browse` | Browses the restaurant menu and returns selected items with names, prices, and quantities |
| **DeliverWorker** | `fod_deliver` | Delivers the order and returns delivery status with ETA |
| **OrderWorker** | `fod_order` | Places the order for the customer and returns an order ID, total, and item count |
| **PayWorker** | `fod_pay` | Processes payment for the order total and returns a transaction ID |
| **PrepareWorker** | `fod_prepare` | Prepares the order in the kitchen and returns prep time and readiness status |

Workers simulate food service operations .  order processing, kitchen routing, delivery coordination ,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
fod_browse
    │
    ▼
fod_order
    │
    ▼
fod_pay
    │
    ▼
fod_prepare
    │
    ▼
fod_deliver
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
java -jar target/food-ordering-1.0.0.jar
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
java -jar target/food-ordering-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow food_ordering_731 \
  --version 1 \
  --input '{"customerId": "TEST-001", "restaurantId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w food_ordering_731 -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real ordering stack .  your POS for menu browsing, Stripe for payment, your KDS for kitchen prep, your delivery platform for dispatch, and the workflow runs identically in production.

- **Menu browser**: fetch real-time menu data from your restaurant platform (Toast, Square, custom POS) with pricing and availability
- **Order placer**: validate item availability, apply modifiers (extra cheese, no onions), and calculate order total with taxes
- **Payment processor**: charge via Stripe, Square, or Apple Pay/Google Pay with tip calculation
- **Kitchen dispatcher**: send the order to your KDS (Kitchen Display System) with prep instructions and estimated completion time
- **Delivery handler**: dispatch to your delivery fleet or integrate with DoorDash Drive, Uber Direct, or in-house driver assignment

Replace your payment processor or kitchen display system and the ordering flow keeps its shape.

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
food-ordering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/foodordering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FoodOrderingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BrowseWorker.java
│       ├── DeliverWorker.java
│       ├── OrderWorker.java
│       ├── PayWorker.java
│       └── PrepareWorker.java
└── src/test/java/foodordering/workers/
    ├── BrowseWorkerTest.java
    ├── DeliverWorkerTest.java
    ├── OrderWorkerTest.java
    ├── PayWorkerTest.java
    └── PrepareWorkerTest.java
```
