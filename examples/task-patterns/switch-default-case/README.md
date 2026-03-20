# Switch Default Case in Java with Conductor

Fallback routing for unmatched payment methods using SWITCH with defaultCase. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to route payment processing based on the payment method .  credit card goes to Stripe, bank transfer goes to Plaid, crypto goes to Coinbase. But customers sometimes submit unrecognized payment methods (PayPal, Apple Pay, "cash") that don't match any configured processor. Those unmatched methods need a fallback path that flags them for manual review rather than silently failing or throwing an exception. After processing (or flagging), every payment attempt must be logged regardless of which branch was taken.

Without orchestration, you'd write an if/else chain or switch statement with a catch-all else clause, but there is no record of which branch executed for a given payment. If you add a new payment method (e.g., "digital_wallet"), you modify the routing code and hope the default case still works. When a customer complains that their payment failed, debugging whether it hit the crypto branch or the default case requires searching through application logs.

## The Solution

**You just write the per-method payment processing and logging workers. Conductor handles the routing, default-case fallback, and branch tracking.**

This example demonstrates Conductor's SWITCH task with a `defaultCase` for handling unmatched values. The SWITCH routes on `paymentMethod`: `credit_card` goes to ProcessCardWorker (Stripe), `bank_transfer` goes to ProcessBankWorker (Plaid), `crypto` goes to ProcessCryptoWorker (Coinbase). Any unrecognized method falls through to the `defaultCase`, where UnknownMethodWorker flags it for manual review. After the SWITCH resolves .  regardless of which branch ran. LogWorker records the payment attempt. Conductor records exactly which branch executed, so you can see that `paymentMethod=paypal` hit the default case and was flagged for review.

### What You Write: Workers

Five workers cover payment routing: ProcessCardWorker handles credit cards via Stripe, ProcessBankWorker handles bank transfers via Plaid, ProcessCryptoWorker handles crypto via Coinbase, UnknownMethodWorker flags unrecognized methods for review, and LogWorker records every payment attempt regardless of branch.

| Worker | Task | What It Does |
|---|---|---|
| **LogWorker** | `dc_log` | Logs the payment processing action. Runs after the SWITCH for all cases. |
| **ProcessBankWorker** | `dc_process_bank` | Processes bank transfer payments via Plaid. |
| **ProcessCardWorker** | `dc_process_card` | Processes credit card payments via Stripe. |
| **ProcessCryptoWorker** | `dc_process_crypto` | Processes crypto payments via Coinbase. |
| **UnknownMethodWorker** | `dc_unknown_method` | Handles unrecognized payment methods (default case). Takes the method name and flags it for manual review. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_ref)
    ├── credit_card: dc_process_card
    ├── bank_transfer: dc_process_bank
    ├── crypto: dc_process_crypto
    └── default: dc_unknown_method
    │
    ▼
dc_log
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
java -jar target/switch-default-case-1.0.0.jar
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
java -jar target/switch-default-case-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow default_case_demo \
  --version 1 \
  --input '{"paymentMethod": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w default_case_demo -s COMPLETED -c 5
```

## How to Extend

Connect the card worker to Stripe, the bank worker to Plaid, and the crypto worker to Coinbase, the SWITCH-with-defaultCase routing workflow runs unchanged.

- **ProcessCardWorker** (`dc_process_card`): charge via the Stripe API: create a PaymentIntent, confirm the charge, and return the transaction ID
- **ProcessBankWorker** (`dc_process_bank`): initiate an ACH transfer via Plaid or Dwolla, handle the multi-day settlement timeline, and return the transfer status
- **ProcessCryptoWorker** (`dc_process_crypto`): process cryptocurrency payments via Coinbase Commerce or BitPay, verify blockchain confirmations, and return the transaction hash
- **UnknownMethodWorker** (`dc_unknown_method`): create a manual review ticket in your support system (Zendesk, Jira), notify the payments team via Slack, and hold the order until reviewed
- **LogWorker** (`dc_log`): write a payment audit record to your database or event stream (Kafka, Kinesis) for compliance and analytics

Adding a new payment method as a SWITCH case or connecting to real payment processors does not affect the routing logic, since each worker just needs to return the expected processing result and status.

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
switch-default-case/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/switchdefaultcase/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SwitchDefaultCaseExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── LogWorker.java
│       ├── ProcessBankWorker.java
│       ├── ProcessCardWorker.java
│       ├── ProcessCryptoWorker.java
│       └── UnknownMethodWorker.java
└── src/test/java/switchdefaultcase/workers/
    ├── WorkflowJsonTest.java        # 19 tests
    ├── LogWorkerTest.java        # 4 tests
    ├── ProcessBankWorkerTest.java        # 4 tests
    ├── ProcessCardWorkerTest.java        # 4 tests
    ├── ProcessCryptoWorkerTest.java        # 4 tests
    └── UnknownMethodWorkerTest.java        # 4 tests
```
