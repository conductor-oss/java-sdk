# Purchase Order Lifecycle in Java with Conductor :  PO Creation, Approval, Vendor Transmission, Order Tracking, and Goods Receipt

A Java Conductor workflow example for purchase order lifecycle management .  creating a PO with line items and vendor details, obtaining budget/authority approval, transmitting the order to the vendor, tracking fulfillment status, and confirming goods receipt at the warehouse. Uses [Conductor](https://github.## The Problem

You need to manage purchase orders from creation through delivery. A PO must be created with vendor, items, quantities, and pricing. It requires approval based on the total amount and the requester's authority level. The approved PO must be transmitted to the vendor (via EDI, email, or supplier portal). The order must be tracked for fulfillment status (acknowledged, shipped, partial delivery). Finally, goods receipt must be confirmed against the original PO line items.

Without orchestration, buyers create POs in the ERP but send them to vendors via email separately .  sometimes the email fails and the vendor never receives the order. Tracking is manual: buyers call vendors weekly to check status. When goods arrive, the receiving team doesn't always know which PO to match against, leading to receiving errors and payment delays.

## The Solution

**You just write the PO lifecycle workers. Creation, approval, vendor transmission, order tracking, and goods receipt. Conductor handles transmission retries, fulfillment tracking, and full PO history for spend analytics.**

Each stage of the purchase order lifecycle is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so POs are only sent after approval, tracking begins only after the vendor confirms receipt, and goods are received only against transmitted POs. If the vendor transmission fails (EDI timeout, email bounce), Conductor retries without re-creating or re-approving the PO. Every PO creation, approval, transmission confirmation, tracking update, and receiving record is captured for spend analytics and supplier performance measurement.

### What You Write: Workers

Five workers manage PO lifecycle: CreateWorker drafts the order, ApproveWorker checks authority, SendWorker transmits to the vendor, TrackWorker monitors fulfillment, and ReceiveWorker confirms goods arrival.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `po_approve` | Approves the purchase order based on total amount and requester's authority level. |
| **CreateWorker** | `po_create` | Creates a purchase order with vendor, items, quantities, and pricing. |
| **ReceiveWorker** | `po_receive` | Confirms goods receipt against the original PO line items. |
| **SendWorker** | `po_send` | Transmits the approved PO to the vendor via EDI, email, or supplier portal. |
| **TrackWorker** | `po_track` | Tracks order fulfillment status .  acknowledged, shipped, partial delivery. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
po_create
    │
    ▼
po_approve
    │
    ▼
po_send
    │
    ▼
po_track
    │
    ▼
po_receive
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
java -jar target/purchase-order-1.0.0.jar
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
java -jar target/purchase-order-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow po_purchase_order \
  --version 1 \
  --input '{"vendor": "test-value", "items": "test-value", "totalAmount": 100}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w po_purchase_order -s COMPLETED -c 5
```

## How to Extend

Connect CreateWorker to your ERP, SendWorker to your EDI gateway or supplier portal, and TrackWorker to your vendor's order status API. The workflow definition stays exactly the same.

- **CreateWorker** (`po_create`): generate POs in your ERP (SAP, Oracle, NetSuite) with line items, pricing, delivery dates, and payment terms
- **ApproveWorker** (`po_approve`): route POs through your approval hierarchy based on dollar amount and category, integrating with Slack or your ERP approval workflow
- **SendWorker** (`po_send`): transmit POs to vendors via EDI 850, cXML PunchOut, supplier portal upload, or email with PDF attachment
- **TrackWorker** (`po_track`): poll vendor order acknowledgments (EDI 855) and shipment notices (EDI 856), updating PO status in your ERP
- **ReceiveWorker** (`po_receive`): match received goods against PO line items in your WMS, flagging quantity discrepancies and quality issues for buyer review

Integrate any worker with your ERP or EDI gateway while preserving its return fields, and the PO lifecycle flow needs no reconfiguration.

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
purchase-order/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/purchaseorder/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PurchaseOrderExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── CreateWorker.java
│       ├── ReceiveWorker.java
│       ├── SendWorker.java
│       └── TrackWorker.java
└── src/test/java/purchaseorder/workers/
    ├── ApproveWorkerTest.java        # 3 tests
    ├── CreateWorkerTest.java        # 2 tests
    ├── ReceiveWorkerTest.java        # 2 tests
    ├── SendWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests
```
