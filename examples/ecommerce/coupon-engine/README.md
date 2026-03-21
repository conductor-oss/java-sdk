# Coupon Engine in Java Using Conductor :  Validate Code, Check Eligibility, Apply Discount, Record Usage

Coupon engine: validate code, check eligibility, apply discount, record usage. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Coupons Have Complex Validation Rules

A customer enters coupon code "SAVE20" at checkout. Before applying a 20% discount, the system must verify the code exists and is currently active (not expired, not past its redemption limit), check that the customer is eligible (hasn't used this code before, meets the minimum cart total of $50, cart contains items from eligible categories), apply the correct discount type (percentage off, fixed dollar amount, free shipping, buy-one-get-one), and record the usage atomically so the same code can't be reused.

Each validation step is independent: code validation doesn't depend on eligibility checking, but both must pass before the discount is applied. If the usage recording step fails, the discount should not be applied .  otherwise the coupon appears used but the customer didn't get the discount. And every coupon redemption needs an audit trail for financial reconciliation.

## The Solution

**You just write the code validation, eligibility checking, discount calculation, and usage recording logic. Conductor handles validation retries, eligibility routing, and usage tracking for every coupon redemption.**

`ValidateCodeWorker` checks that the coupon code exists, is within its active date range, and hasn't exceeded its redemption limit. `CheckEligibilityWorker` verifies the customer meets the coupon's requirements .  minimum cart total, eligible product categories, first-time or repeat customer restrictions, and per-customer usage limits. `ApplyDiscountWorker` calculates and applies the correct discount based on the coupon type (percentage, fixed, free shipping) and any caps (maximum discount amount). `RecordUsageWorker` atomically records the redemption to prevent reuse and updates the coupon's remaining redemption count. Conductor chains these four steps and records every redemption attempt (successful or rejected) for coupon analytics.

### What You Write: Workers

Validation, eligibility, discount calculation, and usage recording workers each handle one aspect of coupon redemption in complete isolation.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyDiscountWorker** | `cpn_apply_discount` | Applies the discount |
| **CheckEligibilityWorker** | `cpn_check_eligibility` | Performs the check eligibility operation |
| **RecordUsageWorker** | `cpn_record_usage` | Performs the record usage operation |
| **ValidateCodeWorker** | `cpn_validate_code` | Performs the validate code operation |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
cpn_validate_code
    │
    ▼
cpn_check_eligibility
    │
    ▼
cpn_apply_discount
    │
    ▼
cpn_record_usage

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
java -jar target/coupon-engine-1.0.0.jar

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
java -jar target/coupon-engine-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow coupon_engine \
  --version 1 \
  --input '{"couponCode": "sample-couponCode", "customerId": "TEST-001", "cartTotal": "sample-cartTotal", "cartItems": [{"id": "ITEM-001", "quantity": 2}]}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w coupon_engine -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real coupon infrastructure .  your promotion database for code validation, your customer service for eligibility checks, your order system for discount application, and the workflow runs identically in production.

- **ValidateCodeWorker** (`cpn_validate_code`): query a real coupon database (PostgreSQL, DynamoDB) for code lookup with active date range checking and atomic redemption count decrement
- **CheckEligibilityWorker** (`cpn_check_eligibility`): verify customer eligibility rules: first-time buyer restrictions, minimum cart total thresholds, product category limitations, and maximum redemptions per customer
- **ApplyDiscountWorker** (`cpn_apply_discount`): implement stackable coupons (multiple codes per order), tiered discounts (10% off $50+, 20% off $100+), and BOGO logic with automatic item selection
- **RecordUsageWorker** (`cpn_record_usage`): use database transactions for atomic usage recording, emit events to analytics pipelines for coupon performance tracking, and update customer profiles

Change your coupon validation rules or usage database and the pipeline structure stays intact.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
coupon-engine/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/couponengine/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CouponEngineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyDiscountWorker.java
│       ├── CheckEligibilityWorker.java
│       ├── RecordUsageWorker.java
│       └── ValidateCodeWorker.java
└── src/test/java/couponengine/workers/
    ├── ApplyDiscountWorkerTest.java        # 2 tests
    ├── CheckEligibilityWorkerTest.java        # 3 tests
    ├── RecordUsageWorkerTest.java        # 2 tests
    └── ValidateCodeWorkerTest.java        # 2 tests

```
