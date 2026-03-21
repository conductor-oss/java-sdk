# Abandoned Cart Recovery in Java Using Conductor :  Detect, Wait, Remind, Discount, Convert

Abandoned cart recovery: detect, wait, remind, offer discount, convert. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## 70% of Shopping Carts Are Abandoned

A customer adds $150 worth of items to their cart and leaves. Without intervention, that revenue is lost. Abandoned cart recovery sends a reminder email after a delay (typically 1-4 hours), and if the customer still hasn't returned, offers a discount to incentivize completion. This sequence recovers 5-15% of abandoned carts on average.

The recovery pipeline needs timed delays (wait 2 hours before sending the reminder), state tracking (did the customer return after the reminder?), and conditional logic (only offer a discount if the reminder didn't work). If the email service is temporarily unavailable, the reminder should be retried. not skipped. And every cart recovery attempt needs tracking to measure conversion rates and optimize timing.

## The Solution

**You just write the abandonment detection, reminder sending, discount offer, and conversion tracking logic. Conductor handles retry timing, recovery step sequencing, and conversion tracking across every cart.**

`DetectAbandonmentWorker` identifies the abandoned cart and captures cart details. items, total, customer ID, and abandonment timestamp. Conductor's `WAIT` task pauses the workflow for the configured delay period without consuming resources. `SendReminderWorker` sends a personalized email with the cart contents and a direct link to complete checkout. `OfferDiscountWorker` generates a time-limited discount code if the reminder hasn't driven conversion. `ConvertWorker` tracks whether the customer completed the purchase, recording the conversion method (reminder, discount, or organic return). Conductor handles the timing, retries failed email sends, and tracks conversion rates across all recovery attempts.

### What You Write: Workers

Workers for abandonment detection, reminder delivery, discount offers, and conversion tracking each focus on one recovery tactic without knowledge of the broader campaign.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertWorker** | `abc_convert` | Conversion attempt for cart |
| **DetectAbandonmentWorker** | `abc_detect_abandonment` | Detects the abandonment |
| **OfferDiscountWorker** | `abc_offer_discount` | Performs the offer discount operation |
| **SendReminderWorker** | `abc_send_reminder` | Performs the send reminder operation |

Workers implement e-commerce operations. payment processing, inventory checks, shipping,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
abc_detect_abandonment
    │
    ▼
abc_wait_period [WAIT]
    │
    ▼
abc_send_reminder
    │
    ▼
abc_offer_discount
    │
    ▼
abc_convert

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
java -jar target/abandoned-cart-1.0.0.jar

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
java -jar target/abandoned-cart-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow abandoned_cart_workflow \
  --version 1 \
  --input '{"cartId": "TEST-001", "customerId": "TEST-001", "cartTotal": "sample-cartTotal"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w abandoned_cart_workflow -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real marketing tools. your analytics platform for abandonment detection, SendGrid for reminder emails, your coupon system for discount codes, and the workflow runs identically in production.

- **DetectAbandonmentWorker** (`abc_detect_abandonment`): listen to real cart events from Shopify webhooks, WooCommerce REST API, or custom event streams to trigger recovery workflows automatically
- **SendReminderWorker** (`abc_send_reminder`): integrate with SendGrid, Amazon SES, or Mailchimp for templated cart abandonment emails with product images, prices, and one-click checkout links
- **OfferDiscountWorker** (`abc_offer_discount`): generate unique discount codes via Stripe Coupons API or Shopify Discount API with configurable percentage, expiration, and single-use restrictions
- **ConvertWorker** (`abc_convert`): check order completion status via the ecommerce platform API, record whether the customer converted via reminder, discount, or organic return for funnel analysis

Switch your email provider or discount engine and the recovery workflow adapts with no structural changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
abandoned-cart/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/abandonedcart/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AbandonedCartExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConvertWorker.java
│       ├── DetectAbandonmentWorker.java
│       ├── OfferDiscountWorker.java
│       └── SendReminderWorker.java
└── src/test/java/abandonedcart/workers/
    ├── ConvertWorkerTest.java        # 2 tests
    ├── DetectAbandonmentWorkerTest.java        # 2 tests
    └── OfferDiscountWorkerTest.java        # 3 tests

```
