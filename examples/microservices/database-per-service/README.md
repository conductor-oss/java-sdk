# Database Per Service in Java with Conductor

Database per service pattern with parallel queries and view composition. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

When each microservice owns its own database, building a unified view that spans multiple services (e.g., a user dashboard showing profile, orders, and product recommendations) requires querying each service's database independently and then composing the results. These queries are independent and should run in parallel for performance.

Without orchestration, the calling service manually fans out HTTP calls to each service, collects responses, handles partial failures, and merges the data, all in application code with no visibility into which query is slow or failed.

## The Solution

**You just write the per-database query workers and the view-composition worker. Conductor handles parallel fan-out, per-query retries and timeouts, and automatic join before composition.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers span isolated databases: QueryUserDbWorker, QueryOrderDbWorker, and QueryProductDbWorker each hit their own data store in parallel, then ComposeViewWorker merges the results into a unified dashboard view.

| Worker | Task | What It Does |
|---|---|---|
| **ComposeViewWorker** | `dps_compose_view` | Merges user, order, and product data into a single unified view. |
| **QueryOrderDbWorker** | `dps_query_order_db` | Queries the order database for order count and latest order ID by userId. |
| **QueryProductDbWorker** | `dps_query_product_db` | Queries the product database for recently viewed products. |
| **QueryUserDbWorker** | `dps_query_user_db` | Queries the user database for profile data (name, tier) by userId. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
    ├── dps_query_user_db
    ├── dps_query_order_db
    └── dps_query_product_db
    │
    ▼
JOIN (wait for all branches)
dps_compose_view

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
java -jar target/database-per-service-1.0.0.jar

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
java -jar target/database-per-service-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow database_per_service_workflow \
  --version 1 \
  --input '{"userId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w database_per_service_workflow -s COMPLETED -c 5

```

## How to Extend

Point each query worker at your real user, order, and product databases or microservice APIs, the parallel-query-and-compose workflow stays exactly the same.

- **ComposeViewWorker** (`dps_compose_view`): apply real business logic for view composition (e.g., personalized sorting, access control)
- **QueryOrderDbWorker** (`dps_query_order_db`): query your order database or order microservice API for recent orders
- **QueryProductDbWorker** (`dps_query_product_db`): query your product catalog database or product microservice API for browsing history and recommendations

Pointing any query worker at a different database or microservice API preserves the parallel-query-and-compose workflow.

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
database-per-service/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/databaseperservice/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DatabasePerServiceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComposeViewWorker.java
│       ├── QueryOrderDbWorker.java
│       ├── QueryProductDbWorker.java
│       └── QueryUserDbWorker.java
└── src/test/java/databaseperservice/workers/
    ├── ComposeViewWorkerTest.java        # 2 tests
    ├── QueryOrderDbWorkerTest.java        # 2 tests
    ├── QueryProductDbWorkerTest.java        # 2 tests
    └── QueryUserDbWorkerTest.java        # 2 tests

```
