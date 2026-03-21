# Delivery Tracking in Java with Conductor

Tracks a food delivery end-to-end: assigning a driver, recording pickup, tracking location en route, confirming delivery, and closing the order. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to track a food delivery from restaurant to customer. The workflow assigns an available delivery driver, records the pickup at the restaurant, tracks the driver's location en route, confirms delivery at the customer's address, and records completion. Late deliveries lead to cold food and unhappy customers; losing track of a driver means the customer has no ETA.

Without orchestration, you'd build a single delivery service that queries driver availability, updates status through the delivery lifecycle, sends push notifications, and handles exceptions (driver cancellation, wrong address). manually managing driver state across concurrent deliveries.

## The Solution

**You just write the driver assignment, pickup recording, location tracking, delivery confirmation, and order closure logic. Conductor handles tracking retries, ETA recalculations, and delivery chain audit trails.**

Each delivery concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (assign driver, pickup, track, deliver, confirm), retrying if the GPS tracking service is unavailable, tracking every delivery's full lifecycle, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Pickup confirmation, route tracking, ETA updates, and delivery completion workers each own one segment of the last-mile delivery chain.

| Worker | Task | What It Does |
|---|---|---|
| **AssignDriverWorker** | `dlt_assign_driver` | Assigns an available driver to the order and returns the driver ID, name, and ETA |
| **ConfirmWorker** | `dlt_confirm` | Confirms delivery completion for the order with a final status and customer rating |
| **DeliverWorker** | `dlt_deliver` | Records that the order has been delivered by the assigned driver with a delivery timestamp |
| **PickupWorker** | `dlt_pickup` | Records the driver picking up the order at the restaurant with a pickup timestamp |
| **TrackWorker** | `dlt_track` | Tracks the driver's GPS location (lat/lng) en route to the destination and returns the current ETA |

Workers implement food service operations. order processing, kitchen routing, delivery coordination,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### The Workflow

```
dlt_assign_driver
    │
    ▼
dlt_pickup
    │
    ▼
dlt_track
    │
    ▼
dlt_deliver
    │
    ▼
dlt_confirm

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
java -jar target/delivery-tracking-1.0.0.jar

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
java -jar target/delivery-tracking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow delivery_tracking_733 \
  --version 1 \
  --input '{"orderId": "TEST-001", "restaurantAddr": "sample-restaurantAddr", "customerAddr": "sample-customerAddr"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w delivery_tracking_733 -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real delivery stack. your driver dispatch system for assignment, GPS tracking for location updates, your order platform for delivery confirmation, and the workflow runs identically in production.

- **Driver assigner**: match orders to nearby available drivers using geolocation APIs (Google Maps, Mapbox) and driver availability status
- **Pickup recorder**: update order status when the driver confirms pickup at the restaurant; notify the customer of estimated arrival
- **Location tracker**: poll driver GPS coordinates via your fleet tracking system and update the customer's live map view
- **Delivery confirmer**: capture proof of delivery (photo, signature) and update order status; trigger customer feedback request
- **Completion handler**: calculate driver earnings, update delivery metrics, and close the order

Swap GPS providers or notification services and the tracking pipeline operates identically.

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
delivery-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/deliverytracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DeliveryTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignDriverWorker.java
│       ├── ConfirmWorker.java
│       ├── DeliverWorker.java
│       ├── PickupWorker.java
│       └── TrackWorker.java
└── src/test/java/deliverytracking/workers/
    ├── AssignDriverWorkerTest.java
    └── ConfirmWorkerTest.java

```
