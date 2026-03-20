# Sub-Workflows in Java with Conductor

SUB_WORKFLOW demo: an order processing workflow that delegates payment handling to a reusable child workflow. The parent calculates the order total, invokes a payment sub-workflow (validate + charge), then confirms the order with the returned transaction ID. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to process an order through three phases: calculate the order total from line items (summing price * quantity for each item), process the payment (validate payment details, then charge the card), and confirm the order. Payment processing is a reusable multi-step process: validate the payment method and amount, then charge it, that should be a self-contained unit you can invoke from any order workflow, refund workflow, or subscription workflow. Embedding payment logic directly in the order workflow makes it impossible to reuse, test, or version independently.

Without orchestration, you'd call payment functions directly from the order code, tightly coupling order processing to payment logic. Changing the payment flow (adding fraud checks, supporting new payment methods) requires modifying the order workflow. Testing payment logic means running the full order flow. If the charge fails after validation succeeds, there is no clean boundary for retrying just the payment portion.

## The Solution

**You just write the order calculation, payment validation, charge, and confirmation workers. Conductor handles the parent-child workflow composition via SUB_WORKFLOW.**

This example demonstrates Conductor's SUB_WORKFLOW task for composable workflow design. The parent `sub_order_workflow` calculates the order total via CalcTotalWorker (summing price * qty for each line item), then delegates payment processing to a `sub_payment_process` child workflow via SUB_WORKFLOW task, passing the orderId, computed total, and payment method. The child workflow runs ValidatePaymentWorker (checks method is present and amount is positive) then ChargePaymentWorker (processes the charge and returns a deterministic transactionId of "TXN-" + orderId). After the sub-workflow completes, the parent runs ConfirmOrderWorker with the transactionId from the payment result, returning the final order confirmation. The payment sub-workflow can be versioned, tested, and invoked independently, from refund flows, subscription renewals, or any other workflow that needs payment processing.

### What You Write: Workers

Four workers span the parent and child workflows: CalcTotalWorker computes the order total in the parent, ValidatePaymentWorker and ChargePaymentWorker run inside the reusable payment sub-workflow, and ConfirmOrderWorker finalizes the order with the transaction ID returned from the child.

| Worker | Task | What It Does |
|---|---|---|
| **CalcTotalWorker** | `sub_calc_total` | Calculates the order total from a list of items. Each item has name, price, and qty. Returns total = sum of (price * qty) and itemCount. Returns 0.0/0 for null or empty items. |
| **ValidatePaymentWorker** | `sub_validate_payment` | Validates payment details: checks that paymentMethod is present and non-blank, and that amount is present and positive. Returns valid=true/false with a descriptive reason string. |
| **ChargePaymentWorker** | `sub_charge_payment` | Charges payment and returns a deterministic transactionId: "TXN-" + orderId. Returns charged=true and the amount. Defaults orderId to "UNKNOWN" if missing/blank. |
| **ConfirmOrderWorker** | `sub_confirm_order` | Confirms the order after payment: returns the orderId, transactionId, and confirmed=true. Defaults orderId to "UNKNOWN" and transactionId to "NONE" if missing/blank. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Workflow composition** | SUB_WORKFLOW nests workflows inside workflows for modular, reusable design |

### The Workflow

```
Parent: sub_order_workflow
    sub_calc_total
        │
        ▼
    SUB_WORKFLOW (sub_payment_process)
        │   ├── sub_validate_payment
        │   └── sub_charge_payment
        ▼
    sub_confirm_order
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
java -jar target/sub-workflows-1.0.0.jar
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
java -jar target/sub-workflows-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sub_order_workflow \
  --version 1 \
  --input '{"orderId": "ORD-042", "items": [{"name": "Widget", "price": 10.0, "qty": 2}, {"name": "Gadget", "price": 25.0, "qty": 1}], "paymentMethod": "credit_card"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sub_order_workflow -s COMPLETED -c 5
```

## Example Output

```
=== Sub-Workflow Pattern: Order Processing with Payment Sub-Workflow ===

Step 1: Registering task definitions...
  Registered: sub_calc_total, sub_validate_payment, sub_charge_payment, sub_confirm_order

Step 2: Registering workflows...
  Registered child: sub_payment_process
  Registered parent: sub_order_workflow

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...

  Workflow ID: 5a6b7c8d-...

Step 5: Waiting for completion...
  [sub_calc_total] Calculated total: 65.0 from 3 items
  [sub_validate_payment] Payment validated (method=credit_card, amount=65.0)
  [sub_charge_payment] Charged 65.0 via credit_card -> TXN-ORD-001
  [sub_confirm_order] Order ORD-001 confirmed with transaction TXN-ORD-001
  Status: COMPLETED
  Output: {orderId=ORD-001, transactionId=TXN-ORD-001, confirmed=true}

Result: PASSED
```

## How to Extend

Connect the order and payment workers to your product catalog, payment gateway (Stripe, Braintree), and OMS, and the SUB_WORKFLOW composition works unchanged.

- **CalcTotalWorker** (`sub_calc_total`): compute the real order total from line items in your product catalog, applying discounts, taxes, and shipping costs
- **ValidatePaymentWorker** (`sub_validate_payment`): validate the payment method against your payment gateway (Stripe, Braintree), check for expired cards, and verify the billing address
- **ChargePaymentWorker** (`sub_charge_payment`): process the actual charge via your payment gateway, capture the authorization, and return the transaction ID and receipt
- **ConfirmOrderWorker** (`sub_confirm_order`): create the order record in your OMS, send a confirmation email, and trigger fulfillment
- **Reuse the payment sub-workflow**: invoke `sub_payment_process` from a subscription renewal workflow or refund flow; version it independently to add fraud checks without touching the order workflow

Connecting the payment workers to Stripe or Braintree and reusing the payment sub-workflow from other flows (refunds, subscriptions) requires no changes to the parent-child composition, since each worker just returns its expected result fields.

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
sub-workflows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   ├── workflow.json                # Parent workflow (sub_order_workflow)
│   └── payment-workflow.json        # Child workflow (sub_payment_process)
├── src/main/java/subworkflows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SubWorkflowsExample.java     # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalcTotalWorker.java
│       ├── ChargePaymentWorker.java
│       ├── ConfirmOrderWorker.java
│       └── ValidatePaymentWorker.java
└── src/test/java/subworkflows/workers/
    ├── CalcTotalWorkerTest.java      # 5 tests
    ├── ChargePaymentWorkerTest.java  # 5 tests
    ├── ConfirmOrderWorkerTest.java   # 5 tests
    └── ValidatePaymentWorkerTest.java # 6 tests
```
