# Request Aggregation in Java with Conductor

Aggregates data from multiple microservices in parallel using FORK_JOIN, then merges results into a single response. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A single client request (e.g., loading a user dashboard) often requires data from multiple backend services. User profiles, order history, and personalized recommendations. These fetches are independent and should run in parallel for performance, then the results must be merged into a single response.

Without orchestration, the API layer manually fans out HTTP calls using CompletableFuture or RxJava, handles partial failures (one service down), and merges results in application code. Adding a new data source means modifying the fan-out logic and the merge step.

## The Solution

**You just write the user-fetch, orders-fetch, recommendations-fetch, and merge workers. Conductor handles parallel fan-out to all data sources, per-source timeout isolation, and automatic join before merging.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers serve the aggregation pipeline: FetchUserWorker, FetchOrdersWorker, and FetchRecommendationsWorker each retrieve data from independent services in parallel, then MergeResultsWorker combines them into a single dashboard response.

| Worker | Task | What It Does |
|---|---|---|
| **FetchOrdersWorker** | `agg_fetch_orders` | Fetches recent orders for a user. |
| **FetchRecommendationsWorker** | `agg_fetch_recommendations` | Generates recommendations for a user. |
| **FetchUserWorker** | `agg_fetch_user` | Fetches user profile data. |
| **MergeResultsWorker** | `agg_merge_results` | Merges results from multiple services into a single response. |

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
    ├── agg_fetch_user
    ├── agg_fetch_orders
    └── agg_fetch_recommendations
    │
    ▼
JOIN (wait for all branches)
agg_merge_results
```

## Example Output

```
=== Request Aggregation Demo ===

Step 1: Registering task definitions...
  Registered: agg_fetch_user, agg_fetch_orders, agg_fetch_recommendations, agg_merge_results

Step 2: Registering workflow 'request_aggregation_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [orders] Fetching recent orders for
  [recs] Generating recommendations for
  [user] Fetching profile for
  [merge] Combining results from 3 services

  Status: COMPLETED
  Output: {orders=..., items=..., count=..., name=...}

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
java -jar target/request-aggregation-1.0.0.jar
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
java -jar target/request-aggregation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow request_aggregation_workflow \
  --version 1 \
  --input '{"userId": "user-42", "user-42": "sample-user-42"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w request_aggregation_workflow -s COMPLETED -c 5
```

## How to Extend

Point each fetch worker at your real user-profile API, order database, and recommendation engine (AWS Personalize, custom ML), the parallel-fetch-and-merge workflow stays exactly the same.

- **FetchOrdersWorker** (`agg_fetch_orders`): query your order service API or order database for recent order history
- **FetchRecommendationsWorker** (`agg_fetch_recommendations`): call your recommendation engine (e.g., AWS Personalize, custom ML service)
- **FetchUserWorker** (`agg_fetch_user`): call your user-profile microservice or database via HTTP/gRPC

Replacing any fetch worker with a call to a real microservice or database preserves the parallel-fetch-and-merge workflow.

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
request-aggregation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/requestaggregation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RequestAggregationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FetchOrdersWorker.java
│       ├── FetchRecommendationsWorker.java
│       ├── FetchUserWorker.java
│       └── MergeResultsWorker.java
└── src/test/java/requestaggregation/workers/
    ├── FetchOrdersWorkerTest.java        # 8 tests
    ├── FetchRecommendationsWorkerTest.java        # 8 tests
    ├── FetchUserWorkerTest.java        # 7 tests
    └── MergeResultsWorkerTest.java        # 8 tests
```
