# Idempotent Start in Java with Conductor

Idempotent start demo. demonstrates correlationId-based dedup and search-based idempotency. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to ensure that processing the same order twice does not charge the customer twice. When a webhook fires or a message is redelivered, the same orderId and amount can arrive multiple times. Starting a new workflow for each duplicate request means the order gets processed repeatedly. double charges, duplicate shipments, or inconsistent state. You need a way to guarantee that the second (and third, and fourth) request for the same order is recognized as a duplicate and returns the existing result instead of processing again.

Without orchestration, you'd build your own deduplication layer. checking a database for existing orderId records before processing, handling race conditions when two requests arrive simultaneously, and managing the gap between "check" and "insert" where duplicates can slip through. That logic is error-prone, and there is no built-in way to search for running or completed workflows by correlation ID.

## The Solution

**You just write the order processing worker. Conductor handles the deduplication, correlation tracking, and duplicate detection.**

This example demonstrates two idempotency strategies using Conductor's built-in capabilities. First, correlationId-based dedup. when starting a workflow, you pass the orderId as the correlationId, and Conductor can detect if a workflow with that correlationId is already running. Second, search-based idempotency,  before starting a new workflow, you search for existing executions with the same orderId to check if it has already been processed. The IdemProcessWorker handles the actual order processing (taking orderId and amount), and its deterministic output ensures that even if the same order is processed, the result is consistent. Conductor tracks every start attempt with its correlationId, making duplicate detection trivial.

### What You Write: Workers

A single IdemProcessWorker handles order processing, taking an orderId and amount and returning a deterministic result. Conductor's correlationId and search APIs handle the deduplication logic externally.

| Worker | Task | What It Does |
|---|---|---|
| **IdemProcessWorker** | `idem_process` | Idempotent process worker. Takes an orderId and amount, returns a deterministic result based on orderId. This worker ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
idem_process

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
java -jar target/idempotent-start-1.0.0.jar

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
java -jar target/idempotent-start-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow idempotent_demo \
  --version 1 \
  --input '{"orderId": "TEST-001", "amount": 100}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w idempotent_demo -s COMPLETED -c 5

```

## How to Extend

Replace the demo order processor with your real payment or fulfillment logic, the correlationId dedup and search-based idempotency work unchanged.

- **IdemProcessWorker** (`idem_process`): process the real order: charge the payment via Stripe/Braintree, create the order record in your database, trigger fulfillment, and return a receipt; the worker should be naturally idempotent (check-before-write) so that even if Conductor retries the task, the order is not double-processed

Replacing the demo processing with real order fulfillment does not affect the idempotency guarantees, since deduplication is managed by Conductor's correlationId tracking rather than the worker code.

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
idempotent-start/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/idempotentstart/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IdempotentStartExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── IdemProcessWorker.java
└── src/test/java/idempotentstart/workers/
    └── IdemProcessWorkerTest.java        # 9 tests

```
