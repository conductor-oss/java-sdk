# Stripe Integration in Java Using Conductor

A Java Conductor workflow that processes a Stripe payment end-to-end .  creating a customer in Stripe, creating a payment intent for the specified amount, confirming the charge, and sending a receipt email. Given a customer email, amount, currency, and description, the pipeline produces a Stripe customer ID, charge confirmation, and receipt delivery status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the customer-intent-charge-receipt pipeline.

## Processing Payments Through Stripe

Collecting a payment through Stripe involves a strict sequence: create a customer record (or look up an existing one), create a payment intent that captures the amount and currency, confirm the charge against the payment intent, and send a receipt to the customer. Each step depends on the previous one .  you cannot create a payment intent without a customer ID, and you cannot charge without a payment intent ID. If the charge succeeds but the receipt fails, you need visibility into exactly what happened.

Without orchestration, you would chain Stripe API calls manually, pass customer IDs and payment intent IDs between steps, and handle idempotency and partial failures (like a charge that succeeds but a receipt that fails to send). Conductor sequences the pipeline and routes customer IDs, payment intent IDs, and charge IDs between workers automatically.

## The Solution

**You just write the payment workers. Customer creation, payment intent setup, charge confirmation, and receipt delivery. Conductor handles customer-to-receipt sequencing, Stripe API retries, and idempotent payment intent routing for partial failure recovery.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers process payments: CreateCustomerWorker registers the payer, PaymentIntentWorker sets up the charge amount, ChargeWorker confirms the payment, and SendReceiptWorker delivers the receipt email.

| Worker | Task | What It Does |
|---|---|---|
| **CreateCustomerWorker** | `stp_create_customer` | Creates a Stripe customer. |
| **PaymentIntentWorker** | `stp_payment_intent` | Creates a payment intent. |
| **ChargeWorker** | `stp_charge` | Confirms and captures a payment intent. |
| **SendReceiptWorker** | `stp_send_receipt` | Sends a receipt email. |

The workers auto-detect Stripe credentials at startup. When `STRIPE_API_KEY` is set, CreateCustomerWorker, PaymentIntentWorker, and ChargeWorker use the real Stripe REST API (via `java.net.http`) to create customers, payment intents, and confirm charges. Without the key, they fall back to simulated mode with realistic output shapes so the workflow runs end-to-end without a Stripe account.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
stp_create_customer
    │
    ▼
stp_payment_intent
    │
    ▼
stp_charge
    │
    ▼
stp_send_receipt
```

## Example Output

```
=== Example 435: Stripe Integratio ===

Step 1: Registering task definitions...
  Registered: stp_create_customer, stp_payment_intent, stp_charge, stp_send_receipt

Step 2: Registering workflow 'stripe_integration_435'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [charge] Charged
  [customer] Created
  [intent] Created
  [SIMULATED][receipt] Sent receipt for

  Status: COMPLETED
  Output: {chargeId=..., capturedAt=..., customerId=..., paymentIntentId=...}

Result: PASSED
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
java -jar target/stripe-integration-1.0.0.jar
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
| `STRIPE_API_KEY` | _(none)_ | Stripe secret API key. When set, enables live Stripe API calls for customer creation, payment intents, and charge confirmation. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/stripe-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow stripe_integration_435 \
  --version 1 \
  --input '{"email": "user@example.com", "customer@example.com": "sample-customer@example.com", "amount": 250.0, "currency": "sample-currency", "usd": "sample-usd", "description": "sample-description"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w stripe_integration_435 -s COMPLETED -c 5
```

## How to Extend

CreateCustomerWorker, PaymentIntentWorker, and ChargeWorker already use the real Stripe REST API (via java.net.http) when `STRIPE_API_KEY` is provided. The remaining worker is simulated:

- **SendReceiptWorker** (`stp_send_receipt`): integrate with an email service (e.g., SendGrid) to send real receipt emails after a charge succeeds

Replace the simulation with a real email API call while keeping the same output fields, and the payment pipeline stays unchanged.

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
stripe-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/stripeintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── StripeIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChargeWorker.java
│       ├── CreateCustomerWorker.java
│       ├── PaymentIntentWorker.java
│       └── SendReceiptWorker.java
└── src/test/java/stripeintegration/workers/
    ├── ChargeWorkerTest.java        # 2 tests
    ├── CreateCustomerWorkerTest.java        # 2 tests
    ├── PaymentIntentWorkerTest.java        # 2 tests
    └── SendReceiptWorkerTest.java        # 2 tests
```
