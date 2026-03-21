# Shared Nothing Architecture in Java with Conductor

Shared nothing architecture with fully independent services. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

In a shared-nothing architecture, each service owns its data and shares nothing with other services. No shared database, no shared file system, no shared memory. Services communicate only through explicit message passing. This workflow chains three independent services where each receives only the output of the previous one, and a final aggregation step combines all results.

Without orchestration, the calling service manually chains the calls, passing data between them. If service B fails, the data from service A is lost unless you build your own checkpointing. There is no visibility into which service in the chain is the bottleneck.

## The Solution

**You just write each isolated service worker and the aggregation worker. Conductor handles inter-service data passing, per-step crash recovery, and full visibility into the processing chain.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers demonstrate complete data isolation: ServiceAWorker, ServiceBWorker, and ServiceCWorker each process data using their own private store, then AggregateWorker combines all outputs into a composite response.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `sn_aggregate` | Combines results from all three services into a final aggregated response. |
| **ServiceAWorker** | `sn_service_a` | Processes the request independently with no shared state, using its own local data store. |
| **ServiceBWorker** | `sn_service_b` | Processes service A's output independently using its own isolated data store. |
| **ServiceCWorker** | `sn_service_c` | Processes service B's output independently using its own isolated data store. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
sn_service_a
    │
    ▼
sn_service_b
    │
    ▼
sn_service_c
    │
    ▼
sn_aggregate

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
java -jar target/shared-nothing-architecture-1.0.0.jar

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
java -jar target/shared-nothing-architecture-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow shared_nothing_workflow \
  --version 1 \
  --input '{"requestId": "TEST-001", "data": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w shared_nothing_workflow -s COMPLETED -c 5

```

## How to Extend

Give each service worker its own database (Postgres, MongoDB, DynamoDB) and implement real business logic with no shared state, the service-chain-and-aggregate workflow stays exactly the same.

- **AggregateWorker** (`sn_aggregate`): apply real aggregation logic (merge, deduplicate, enrich) for the composite response
- **ServiceAWorker** (`sn_service_a`): implement real business logic with its own database (e.g., user service with its own Postgres)
- **ServiceBWorker** (`sn_service_b`): implement real business logic with its own database (e.g., order service with its own MongoDB)

Giving each service worker its own real database (Postgres, MongoDB, DynamoDB) requires no changes to the chained workflow.

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
shared-nothing-architecture/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sharednothingarchitecture/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SharedNothingArchitectureExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── ServiceAWorker.java
│       ├── ServiceBWorker.java
│       └── ServiceCWorker.java
└── src/test/java/sharednothingarchitecture/workers/
    ├── AggregateWorkerTest.java        # 2 tests
    ├── ServiceAWorkerTest.java        # 2 tests
    ├── ServiceBWorkerTest.java        # 2 tests
    └── ServiceCWorkerTest.java        # 2 tests

```
