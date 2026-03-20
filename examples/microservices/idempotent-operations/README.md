# Idempotent Operations in Java with Conductor

Idempotent operations with duplicate detection. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

In a distributed system, duplicate requests are inevitable (network retries, user double-clicks). Each operation must be idempotent. Executing it twice must produce the same result. This workflow generates a deterministic idempotency key, checks whether the operation was already executed, and either skips execution (duplicate) or executes and records the completion.

Without orchestration, idempotency checks are sprinkled throughout business logic with ad-hoc Redis or database lookups. Missing a check on one endpoint leads to duplicate charges, double inventory deductions, or repeated notifications.

## The Solution

**You just write the key-generation, duplicate-check, execution, and completion-recording workers. Conductor handles conditional skip-or-execute routing, guaranteed completion recording, and full visibility into duplicate detection.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers enforce exactly-once semantics: GenerateKeyWorker computes a deterministic idempotency key, CheckDuplicateWorker queries the dedup store, ExecuteWorker performs the operation if not a duplicate, and RecordCompletionWorker persists the result.

| Worker | Task | What It Does |
|---|---|---|
| **CheckDuplicateWorker** | `io_check_duplicate` | Checks the idempotency store to determine whether this key was already processed. |
| **ExecuteWorker** | `io_execute` | Executes the operation (only if not a duplicate) and returns the result. |
| **GenerateKeyWorker** | `io_generate_key` | Generates a deterministic idempotency key from the operation ID and action name. |
| **RecordCompletionWorker** | `io_record_completion` | Records the idempotency key and result in the store to prevent future duplicates. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
io_generate_key
    │
    ▼
io_check_duplicate
    │
    ▼
SWITCH (decision_ref)
    ├── true: 
    └── default: io_execute -> io_record_completion
```

## Example Output

```
=== Example 327: Idempotent Operations ===

Step 1: Registering task definitions...
  Registered: io_generate_key, io_check_duplicate, io_execute, io_record_completion

Step 2: Registering workflow 'idempotent_operations_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [check] Key
  [execute]
  [key] Generated idempotency key:
  [record] Stored completion for

  Status: COMPLETED
  Output: {isDuplicate=..., result=..., key=..., recorded=...}

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
java -jar target/idempotent-operations-1.0.0.jar
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
java -jar target/idempotent-operations-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow idempotent_operations_workflow \
  --version 1 \
  --input '{"operationId": "OP-123", "OP-123": "action", "action": "charge-payment", "charge-payment": "data", "data": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w idempotent_operations_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real idempotency store (Redis, DynamoDB) and the business operation that needs dedup protection, the key-generate-check-execute-record workflow stays exactly the same.

- **CheckDuplicateWorker** (`io_check_duplicate`): look up the idempotency key in Redis, DynamoDB, or a database unique-constraint table
- **ExecuteWorker** (`io_execute`): perform the real business operation (charge payment, reserve inventory, send email)
- **GenerateKeyWorker** (`io_generate_key`): compute a deterministic key using a hash of operation ID, action, and payload to ensure stable idempotency keys

Switching the idempotency store from in-memory to Redis or DynamoDB does not alter the key-check-execute-record flow.

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
idempotent-operations/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/idempotentoperations/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IdempotentOperationsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckDuplicateWorker.java
│       ├── ExecuteWorker.java
│       ├── GenerateKeyWorker.java
│       └── RecordCompletionWorker.java
└── src/test/java/idempotentoperations/workers/
    ├── CheckDuplicateWorkerTest.java        # 2 tests
    ├── ExecuteWorkerTest.java        # 2 tests
    ├── GenerateKeyWorkerTest.java        # 2 tests
    └── RecordCompletionWorkerTest.java        # 2 tests
```
