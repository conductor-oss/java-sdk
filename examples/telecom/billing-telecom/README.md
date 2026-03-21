# Billing Telecom in Java Using Conductor

A Java Conductor workflow example that orchestrates the telecom billing cycle. collecting usage records (voice, data, SMS) for a customer's billing period, rating each record against the customer's plan tariffs, generating an itemized invoice with the total amount, delivering the invoice to the customer, and collecting payment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Telecom Billing Needs Orchestration

Running a billing cycle requires a strict pipeline where each step depends on the previous one. You collect all usage records (CDRs, IPDRs) for the customer's billing period. You rate each record by applying the correct tariff based on the customer's plan, time of day, destination, and any bundled allowances. You generate an invoice that itemizes the rated charges and calculates the total. You send the invoice to the customer via their preferred channel. Finally, you collect payment by charging the customer's payment method on file.

If rating fails partway through, you need to know which records were already rated so you don't double-charge. If the invoice is generated but delivery fails, you have a valid invoice the customer never sees and payment never comes. Without orchestration, you'd build a monolithic billing script that mixes CDR collection, tariff lookups, invoice generation, and payment processing. making it impossible to swap rating engines, test invoice formatting independently, or audit which usage records drove which charges.

## The Solution

**You just write the usage collection, tariff rating, invoice generation, delivery, and payment processing logic. Conductor handles usage collection retries, invoice generation, and billing audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Usage collection, charge calculation, invoice generation, and payment processing workers each handle one stage of the telecom billing cycle.

| Worker | Task | What It Does |
|---|---|---|
| **CollectPaymentWorker** | `btl_collect_payment` | Collects payment for the invoice amount by charging the customer's payment method on file. |
| **CollectUsageWorker** | `btl_collect_usage` | Collects all usage records (voice CDRs, data IPDRs, SMS logs) for a customer's billing period. |
| **InvoiceWorker** | `btl_invoice` | Generates an itemized invoice from the rated charges and calculates the total amount due. |
| **RateWorker** | `btl_rate` | Rates each usage record against the customer's plan tariffs, applying time-of-day and destination rules. |
| **SendWorker** | `btl_send` | Delivers the invoice to the customer via their preferred channel (email, postal, in-app). |

Workers implement telecom operations. provisioning, activation, billing,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
btl_collect_usage
    │
    ▼
btl_rate
    │
    ▼
btl_invoice
    │
    ▼
btl_send
    │
    ▼
btl_collect_payment

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
java -jar target/billing-telecom-1.0.0.jar

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
java -jar target/billing-telecom-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow btl_billing_telecom \
  --version 1 \
  --input '{"customerId": "TEST-001", "billingPeriod": "sample-billingPeriod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w btl_billing_telecom -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real billing systems. your mediation platform for CDR collection, your rating engine for tariff application, your BSS for invoice delivery and payment collection, and the workflow runs identically in production.

- **CollectUsageWorker** (`btl_collect_usage`): query your mediation platform or CDR storage (e.g., Oracle BRM, CSG Singleview, or a data lake) for all usage records in the billing period
- **RateWorker** (`btl_rate`): apply tariffs from your rating engine (e.g., Ericsson Charging System, Huawei CBS, or Oracle BRM rating module) to each CDR/IPDR based on the subscriber's plan
- **InvoiceWorker** (`btl_invoice`): generate the itemized invoice in your billing system (Amdocs, Netcracker, CSG) with tax calculations, discounts, and pro-rations
- **SendWorker** (`btl_send`): deliver the invoice via email (SendGrid, SES), postal mail (print vendor API), or in-app notification through your customer portal
- **CollectPaymentWorker** (`btl_collect_payment`): charge the customer's payment method via your payment gateway (Stripe, Adyen) or initiate direct debit through your bank integration

Change usage collection methods or payment gateways and the billing pipeline handles them transparently.

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
billing-telecom-billing-telecom/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/billingtelecom/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BillingTelecomExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectPaymentWorker.java
│       ├── CollectUsageWorker.java
│       ├── InvoiceWorker.java
│       ├── RateWorker.java
│       └── SendWorker.java
└── src/test/java/billingtelecom/workers/
    ├── CollectUsageWorkerTest.java        # 1 tests
    └── InvoiceWorkerTest.java        # 1 tests

```
