# Simple Plus System in Java with Conductor

Combines SIMPLE workers with INLINE system tasks. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to build a store sales report: fetch orders from a database, calculate statistics (total revenue, average order value, max order), generate a visual chart, and format a summary email. Some of these steps require external services (database queries, chart generation APIs) and need dedicated workers. Others are simple calculations or string formatting that can run as lightweight JavaScript on the server. Building a worker for every step. including trivial math and string formatting,  adds unnecessary operational overhead.

Without orchestration, you'd write a single reporting class that mixes data fetching, statistical calculation, chart generation, and email formatting in one monolithic method. The lightweight calculations are buried alongside expensive API calls, making it impossible to retry just the chart generation if the charting service is down. There is no visibility into which step produced which intermediate result.

## The Solution

**You just write the data fetch and chart generation workers. Lightweight calculations and formatting run as server-side INLINE tasks. Conductor handles mixing SIMPLE workers with server-side INLINE tasks, retries for external service calls, and seamless data passing between worker and system task outputs.**

This example demonstrates mixing SIMPLE workers (external processing) with INLINE system tasks (server-side JavaScript) in the same workflow. FetchOrdersWorker (SIMPLE) queries order data for a storeId and date range from a database. this needs a real worker because it calls external services. The `calculate_stats` step (INLINE) computes total revenue, average order value, max order, and order count using JavaScript on the Conductor server,  no worker needed for basic math. GenerateVisualReportWorker (SIMPLE) takes the computed stats and generates a chart,  this needs a worker because it calls a charting service. The `format_summary` step (INLINE) builds a summary email body from the stats and chart URL,  no worker needed for string formatting. You deploy workers only for steps that require external access.

### What You Write: Workers

Two SIMPLE workers handle external interactions. FetchOrdersWorker queries order data from a database, and GenerateVisualReportWorker calls a charting service. While lightweight statistics and formatting run as server-side INLINE JavaScript tasks.

| Worker | Task | What It Does |
|---|---|---|
| **FetchOrdersWorker** | `fetch_orders` | SIMPLE worker that fetches order data for a given store. In a real system this would call a database or API. Here it ... |
| **GenerateVisualReportWorker** | `generate_visual_report` | SIMPLE worker that generates a visual report (chart) from stats. In a real system this would call a charting service ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
fetch_orders
    │
    ▼
calculate_stats [INLINE]
    │
    ▼
generate_visual_report
    │
    ▼
format_summary [INLINE]

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
java -jar target/simple-plus-system-1.0.0.jar

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
java -jar target/simple-plus-system-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mixed_tasks_demo \
  --version 1 \
  --input '{"storeId": "TEST-001", "dateRange": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mixed_tasks_demo -s COMPLETED -c 5

```

## How to Extend

Connect the fetch worker to a real database, point the chart worker at a real charting API, and let INLINE tasks handle the math and formatting, the mixed worker/system-task workflow runs unchanged.

- **FetchOrdersWorker** (`fetch_orders`): query your order database (PostgreSQL, MongoDB) or e-commerce platform API (Shopify, WooCommerce) for orders by store and date range
- **GenerateVisualReportWorker** (`generate_visual_report`): generate charts via a charting library (JFreeChart, QuickChart API) or BI tool API (Metabase, Looker), and upload the resulting image to S3 for embedding in the summary email

Connecting the fetch worker to a real database or the chart worker to a production charting API does not change the hybrid workflow structure, since INLINE tasks handle the calculations independently.

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
simple-plus-system/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/simpleplussystem/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SimplePlusSystemExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FetchOrdersWorker.java
│       └── GenerateVisualReportWorker.java
└── src/test/java/simpleplussystem/workers/
    ├── FetchOrdersWorkerTest.java        # 6 tests
    └── GenerateVisualReportWorkerTest.java        # 6 tests

```
