# Shipping Workflow in Java Using Conductor :  Select Carrier, Create Label, Track, Deliver, Confirm

A Java Conductor workflow example for end-to-end shipment fulfillment .  selecting the optimal carrier based on package weight and dimensions, generating a shipping label, tracking the parcel in transit, confirming delivery, and closing out the order. Uses [Conductor](https://github.

## Shipping Involves Multiple Carriers and Real-Time Tracking

A 5 lb package going from New York to Los Angeles with next-day shipping. FedEx quotes $32, UPS quotes $29, USPS doesn't offer next-day for this weight. The system must compare rates across carriers, select the cheapest qualifying option, generate a shipping label with barcode and customs information (for international), track the package through pickup, transit, and delivery, and confirm delivery with signature verification if required.

Carrier APIs are notoriously unreliable .  rate requests timeout, label generation returns errors during peak season, and tracking updates arrive out of order. If label creation fails with FedEx, the system should try UPS without re-running the rate comparison. Delivery confirmation may take days, requiring the workflow to wait for carrier webhooks or polling updates.

## The Solution

**You just write the carrier selection, label creation, tracking, and delivery confirmation logic. Conductor handles carrier retries, label generation sequencing, and shipment tracking across providers.**

`SelectCarrierWorker` compares rates across carriers for the given weight, dimensions, origin, destination, and speed requirement, selecting the optimal option. `CreateLabelWorker` generates the shipping label with the selected carrier .  including barcode, tracking number, and any customs documentation. `TrackWorker` monitors the package through transit milestones (picked up, in transit, out for delivery). `DeliverWorker` confirms final delivery with signature verification and proof of delivery. `ConfirmWorker` closes the shipment record and sends delivery notification to the customer. Conductor sequences these stages and records every tracking update for shipment analytics.

### What You Write: Workers

Label creation, carrier selection, tracking setup, and delivery confirmation workers each own one stage of the shipping lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **SelectCarrierWorker** | `shp_select_carrier` | Evaluates package weight, dimensions, origin/destination, and speed to pick the best carrier and service type |
| **CreateLabelWorker** | `shp_create_label` | Generates a shipping label with tracking number for the selected carrier |
| **TrackShipmentWorker** | `shp_track` | Polls the carrier for the current transit status of the shipment |
| **DeliverShipmentWorker** | `shp_deliver` | Records the delivery event at the destination address |
| **ConfirmDeliveryWorker** | `shp_confirm` | Confirms delivery back to the order system and closes out the shipment |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
shp_select_carrier
    │
    ▼
shp_create_label
    │
    ▼
shp_track
    │
    ▼
shp_deliver
    │
    ▼
shp_confirm

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
java -jar target/shipping-workflow-1.0.0.jar

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
java -jar target/shipping-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow shipping_workflow \
  --version 1 \
  --input '{"orderId": "TEST-001", "weight": "sample-weight", "dimensions": "sample-dimensions", "origin": "sample-origin", "destination": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w shipping_workflow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to real carrier APIs. EasyPost for rate shopping, FedEx or UPS for label generation, AfterShip for tracking webhooks, and the workflow runs identically in production.

- **SelectCarrierWorker** (`shp_select_carrier`): use EasyPost, Shippo, or ShipStation for multi-carrier rate shopping across FedEx, UPS, USPS, DHL with real-time rate quotes
- **CreateLabelWorker** (`shp_create_label`): generate labels via carrier APIs (FedEx Ship API, UPS Shipping API) with return labels, customs forms for international shipments, and hazmat declarations
- **TrackWorker** (`shp_track`): subscribe to carrier tracking webhooks (EasyPost Trackers, AfterShip) for real-time status updates, or poll carrier APIs for tracking milestones

Switch carriers or label generation services and the shipping pipeline adjusts without changes to the workflow.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
shipping-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/shippingworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ShippingWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmDeliveryWorker.java
│       ├── CreateLabelWorker.java
│       ├── DeliverShipmentWorker.java
│       ├── SelectCarrierWorker.java
│       └── TrackShipmentWorker.java
└── src/test/java/shippingworkflow/workers/
    ├── ConfirmDeliveryWorkerTest.java        # 2 tests
    ├── CreateLabelWorkerTest.java        # 4 tests
    ├── DeliverShipmentWorkerTest.java        # 2 tests
    ├── SelectCarrierWorkerTest.java        # 3 tests
    └── TrackShipmentWorkerTest.java        # 2 tests

```
