# Health Check Aggregation in Java with Conductor

System-wide health check aggregation using FORK/JOIN. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Determining overall system health requires checking multiple infrastructure components. API gateway, database, cache, message queue, and aggregating their individual statuses into a single health verdict. These checks are independent and should run in parallel for speed, but the aggregation must wait for all of them to complete.

Without orchestration, health aggregation is a polling loop that sequentially pings each component, making the overall check slow (sum of all latencies). Partial failures (e.g., cache is down but everything else is healthy) are hard to represent without a structured aggregation step.

## The Solution

**You just write the per-component health-check workers and the aggregation worker. Conductor handles parallel health probes, per-component timeouts, and automatic aggregation once all checks complete.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Five workers probe infrastructure in parallel: CheckApiWorker, CheckDbWorker, CheckCacheWorker, and CheckQueueWorker each report component health, then AggregateHealthWorker produces an overall system verdict.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateHealthWorker** | `hc_aggregate_health` | Aggregates all component health checks into an overall system status (healthy/degraded). |
| **CheckApiWorker** | `hc_check_api` | Checks the API gateway health and reports latency. |
| **CheckCacheWorker** | `hc_check_cache` | Checks the Redis cache health and reports memory usage. |
| **CheckDbWorker** | `hc_check_db` | Checks the database health and reports connection pool utilization. |
| **CheckQueueWorker** | `hc_check_queue` | Checks the Kafka message queue health and reports consumer lag. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
    ├── hc_check_api
    ├── hc_check_db
    ├── hc_check_cache
    └── hc_check_queue
    │
    ▼
JOIN (wait for all branches)
hc_aggregate_health

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
java -jar target/health-check-aggregation-1.0.0.jar

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
java -jar target/health-check-aggregation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow health_check_aggregation \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w health_check_aggregation -s COMPLETED -c 5

```

## How to Extend

Point each check worker at your real API gateway, database, Redis cache, and Kafka cluster health endpoints, the parallel-check-and-aggregate workflow stays exactly the same.

- **AggregateHealthWorker** (`hc_aggregate_health`): apply real aggregation logic with degraded-vs-down thresholds and push the result to a monitoring dashboard (Grafana, PagerDuty)
- **CheckApiWorker** (`hc_check_api`): make a real HTTP health-check call to your API gateway's /health endpoint
- **CheckCacheWorker** (`hc_check_cache`): run a Redis PING and check memory usage via INFO MEMORY

Adding a new infrastructure component to monitor means adding one worker, the parallel-check-and-aggregate flow stays the same.

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
health-check-aggregation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/healthcheckaggregation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HealthCheckAggregationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateHealthWorker.java
│       ├── CheckApiWorker.java
│       ├── CheckCacheWorker.java
│       ├── CheckDbWorker.java
│       └── CheckQueueWorker.java
└── src/test/java/healthcheckaggregation/workers/
    ├── AggregateHealthWorkerTest.java        # 2 tests
    ├── CheckApiWorkerTest.java        # 2 tests
    ├── CheckCacheWorkerTest.java        # 2 tests
    ├── CheckDbWorkerTest.java        # 2 tests
    └── CheckQueueWorkerTest.java        # 2 tests

```
