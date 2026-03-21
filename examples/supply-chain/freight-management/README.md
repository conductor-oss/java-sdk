# Freight Management in Java with Conductor :  Carrier Booking, Shipment Tracking, Delivery Confirmation, Invoicing, and Reconciliation

A Java Conductor workflow example for freight management .  booking a carrier (e.g., FastFreight Express for a 2,500 lb shipment from Detroit to Houston), tracking the shipment in transit, confirming delivery at destination, generating the freight invoice, and reconciling charges against the contracted rate. Uses [Conductor](https://github.

## The Problem

You need to manage freight shipments end-to-end. A 2,500 lb load needs to move from Detroit, MI to Houston, TX .  you book a carrier at a quoted rate, track the shipment through pickup, in-transit, and delivery milestones, confirm proof of delivery at the destination, receive the carrier's freight invoice, and reconcile the billed amount against the contracted rate (catching accessorial charges, fuel surcharges, or billing errors). If tracking data from the carrier API goes missing, you lose visibility into a $3K+ shipment.

Without orchestration, the logistics coordinator books carriers via email or phone, checks tracking portals manually, and reconciles invoices in a spreadsheet at month-end. Discrepancies between contracted rates and billed amounts go unnoticed until AP discovers them weeks later. When a shipment is delayed, there is no automated escalation .  someone has to notice the tracking page hasn't updated.

## The Solution

**You just write the freight workers. Carrier booking, shipment tracking, delivery confirmation, invoicing, and rate reconciliation. Conductor handles step ordering, carrier API retries, and full shipment audit trails for freight dispute resolution.**

Each phase of the freight lifecycle is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so the shipment is booked before tracking begins, delivery is confirmed before invoicing, and reconciliation catches discrepancies before payment is released. If the carrier's tracking API is temporarily unavailable, Conductor retries without re-booking the shipment. Every booking confirmation, tracking event, delivery receipt, invoice, and reconciliation result is recorded for freight audit and dispute resolution.

### What You Write: Workers

Five workers span the freight lifecycle: BookWorker reserves the carrier, TrackWorker monitors transit milestones, DeliverWorker captures proof of delivery, InvoiceWorker generates billing, and ReconcileWorker flags rate discrepancies.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `frm_book` | Books the carrier with origin, destination, weight, and returns a booking ID and rate. |
| **DeliverWorker** | `frm_deliver` | Confirms delivery at the destination with proof of delivery. |
| **InvoiceWorker** | `frm_invoice` | Generates the freight invoice based on the booking rate and shipment details. |
| **ReconcileWorker** | `frm_reconcile` | Reconciles the invoiced amount against the contracted rate and flags discrepancies. |
| **TrackWorker** | `frm_track` | Tracks the shipment through pickup, in-transit, and delivery milestones. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
frm_book
    │
    ▼
frm_track
    │
    ▼
frm_deliver
    │
    ▼
frm_invoice
    │
    ▼
frm_reconcile

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
java -jar target/freight-management-1.0.0.jar

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
java -jar target/freight-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow frm_freight_management \
  --version 1 \
  --input '{"origin": "sample-origin", "destination": "production", "weight": "sample-weight", "carrier": "sample-carrier"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w frm_freight_management -s COMPLETED -c 5

```

## How to Extend

Connect BookWorker to your carrier's booking API (FedEx, UPS, or your TMS), TrackWorker to their tracking endpoint, and ReconcileWorker to your accounts payable system. The workflow definition stays exactly the same.

- **BookWorker** (`frm_book`): request quotes and book shipments via carrier APIs (FedEx Freight, XPO, Saia) or your TMS (MercuryGate, Oracle Transportation Management), selecting the best rate for the lane
- **TrackWorker** (`frm_track`): poll carrier tracking APIs or integrate with visibility platforms (FourKites, project44) for real-time milestone updates (picked up, in transit, out for delivery)
- **DeliverWorker** (`frm_deliver`): capture proof of delivery (POD) signatures and photos, confirm delivery timestamps, and flag exceptions (damage, shortage, wrong location)
- **InvoiceWorker** (`frm_invoice`): receive and parse the carrier's freight invoice (EDI 210 or API), extract line items including base rate, fuel surcharge, and accessorial charges
- **ReconcileWorker** (`frm_reconcile`): compare the invoiced amount against the contracted rate and booking quote, flag discrepancies above threshold, and route approved invoices to AP for payment

Integrate any worker with your TMS or carrier API while preserving output fields, and the freight workflow needs no modification.

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
freight-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/freightmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FreightManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java
│       ├── DeliverWorker.java
│       ├── InvoiceWorker.java
│       ├── ReconcileWorker.java
│       └── TrackWorker.java
└── src/test/java/freightmanagement/workers/
    ├── BookWorkerTest.java        # 2 tests
    ├── DeliverWorkerTest.java        # 2 tests
    ├── InvoiceWorkerTest.java        # 2 tests
    ├── ReconcileWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests

```
