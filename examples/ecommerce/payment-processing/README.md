# Payment Processing in Java Using Conductor: Validate, Authorize, Capture, Receipt, Reconcile

A customer pays $259.97 for their order. The payment gateway charges the card successfully, but the confirmation response times out on the network hop back. Your system assumes the charge failed and retries. Now the customer is double-charged, their bank shows two pending holds, and they file a chargeback before your support team even sees the ticket. The second charge eventually settles, the chargeback reverses the first, and your reconciliation report is off by $259.97 for a month until someone manually traces the duplicate. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full payment lifecycle as independent workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Payments Have a Two-Phase Lifecycle: Authorize Then Capture

A $150 payment is not a single operation: it's a lifecycle. First, validate the payment method (card not expired, billing address matches, card not blacklisted). Then authorize (put a hold on the customer's funds without actually charging). Then capture (convert the hold into a real charge, typically done when the order ships). Then generate a receipt for the customer. Finally, reconcile the transaction with the merchant's bank account to ensure the funds actually arrive.

Authorize-then-capture is standard because you don't want to charge until you're sure you can fulfill. If fulfillment fails, you release the authorization instead of processing a refund. If capture fails after authorization, you need to retry capture. . Not re-authorize (which would create a second hold). The reconciliation step catches discrepancies between expected and actual settlement amounts.

## The Solution

**You just write the payment validation, authorization, capture, receipt, and reconciliation logic. Conductor handles authorization retries, settlement sequencing, and transaction audit trails for every payment.**

`ValidateWorker` checks the payment method. Card expiration, billing address verification (AVS), CVV match, and fraud screening. `AuthorizeWorker` places a hold on the customer's funds for the order amount without charging. `CaptureWorker` converts the authorization into a charge, transferring the funds. `ReceiptWorker` generates an itemized receipt with transaction ID, payment method details, and tax breakdown. `ReconcileWorker` matches the captured amount against the merchant settlement to detect discrepancies. Conductor sequences these five stages, ensures idempotent retries for capture, and records the complete payment lifecycle for financial audit.

### What You Write: Workers

Payment workers isolate authorization, capture, settlement, and notification into separate steps, so retry logic targets only the failed transaction phase.

| Worker | Task | What It Does |
|---|---|---|
| **ValidatePaymentWorker** | `pay_validate` | Validates payment method, amount, currency, and computes fraud score |
| **AuthorizePaymentWorker** | `pay_authorize` | Creates a Stripe PaymentIntent with manual capture (authorize only) |
| **CapturePaymentWorker** | `pay_capture` | Captures a previously authorized PaymentIntent |
| **ReceiptWorker** | `pay_receipt` | Generates a receipt with SHA-256 ID, retrieves charge details from Stripe |
| **ReconcileWorker** | `pay_reconcile` | Reconciles captured amount against Stripe, computes fees and net amount |

All workers run in mock mode by default when `STRIPE_API_KEY` is not set, producing realistic deterministic output. Set `STRIPE_API_KEY=sk_test_...` to use the real Stripe API. The workflow stays the same either way.

### The Workflow

```
pay_validate
    │
    ▼
pay_authorize
    │
    ▼
pay_capture
    │
    ▼
pay_receipt
    │
    ▼
pay_reconcile

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
java -jar target/payment-processing-1.0.0.jar

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
| `STRIPE_API_KEY` | _(none, mock mode)_ | Stripe API key. If unset, runs in mock mode with demo validation. Set to `sk_test_...` for real Stripe validation |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/payment-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow payment_processing \
  --version 1 \
  --input '{"orderId": "ORD-8801", "amount": 259.97, "currency": "USD", "paymentMethod": {"type": "credit_card", "brand": "visa", "last4": "1234"}, "merchantId": "merch-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w payment_processing -s COMPLETED -c 5

```

## How to Extend

Replace each worker with your real payment stack. Stripe Payment Intents for auth and capture, your receipt service for invoicing, your ledger for reconciliation, and the workflow runs identically in production.

- **AuthorizeWorker/CaptureWorker** (`pay_authorize/capture`): integrate with Stripe Payment Intents (separate authorize and capture), Braintree, or Adyen for real two-phase payment processing with 3D Secure support
- **ReconcileWorker** (`pay_reconcile`): match captured transactions against Stripe payouts, bank settlement files (BAI2 format), or accounting system entries in QuickBooks/Xero
- **ReceiptWorker** (`pay_receipt`): generate PDF receipts using Apache PDFBox, send via SendGrid with dynamic templates, and store in S3 for customer access and compliance retention

Swap payment gateways or add new settlement logic and the processing pipeline remains stable.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
payment-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/paymentprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PaymentProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuthorizePaymentWorker.java
│       ├── CapturePaymentWorker.java
│       ├── ReceiptWorker.java
│       ├── ReconcileWorker.java
│       └── ValidatePaymentWorker.java
└── src/test/java/paymentprocessing/workers/
    ├── AuthorizePaymentWorkerTest.java        # 4 tests
    ├── CapturePaymentWorkerTest.java        # 3 tests
    ├── ReceiptWorkerTest.java        # 3 tests
    ├── ReconcileWorkerTest.java        # 3 tests
    └── ValidatePaymentWorkerTest.java        # 4 tests

```
