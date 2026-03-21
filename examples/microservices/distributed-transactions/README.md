# Distributed Transactions in Java with Conductor

Distributed transactions with prepare-commit saga pattern. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

An e-commerce order touches three services: order, payment, and inventory, each with its own database. All three must succeed or none should commit, but there is no single database transaction that spans them. This workflow uses a prepare-commit saga: all services prepare (reserve resources) in parallel, and only after all succeed does a commit step finalize them.

Without orchestration, distributed transactions are implemented with ad-hoc compensation logic scattered across services. If the payment prepare succeeds but the inventory prepare fails, the payment must be manually reversed, and forgetting a compensation path leads to data inconsistency.

## The Solution

**You just write the order-prepare, payment-prepare, inventory-prepare, and commit workers. Conductor handles parallel prepare execution, atomic commit-or-rollback routing, and durable transaction state.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Five workers implement a prepare-commit saga: PrepareOrderWorker, PreparePaymentWorker, and PrepareInventoryWorker each reserve resources in parallel, then CommitAllWorker finalizes or RollbackAllWorker compensates.

| Worker | Task | What It Does |
|---|---|---|
| **CommitAllWorker** | `dtx_commit_all` | Commits all three prepared transactions atomically using their transaction IDs. |
| **PrepareInventoryWorker** | `dtx_prepare_inventory` | Reserves the ordered items in the inventory service and returns a transaction ID. |
| **PrepareOrderWorker** | `dtx_prepare_order` | Prepares the order by reserving it in the order service and returns a transaction ID. |
| **PreparePaymentWorker** | `dtx_prepare_payment` | Authorizes the payment method and holds the charge, returning a transaction ID. |
| **RollbackAllWorker** | `dtx_rollback_all` | Rolls back all prepared transactions if any prepare step failed. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
    ├── dtx_prepare_order
    ├── dtx_prepare_payment
    └── dtx_prepare_inventory
    │
    ▼
JOIN (wait for all branches)
dtx_commit_all

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
java -jar target/distributed-transactions-1.0.0.jar

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
java -jar target/distributed-transactions-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow distributed_transaction_workflow \
  --version 1 \
  --input '{"orderId": "TEST-001", "items": [{"id": "ITEM-001", "quantity": 2}], "paymentMethod": "sample-paymentMethod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w distributed_transaction_workflow -s COMPLETED -c 5

```

## How to Extend

Connect each prepare worker to your real order database, payment gateway (Stripe, Adyen), and inventory system, the parallel-prepare-then-commit saga workflow stays exactly the same.

- **CommitAllWorker** (`dtx_commit_all`): commit each prepared transaction by finalizing the order, capturing the payment, and confirming the inventory reservation
- **PrepareInventoryWorker** (`dtx_prepare_inventory`): reserve stock in your inventory system (ERP, warehouse management API)
- **PrepareOrderWorker** (`dtx_prepare_order`): create a pending order record in your order database with a distributed transaction ID

Connecting the prepare workers to real databases and payment gateways preserves the parallel-prepare-then-commit saga.

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
distributed-transactions/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/distributedtransactions/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DistributedTransactionsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CommitAllWorker.java
│       ├── PrepareInventoryWorker.java
│       ├── PrepareOrderWorker.java
│       ├── PreparePaymentWorker.java
│       └── RollbackAllWorker.java
└── src/test/java/distributedtransactions/workers/
    ├── CommitAllWorkerTest.java        # 2 tests
    ├── PrepareInventoryWorkerTest.java        # 2 tests
    ├── PrepareOrderWorkerTest.java        # 2 tests
    ├── PreparePaymentWorkerTest.java        # 2 tests
    └── RollbackAllWorkerTest.java        # 2 tests

```
