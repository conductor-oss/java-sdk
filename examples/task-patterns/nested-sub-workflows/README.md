# Nested Sub Workflows in Java with Conductor

Three-level nested order processing .  order fulfillment (Level 1) delegates to a payment sub-workflow (Level 2), which delegates fraud checking to its own sub-workflow (Level 3). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to process an order through a three-level hierarchy: the order workflow (Level 1) calls a payment sub-workflow (Level 2), which itself calls a fraud check sub-workflow (Level 3). The order workflow takes orderId, amount, email, and items. It delegates payment processing to a separate reusable workflow that handles charging and fraud detection. The payment workflow in turn delegates fraud screening to its own sub-workflow that computes a risk score. After payment completes (with fraud check nested inside), the order workflow fulfills the order. Each level is a self-contained, independently testable, and reusable workflow.

Without orchestration, you'd call payment functions directly from order code, nesting fraud checks inside payment logic, creating tight coupling across all three layers. If the fraud check API changes, you modify code deep inside the order processor. There is no way to reuse the payment workflow from a different order flow, and there is no visibility into which level of the hierarchy failed when something goes wrong.

## The Solution

**You just write the fraud check, charge, and fulfillment workers. Conductor handles the three-level sub-workflow nesting, execution tracking, and independent retry at each level.**

This example demonstrates Conductor's SUB_WORKFLOW tasks nested three levels deep. The root `nested_order` workflow (Level 1) invokes a `nested_payment` sub-workflow (Level 2) via SUB_WORKFLOW task, passing orderId, amount, and email. The payment workflow invokes a `nested_fraud` sub-workflow (Level 3) for risk scoring via CheckFraudWorker, then runs ChargeWorker to process the payment. After the payment sub-workflow returns a transactionId, the root workflow runs FulfillWorker to complete the order. Each level is a standalone workflow definition .  you can run the payment workflow independently for refunds, or reuse the fraud workflow from a different payment flow. Conductor tracks the full execution tree across all three levels.

### What You Write: Workers

Three workers handle the three-level order flow: CheckFraudWorker computes a risk score at Level 3, ChargeWorker processes the payment at Level 2, and FulfillWorker completes the order at Level 1, each running within its own reusable sub-workflow.

| Worker | Task | What It Does |
|---|---|---|
| **ChargeWorker** | `nest_charge` | Charges payment for an order. Returns a transaction ID and charged status. Used by Level 2 sub-workflow (nested_payme... |
| **CheckFraudWorker** | `nest_check_fraud` | Checks fraud risk for a transaction. Returns a deterministic risk score (25) and approval status. Used by the deepest... |
| **FulfillWorker** | `nest_fulfill` | Fulfills an order after payment is complete. Returns fulfillment status. Used by the root workflow (Level 1: nested_o... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> ChargeWorker -> CheckFraudWorker -> FulfillWorker -> Output
```

## Example Output

```
=== Nested Sub-Workflows Demo: Order -> Payment -> Fraud Check ===

Step 1: Registering task definitions...
  Registered: nest_check_fraud, nest_charge, nest_fulfill

Step 2: Registering workflow 'nested_order'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [nest_charge] Charging $
  [nest_check_fraud] Checking fraud for email=
  [nest_fulfill] Fulfilling order

  Status: COMPLETED
  Output: {transactionId=..., charged=..., riskScore=..., approved=...}

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
java -jar target/nested-sub-workflows-1.0.0.jar
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
java -jar target/nested-sub-workflows-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nested_sub_workflows \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nested_sub_workflows -s COMPLETED -c 5
```

## How to Extend

Connect the charge worker to Stripe or your payment gateway, integrate a real fraud scoring API, and the three-level nested workflow runs unchanged with each level independently reusable.

- **CheckFraudWorker** (`nest_check_fraud`): call a real fraud detection service (Sift, Stripe Radar, Signifyd) with the transaction amount and email, return the risk score and approval/decline decision
- **ChargeWorker** (`nest_charge`): process the payment via Stripe, Braintree, or Adyen, returning the transaction ID, charge status, and receipt URL
- **FulfillWorker** (`nest_fulfill`): trigger order fulfillment in your warehouse management system, create a shipment record, and send a confirmation email to the customer

Connecting the fraud check to a real risk engine or the charge worker to a payment processor does not affect the three-level sub-workflow composition, as long as each worker returns its expected result fields.

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
nested-sub-workflows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/nestedsubworkflows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NestedSubWorkflowsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChargeWorker.java
│       ├── CheckFraudWorker.java
│       └── FulfillWorker.java
└── src/test/java/nestedsubworkflows/workers/
    ├── ChargeWorkerTest.java        # 10 tests
    ├── CheckFraudWorkerTest.java        # 10 tests
    └── FulfillWorkerTest.java        # 7 tests
```
