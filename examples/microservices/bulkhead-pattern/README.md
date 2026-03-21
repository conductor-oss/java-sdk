# Bulkhead Pattern in Java with Conductor

It's Tuesday at 3 AM. The recommendation service starts returning 504s because a third-party ML endpoint is hanging. No big deal. except every request to the recommendation service is holding a thread from the shared pool. Within two minutes, the thread pool is exhausted. Now the payment service, the user service, and the search service can't get threads either. Customers can't check out, can't log in, can't search. Your entire platform is down because a non-critical recommendation feature got slow. This is the noisy-neighbor problem: without resource isolation, one misbehaving service starves everything else. This workflow implements the bulkhead pattern, classifying requests into isolated resource pools so a failure in one service never consumes resources needed by others. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

When multiple microservices share the same thread pool, a slow or failing service can exhaust all available threads and starve healthy services. The bulkhead pattern isolates each service into its own resource pool, so a failure in one service cannot consume resources needed by others.

Without orchestration, implementing bulkhead isolation means managing thread pools, semaphores, or connection limits in application code, with no centralized view of pool utilization. Tuning pool sizes requires redeployments, and there is no audit trail of which requests were throttled.

## The Solution

**You just write the request-classification, pool-allocation, and execution workers. Conductor handles pool-aware task ordering, per-request retries, and full visibility into slot utilization.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

The bulkhead pipeline uses four workers: ClassifyRequestWorker assigns incoming requests to resource pools, AllocatePoolWorker reserves a slot, ExecuteRequestWorker runs the bounded operation, and ReleasePoolWorker frees the slot.

| Worker | Task | What It Does |
|---|---|---|
| **AllocatePoolWorker** | `bh_allocate_pool` | Allocates a slot in the specified resource pool. |
| **ClassifyRequestWorker** | `bh_classify_request` | Classifies an incoming request into a resource pool based on priority. |
| **ExecuteRequestWorker** | `bh_execute_request` | Executes the request within the allocated pool. |
| **ReleasePoolWorker** | `bh_release_pool` | Releases the pool slot after request execution. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients, the workflow coordination stays the same.

### The Workflow

```
bh_classify_request
    │
    ▼
bh_allocate_pool
    │
    ▼
bh_execute_request
    │
    ▼
bh_release_pool

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
java -jar target/bulkhead-pattern-1.0.0.jar

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
java -jar target/bulkhead-pattern-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bulkhead_pattern_workflow \
  --version 1 \
  --input '{"serviceName": "payment-service", "priority": "high", "request": {"action": "charge"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bulkhead_pattern_workflow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real tenant registry, Redis or ZooKeeper semaphore, and downstream services, the bulkhead isolation workflow stays exactly the same.

- **AllocatePoolWorker** (`bh_allocate_pool`): integrate with a distributed semaphore (Redis, ZooKeeper) to enforce cross-instance concurrency limits
- **ClassifyRequestWorker** (`bh_classify_request`): look up tenant tier and priority from your tenant-management service or database
- **ExecuteRequestWorker** (`bh_execute_request`): make the real downstream service call within the bounded pool

Switching from a simulated semaphore to a Redis-backed distributed one changes nothing in the isolation workflow.

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
bulkhead-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bulkheadpattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BulkheadPatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AllocatePoolWorker.java
│       ├── ClassifyRequestWorker.java
│       ├── ExecuteRequestWorker.java
│       └── ReleasePoolWorker.java
└── src/test/java/bulkheadpattern/workers/
    ├── AllocatePoolWorkerTest.java        # 8 tests
    ├── ClassifyRequestWorkerTest.java        # 9 tests
    ├── ExecuteRequestWorkerTest.java        # 8 tests
    └── ReleasePoolWorkerTest.java        # 8 tests

```
