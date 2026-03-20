# Recurring Billing in Java Using Conductor :  Scheduled Invoice Generation, Payment Processing, and Notification

A Java Conductor workflow example for recurring billing .  generating invoices on a recurring schedule, processing payments against saved payment methods, and notifying customers of successful charges or payment failures.

## The Problem

You need to bill customers on a recurring basis .  monthly subscriptions, annual renewals, usage-based charges. Each billing cycle must generate an invoice, attempt to charge the customer's saved payment method, handle payment failures (retry, update card prompt), and send a receipt or failure notification. If the invoice generation step fails, you can't charge. If the charge fails, you need to retry before suspending service.

Without orchestration, recurring billing is a cron job that queries subscribers, charges each one in a loop, and sends emails. When the payment gateway times out for one customer, the entire batch stalls. Failed charges aren't retried systematically, and there's no audit trail connecting invoices to payments to notifications.

## The Solution

**You just write the invoice generation and payment gateway integration. Conductor handles the generate-charge-notify sequence, automatic retries on payment gateway failures, and a complete audit trail linking every invoice to its payment attempt and customer notification.**

Each billing concern is an independent worker .  invoice generation, payment processing, and notification. Conductor runs them in sequence: generate the invoice, charge the payment method, then notify the customer. Failed payments are retried automatically with Conductor's retry logic. Every billing cycle is tracked with invoice details, payment status, and notification delivery. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

This workflow uses Conductor system tasks to generate invoices, process payments against saved methods, and send customer notifications, each billing step handled by built-in task types.

This example uses Conductor system tasks .  no custom workers needed.

The workflow relies on Conductor's built-in task types. To go to production, add custom workers as needed .  the worker interface is simple, and no workflow changes are required.

### The Workflow

```
Input ->  -> Output
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
java -jar target/recurring-billing-1.0.0.jar
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
java -jar target/recurring-billing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow recurring_billing \
  --version 1 \
  --input '{"customerId": "TEST-001", "planId": "TEST-001", "billingCycleStart": "test-value", "billingCycleEnd": "test-value", "subscriptionStart": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w recurring_billing -s COMPLETED -c 5
```

## How to Extend

Each worker handles one billing step .  connect the invoice generator to your billing system, the payment processor to Stripe or Braintree, the notifier to your email service, and the generate-charge-notify workflow stays the same.

Integrate with Stripe or Braintree for real payment processing, and the billing pipeline handles retries and receipts automatically.

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
recurring-billing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/recurringbilling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RecurringBillingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
```
