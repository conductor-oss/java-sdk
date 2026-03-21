# Procurement Workflow in Java with Conductor: Requisition, Budget Approval, Purchase Order, Goods Receipt, and Payment Processing

The ops team submitted a purchase request for 50 server racks on Tuesday. It sat in a VP's inbox for nine days because she was on a trip and didn't know it was waiting for her. By the time she approved it, the supplier's bulk pricing window had closed and the unit cost went up 15%. $38,000 wasted on a signature delay. Meanwhile, accounts payable just paid an invoice for a different order where the goods never actually arrived, because there was no gate between "PO sent" and "payment released." Nobody noticed until the quarterly reconciliation, eight weeks later. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full procure-to-pay cycle, requisition, budget approval, purchase order, goods receipt, and payment, with enforced gates so nothing moves forward until the previous step is verified.

## The Problem

You need to manage procurement from request to payment. A department submits a requisition for materials with quantity and budget justification. The requisition must be approved based on budget authority and spending limits. Once approved, a purchase order is created and sent to the vendor. Goods receipt must be confirmed against the PO before payment can be released. If payment is processed before goods are verified, you risk paying for items never received or not matching specifications.

Without orchestration, requisitions arrive via email, sit in an approver's inbox for days, and purchase orders are created manually in the ERP. There is no enforced gate between goods receipt and payment. AP might pay invoices for goods that haven't arrived. When audit asks for a trail from requisition to payment for a specific purchase, reconstructing the chain across email, ERP, and the bank takes hours.

## The Solution

**You just write the procurement workers. Requisition intake, budget approval, PO creation, goods receipt, and payment processing. Conductor handles enforced approval gates, payment retries, and a complete requisition-to-payment audit trail.**

Each stage of the procure-to-pay process is a simple, independent worker, a plain Java class that does one thing. Conductor sequences them so requisitions are approved before purchase orders are created, POs are sent before goods can be received, and payment only processes after goods receipt is confirmed. If the payment processing worker fails, Conductor retries it without re-creating the purchase order. Every requisition, approval decision, PO, goods receipt, and payment is tracked with timestamps for audit and spend analytics.

### What You Write: Workers

Five workers cover the procure-to-pay cycle: RequisitionWorker creates the request, ApproveWorker validates the budget, PurchaseWorker generates the PO, ReceiveWorker confirms delivery, and PayWorker processes vendor payment.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `prw_approve` | Approves or rejects a requisition based on budget. |
| **PayWorker** | `prw_pay` | Processes payment for a purchase order. |
| **PurchaseWorker** | `prw_purchase` | Creates a purchase order. |
| **ReceiveWorker** | `prw_receive` | Confirms goods receipt. |
| **RequisitionWorker** | `prw_requisition` | Creates a purchase requisition. |

Workers simulate supply chain operations: inventory checks, shipment tracking, supplier coordination, with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
prw_requisition
    │
    ▼
prw_approve
    │
    ▼
prw_purchase
    │
    ▼
prw_receive
    │
    ▼
prw_pay

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
java -jar target/procurement-workflow-1.0.0.jar

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
java -jar target/procurement-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow prw_procurement \
  --version 1 \
  --input '{"item": "Office Laptops", "quantity": 100, "budget": 50000, "requester": "eng-manager"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w prw_procurement -s COMPLETED -c 5

```

## How to Extend

Connect RequisitionWorker to your procurement portal, ApproveWorker to your budget authority system, and PayWorker to your accounts payable and banking integration. The workflow definition stays exactly the same.

- **RequisitionWorker** (`prw_requisition`): create purchase requisitions in your ERP (SAP MM, Oracle Procurement) with line items, cost center allocation, and budget reference
- **ApproveWorker** (`prw_approve`): route approvals based on spending thresholds (e.g., manager <$5K, director <$50K, VP >$50K) via your approval engine or Slack integration
- **PurchaseWorker** (`prw_purchase`): convert approved requisitions to purchase orders in the ERP and transmit to the vendor via EDI, email, or supplier portal
- **ReceiveWorker** (`prw_receive`): confirm goods receipt against the PO in your WMS/ERP, performing three-way matching (PO, receipt, invoice)
- **PayWorker** (`prw_pay`): submit payment to AP via your ERP's payment run or payment platform (Bill.com, Tipalti), releasing funds only after goods receipt confirmation

Connect any worker to your ERP procurement module while keeping output fields consistent, and the approval-to-payment pipeline remains the same.

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
procurement-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/procurementworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ProcurementWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── PayWorker.java
│       ├── PurchaseWorker.java
│       ├── ReceiveWorker.java
│       └── RequisitionWorker.java
└── src/test/java/procurementworkflow/workers/
    ├── ApproveWorkerTest.java        # 3 tests
    ├── PayWorkerTest.java        # 2 tests
    ├── PurchaseWorkerTest.java        # 2 tests
    ├── ReceiveWorkerTest.java        # 2 tests
    └── RequisitionWorkerTest.java        # 3 tests

```
