# Terminate Task in Java with Conductor

Early exit with TERMINATE based on validation. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to validate an order before processing it. If the amount is invalid, the currency is unsupported, or the order exceeds limits, the workflow should stop immediately with a FAILED status and a clear error message. The TERMINATE task provides a clean early exit: the validation worker checks constraints, a SWITCH routes invalid orders to TERMINATE (which ends the workflow), and valid orders continue to the processing worker.

Without orchestration, you'd use exceptions or return codes to short-circuit processing, but there's no standard way to mark the workflow as failed with a specific reason. TERMINATE gives you a declarative early exit with proper status tracking, every terminated workflow shows exactly why it stopped.

## The Solution

**You just write the order validation and processing workers. Conductor handles the conditional routing and TERMINATE-based early exit when validation fails.**

This example validates an order before processing it, using a TERMINATE task to short-circuit the workflow when validation fails. ValidateWorker checks the order's amount (must be positive and under $1,000,000) and currency (must be USD, EUR, or GBP), returning `{ valid: true/false, reason: "..." }`. A SWITCH task inspects the validation result. If `valid` is false, the workflow routes to a TERMINATE task that ends the execution with FAILED status and the validation error message. If valid, the workflow continues to ProcessWorker, which processes the order and returns `{ processedAmount }`. Every terminated workflow shows exactly why it was rejected in Conductor's execution history.

### What You Write: Workers

Two workers support the validate-then-process pattern: ValidateWorker checks order constraints (amount range, supported currency) and returns a valid/invalid verdict, while ProcessWorker handles the order only if validation passed, the TERMINATE early exit for invalid orders is handled entirely by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessWorker** | `term_process` | Processes a validated order. Only reached if validation passed. Takes orderId and amount, returns processedAmount. |
| **ValidateWorker** | `term_validate` | Validates an order by checking amount and currency constraints. Validation rules: - Amount must be positive (greater ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
term_validate
    │
    ▼
SWITCH (check_ref)
    ├── reject: terminate_invalid
    │
    ▼
term_process

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
java -jar target/terminate-task-1.0.0.jar

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
java -jar target/terminate-task-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow terminate_demo \
  --version 1 \
  --input '{"orderId": "TEST-001", "amount": 100, "currency": "USD"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w terminate_demo -s COMPLETED -c 5

```

## How to Extend

Connect the validation to fraud-scoring APIs and the order processing to your payment gateway (Stripe, Braintree), and the TERMINATE-based early exit works unchanged.

- **ValidateWorker** (`term_validate`): add real validation rules: check the order against fraud scoring APIs, verify inventory availability, validate the customer's credit limit, or enforce region-specific regulatory constraints
- **ProcessWorker** (`term_process`): connect to your payment gateway (Stripe, Braintree) to charge the validated order, create the fulfillment record in your OMS, and return the transaction ID and receipt URL

Adding new validation rules or connecting the processor to a real payment gateway does not affect the TERMINATE-based early exit flow, since the SWITCH routes on the validator's boolean output regardless of what the workers check internally.

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
terminate-task/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/terminatetask/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TerminateTaskExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessWorker.java
│       └── ValidateWorker.java
└── src/test/java/terminatetask/workers/
    ├── ProcessWorkerTest.java        # 6 tests
    └── ValidateWorkerTest.java        # 13 tests

```
