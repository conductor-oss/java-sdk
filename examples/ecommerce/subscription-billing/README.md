# Subscription Billing in Java Using Conductor :  Check Period, Generate Invoice, Charge, Activate

A Java Conductor workflow example for recurring subscription billing .  determining the current billing period, generating an invoice for the subscriber's plan, charging their payment method, and activating the next billing cycle. Uses [Conductor](https://github.

## Subscription Billing Must Be Reliable and Retry-Safe

A customer on the $29/month Pro plan has their billing date today. The system must verify the billing period (not already billed, subscription is active, no pending cancellation), generate an invoice (pro-rated amounts for mid-cycle changes, usage-based add-ons, applicable taxes), charge the payment method (with retry logic for declined cards), and activate the next period (extending access, resetting usage counters).

Failed charges are the biggest challenge .  a card declines due to insufficient funds, but the customer adds money the next day. The billing system needs grace period handling: retry the charge daily for 3-7 days before suspending the subscription. If the charge succeeds on retry, the subscription should activate seamlessly as if nothing happened. And every billing event needs an audit trail for revenue recognition and dispute resolution.

## The Solution

**You just write the period verification, invoice generation, payment charging, and subscription activation logic. Conductor handles payment retries, billing cycle sequencing, and subscription audit trails.**

`CheckPeriodWorker` verifies the billing period .  confirming the subscription is active, checking for pending cancellations or plan changes, and determining the billing amount including any proration. `GenerateInvoiceWorker` creates an itemized invoice with the plan cost, any usage-based charges, applicable taxes, and credits. `ChargeWorker` processes the payment against the customer's payment method, handling declined cards with configurable retry logic. `ActivateWorker` extends the subscription to the next period, resets usage counters, and confirms the billing cycle. Conductor sequences these steps, retries failed charges with backoff, and records every billing event.

### What You Write: Workers

Billing workers for metering, invoice generation, payment collection, and renewal each handle one billing cycle phase autonomously.

| Worker | Task | What It Does |
|---|---|---|
| **CheckPeriodWorker** | `sub_check_period` | Determines the current billing period start/end dates based on the subscription's billing cycle |
| **GenerateInvoiceWorker** | `sub_generate_invoice` | Creates an itemized invoice for the subscriber's plan and billing period |
| **ChargeWorker** | `sub_charge` | Charges the customer's payment method for the invoiced amount |
| **ActivateWorker** | `sub_activate` | Activates the subscription for the next billing period after successful payment |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
sub_check_period
    │
    ▼
sub_generate_invoice
    │
    ▼
sub_charge
    │
    ▼
sub_activate

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
java -jar target/subscription-billing-1.0.0.jar

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
java -jar target/subscription-billing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow subscription_billing \
  --version 1 \
  --input '{"subscriptionId": "TEST-001", "customerId": "TEST-001", "plan": "pro", "billingCycle": "sample-billingCycle"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w subscription_billing -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real billing services. Stripe Subscriptions for charges, your invoicing system for PDF generation, your entitlements API for activation, and the workflow runs identically in production.

- **ChargeWorker** (`sub_charge`): integrate with Stripe Subscriptions, Braintree Recurring Billing, or Recurly for real subscription charge processing with smart retry (Stripe's Smart Retries)
- **GenerateInvoiceWorker** (`sub_generate_invoice`): generate PDF invoices using Apache PDFBox, store in S3 for customer portal access, and calculate proration for mid-cycle plan changes
- **ActivateWorker** (`sub_activate`): update entitlements in your access control system, reset API usage counters, and trigger renewal confirmation emails via Mailchimp or SendGrid

Change metering granularity or payment processors and the billing cycle continues without pipeline modifications.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
subscription-billing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/subscriptionbilling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SubscriptionBillingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateWorker.java
│       ├── ChargeWorker.java
│       ├── CheckPeriodWorker.java
│       └── GenerateInvoiceWorker.java
└── src/test/java/subscriptionbilling/workers/
    ├── ActivateWorkerTest.java        # 2 tests
    ├── ChargeWorkerTest.java        # 2 tests
    ├── CheckPeriodWorkerTest.java        # 2 tests
    └── GenerateInvoiceWorkerTest.java        # 3 tests

```
