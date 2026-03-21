# Checkout Flow in Java Using Conductor: Validate Cart, Calculate Tax, Process Payment, Confirm Order

Your checkout abandonment rate is 68%. Not because customers changed their minds. because your checkout calls five APIs sequentially: inventory validation, price confirmation, tax calculation, payment processing, and order creation. Any one of them timing out kills the entire flow, and the customer sees a spinner for 12 seconds before a generic error page. They refresh, get a new cart with different prices, and leave. The tax service went down for 90 seconds during a Tuesday sale and you lost $40,000 in completed carts that never converted. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate checkout steps as independent workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Checkout Must Be Reliable and Atomic

A customer clicks "Place Order" with a $230 cart. The system must verify every item is still in stock and priced correctly (prices may have changed since the item was added), calculate the correct tax for the shipping destination (sales tax varies by state, county, and city), charge the payment method, and create the order, all as a reliable sequence where a failure at any step doesn't leave the system in an inconsistent state.

If the payment processor times out after the tax calculation, you need to retry the payment. . Not recalculate the tax. If the payment succeeds but order confirmation fails, the payment must be recorded so the customer isn't charged again on retry. Every checkout needs a complete audit trail: what was in the cart, what tax was calculated, whether payment succeeded, and the final order confirmation.

## The Solution

**You just write the cart validation, tax, payment, and order confirmation logic. Conductor handles payment retries with idempotency, step sequencing, and full checkout audit trails.**

`ValidateCartWorker` verifies each item's availability, current price, and quantity limits, returning the validated cart with the confirmed subtotal. `CalculateTaxWorker` determines the applicable tax rates based on the shipping address jurisdiction and computes the tax amount. `ProcessPaymentWorker` charges the customer's payment method for the total (subtotal + tax + shipping) and returns the transaction ID. `ConfirmOrderWorker` creates the order record with an order number, itemized receipt, and estimated delivery date. Conductor chains these four steps, retries failed payment processing with idempotency keys, and records the complete checkout for accounting.

### What You Write: Workers

Four checkout workers: cart validation, tax calculation, payment processing, and order confirmation, each encapsulate one transactional step of the purchase.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateTaxWorker** | `chk_calculate_tax` | Calculates tax, shipping, and grand total based on subtotal and shipping address. |
| **ConfirmOrderWorker** | `chk_confirm_order` | Confirms the order and generates an order ID. |
| **ProcessPaymentWorker** | `chk_process_payment` | Processes payment and returns a payment ID. |
| **ValidateCartWorker** | `chk_validate_cart` | Validates the shopping cart and returns subtotal information. |

Workers implement e-commerce operations: payment processing, inventory checks, shipping, with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
chk_validate_cart
    │
    ▼
chk_calculate_tax
    │
    ▼
chk_process_payment
    │
    ▼
chk_confirm_order

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
java -jar target/checkout-flow-1.0.0.jar

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
java -jar target/checkout-flow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow checkout_flow \
  --version 1 \
  --input '{"cartId": "cart-abc123", "userId": "usr-501", "shippingAddress": {"street": "123 Main St", "city": "San Francisco", "state": "CA", "zip": "94105"}, "paymentMethod": {"type": "credit_card", "last4": "4242"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w checkout_flow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real checkout services. Stripe for payments, Avalara for tax, your inventory system for cart validation, and the workflow runs identically in production.

- **ValidateCartWorker** (`chk_validate_cart`): verify item availability against real inventory, confirm current pricing, and check for any cart-level restrictions (minimum order, shipping eligibility)
- **CalculateTaxWorker** (`chk_calculate_tax`): integrate with Avalara AvaTax, TaxJar, or Stripe Tax for real-time tax calculation with jurisdiction-level accuracy and exemption handling
- **ProcessPaymentWorker** (`chk_process_payment`): use Stripe Payment Intents or Braintree Transactions API with idempotency keys to prevent double-charging on retries
- **ConfirmOrderWorker** (`chk_confirm_order`): create orders in Shopify, WooCommerce, or a custom OMS, send confirmation emails via SendGrid, and publish order events to an event stream for downstream systems

Connect each worker to your real checkout services and the workflow runs identically in production.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
checkout-flow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/checkoutflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CheckoutFlowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateTaxWorker.java
│       ├── ConfirmOrderWorker.java
│       ├── ProcessPaymentWorker.java
│       └── ValidateCartWorker.java
└── src/test/java/checkoutflow/workers/
    ├── CalculateTaxWorkerTest.java        # 7 tests
    ├── ConfirmOrderWorkerTest.java        # 5 tests
    ├── ProcessPaymentWorkerTest.java        # 5 tests
    └── ValidateCartWorkerTest.java        # 5 tests

```
