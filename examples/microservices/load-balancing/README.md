# Load Balancing in Java with Conductor

Distribute requests across service instances in parallel. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

Processing a large batch of requests efficiently requires distributing the work across multiple service instances in parallel, collecting results from each instance, and aggregating them into a single response. The work is partitioned so each instance handles a subset, and the aggregation waits for all partitions.

Without orchestration, fan-out/fan-in patterns are implemented with manual thread management, CompletableFuture chains, and custom aggregation logic. Handling a failed partition (retrying just that instance) is complex, and there is no visibility into which partition is slow.

## The Solution

**You just write the instance-call and result-aggregation workers. Conductor handles parallel partition dispatch, per-instance retry on failure, and automatic join before aggregation.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Two worker types implement fan-out/fan-in: CallInstanceWorker processes a partition on a specific service instance, and AggregateResultsWorker merges all partition results into a single response.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateResultsWorker** | `lb_aggregate_results` | Aggregates results from all parallel instance calls. |
| **CallInstanceWorker** | `lb_call_instance` | Processes a partition of a batch on a specific instance. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
    ├── lb_call_instance
    ├── lb_call_instance
    └── lb_call_instance
    │
    ▼
JOIN (wait for all branches)
lb_aggregate_results
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
java -jar target/load-balancing-1.0.0.jar
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
java -jar target/load-balancing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow load_balancing_294 \
  --version 1 \
  --input '{"requestBatch": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w load_balancing_294 -s COMPLETED -c 5
```

## How to Extend

Point each instance worker at your real service endpoints and wire the aggregation worker to your result-merge logic, the fan-out/fan-in workflow stays exactly the same.

- **AggregateResultsWorker** (`lb_aggregate_results`): apply real aggregation logic (merge sorted results, deduplicate, compute statistics)
- **CallInstanceWorker** (`lb_call_instance`): make real HTTP calls to your service instances using their actual host:port addresses

Changing the partitioning strategy or pointing to real service instances leaves the fan-out/fan-in workflow unchanged.

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
load-balancing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/loadbalancing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LoadBalancingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateResultsWorker.java
│       └── CallInstanceWorker.java
└── src/test/java/loadbalancing/workers/
    ├── AggregateResultsWorkerTest.java        # 8 tests
    └── CallInstanceWorkerTest.java        # 8 tests
```
