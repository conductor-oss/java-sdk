# Distributed Transactions in Java with Conductor

Distributed transactions with prepare-commit saga pattern. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

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

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

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

## Example Output

```
=== Example 329: Distributed Transactions ===

Step 1: Registering task definitions...
  Registered: dtx_prepare_order, dtx_prepare_payment, dtx_prepare_inventory, dtx_commit_all, dtx_rollback_all

Step 2: Registering workflow 'distributed_transaction_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [commit] All 3 transactions committed
  [inventory] Reserved items
  [order] Prepared:
  [payment] Authorized:
  [rollback] All transactions rolled back

  Status: COMPLETED
  Output: {committed=..., globalTxId=..., txId=..., prepared=...}

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
  --input '{"orderId": "ORD-700", "ORD-700": "items", "items": [{"name": "Widget A", "quantity": 2}, {"name": "Widget B", "quantity": 1}], "credit-card": "sample-credit-card"}'
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
