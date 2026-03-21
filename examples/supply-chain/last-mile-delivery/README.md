# Last Mile Delivery in Java with Conductor :  Driver Assignment, Route Optimization, Package Delivery, and Delivery Confirmation

A Java Conductor workflow example for last mile delivery. assigning a driver to an order (e.g., ORD-2024-668 going to 742 Evergreen Terrace with a 2pm-4pm delivery window), optimizing the delivery route across all stops, executing the delivery, and confirming receipt with proof of delivery. Uses [Conductor](https://github.

## The Problem

You need to deliver packages to customers within promised time windows. Order ORD-2024-668 has a 2pm-4pm delivery window at 742 Evergreen Terrace, Springfield. A driver must be assigned based on availability, location, and vehicle capacity. The route must be optimized across all the driver's stops to minimize distance while honoring each customer's time window. The delivery must be executed and confirmed with proof. a signature, photo, or safe-place confirmation. If the delivery fails (customer not home, wrong address), a reattempt must be scheduled.

Without orchestration, dispatchers assign drivers manually, drivers navigate using their own judgment, and delivery confirmations come in as text messages or phone calls. If route optimization runs but the driver assignment changes afterward, the route is invalid. When a delivery fails, the dispatcher doesn't know until the driver calls in, and rescheduling happens via sticky notes. There is no end-to-end visibility into whether a package was delivered within its promised window.

## The Solution

**You just write the delivery workers. Driver assignment, route optimization, package delivery, and proof-of-delivery capture. Conductor handles driver-to-route coordination, automatic retries on upload failures, and end-to-end SLA tracking.**

Each step of the last mile process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so driver assignment considers the route, routes are optimized for the assigned driver's full stop list, delivery is executed with the optimized route, and confirmation captures proof of delivery. If the delivery confirmation worker fails to upload the signature photo, Conductor retries without reassigning the driver. Every assignment, route plan, delivery attempt, and confirmation is recorded for SLA tracking and customer service visibility.

### What You Write: Workers

Four workers manage each delivery: AssignDriverWorker selects a driver by availability and capacity, OptimizeRouteWorker plans the most efficient stop sequence, DeliverWorker executes the drop-off, and ConfirmWorker captures proof of delivery.

| Worker | Task | What It Does |
|---|---|---|
| **AssignDriverWorker** | `lmd_assign_driver` | Assigns a driver based on availability, location, and vehicle capacity. |
| **ConfirmWorker** | `lmd_confirm` | Captures proof of delivery. signature, photo, or safe-place confirmation. |
| **DeliverWorker** | `lmd_deliver` | Executes the delivery to the customer address within the time window. |
| **OptimizeRouteWorker** | `lmd_optimize_route` | Optimizes the delivery route across all stops to minimize distance while honoring time windows. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
lmd_assign_driver
    │
    ▼
lmd_optimize_route
    │
    ▼
lmd_deliver
    │
    ▼
lmd_confirm

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
java -jar target/last-mile-delivery-1.0.0.jar

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
java -jar target/last-mile-delivery-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lmd_last_mile_delivery \
  --version 1 \
  --input '{"orderId": "TEST-001", "address": "sample-address", "timeWindow": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lmd_last_mile_delivery -s COMPLETED -c 5

```

## How to Extend

Connect AssignDriverWorker to your fleet management system, OptimizeRouteWorker to a routing engine (Google Routes API, OSRM), and ConfirmWorker to your proof-of-delivery platform. The workflow definition stays exactly the same.

- **AssignDriverWorker** (`lmd_assign_driver`): match orders to drivers using your fleet management system (Onfleet, Bringg, or Route4Me), considering driver location, vehicle capacity, and delivery window constraints
- **OptimizeRouteWorker** (`lmd_optimize_route`): compute the optimal stop sequence using Google Maps Directions API, HERE Routing API, or a TSP/VRP solver, minimizing total distance while honoring each customer's time window
- **DeliverWorker** (`lmd_deliver`): push the delivery task to the driver's mobile app, track GPS location during delivery, and handle exceptions (access code needed, gate closed, no safe place)
- **ConfirmWorker** (`lmd_confirm`): capture proof of delivery (e-signature, delivery photo, GPS coordinates at drop-off), update the order management system, and trigger customer notification

Swap any simulation for a real routing engine or fleet API while maintaining the output contract, and the delivery workflow requires no changes.

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
last-mile-delivery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/lastmiledelivery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LastMileDeliveryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignDriverWorker.java
│       ├── ConfirmWorker.java
│       ├── DeliverWorker.java
│       └── OptimizeRouteWorker.java
└── src/test/java/lastmiledelivery/workers/
    ├── AssignDriverWorkerTest.java        # 2 tests
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── DeliverWorkerTest.java        # 2 tests
    └── OptimizeRouteWorkerTest.java        # 2 tests

```
