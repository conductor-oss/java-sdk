# Scatter-Gather in Java Using Conductor: Broadcast Query, Gather from Sources in Parallel, Aggregate

You need a price quote from five vendors. Vendor A responds in 200ms. Vendor B in 400ms. Vendor C is having a bad day and takes 30 seconds. If you query them sequentially, every customer waits 31 seconds for a price comparison page. If you query them in parallel, you're managing thread pools, countdown latches, partial failure handling (vendor C timed out but A and B are fine), and response merging, all hand-rolled. And when vendor D starts returning prices in euros instead of dollars, your aggregation logic silently produces garbage. This example broadcasts a query to multiple sources in parallel using Conductor's `FORK_JOIN`, gathers responses from each, and aggregates them into a single ranked result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Best Prices Require Querying Multiple Sources Simultaneously

A travel search needs to query multiple airline APIs. Delta, United, American, for flight prices on the same route. Querying them sequentially takes 3x as long. Querying them in parallel means managing concurrent HTTP calls, handling partial failures (United is down but Delta and American responded), setting per-source timeouts, and merging heterogeneous response formats into a single ranked list sorted by price.

The scatter-gather pattern broadcasts the same query to all sources simultaneously, gathers responses from each (tolerating failures), and aggregates them into a unified result. Building this by hand means thread pools, countdown latches, exception aggregation, and response merging, all of which Conductor provides declaratively.

## The Solution

**You write the per-source query logic. Conductor handles parallel scatter, per-source retries, and result aggregation.**

`SgrBroadcastWorker` prepares the query for distribution to all configured sources. A `FORK_JOIN` scatters the query to three gatherers in parallel. `SgrGather1Worker`, `SgrGather2Worker`, and `SgrGather3Worker` each query their respective data source and return results. The `JOIN` waits for all three to respond. `SgrAggregateWorker` merges the per-source results into a single ranked output (e.g., sorted by price, relevance, or latency). Conductor handles the parallel scatter, retries any slow or failed gatherer, and records per-source response times and result counts.

### What You Write: Workers

Five workers implement the scatter-gather: query broadcasting, three parallel source gatherers (one per pricing API), and best-price aggregation, each source queried independently and in parallel.

| Worker | Task | What It Does |
|---|---|---|
| **SgrAggregateWorker** | `sgr_aggregate` | Collects all price responses and selects the best (lowest) price across sources |
| **SgrBroadcastWorker** | `sgr_broadcast` | Broadcasts the price query to all source services in parallel |
| **SgrGather1Worker** | `sgr_gather_1` | Queries price service A and returns its price quote with currency |
| **SgrGather2Worker** | `sgr_gather_2` | Queries price service B and returns its price quote with currency |
| **SgrGather3Worker** | `sgr_gather_3` | Queries price service C and returns its price quote with currency |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations, the pattern and Conductor orchestration stay the same.

### The Workflow

```
sgr_broadcast
    │
    ▼
FORK_JOIN
    ├── sgr_gather_1
    ├── sgr_gather_2
    └── sgr_gather_3
    │
    ▼
JOIN (wait for all branches)
sgr_aggregate

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
java -jar target/scatter-gather-1.0.0.jar

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
java -jar target/scatter-gather-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sgr_scatter_gather \
  --version 1 \
  --input '{"query": "laptop_model_X500"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sgr_scatter_gather -s COMPLETED -c 5

```

## How to Extend

Each worker queries one data source. Replace the demo API responses with real airline, pricing, or search provider calls and the parallel broadcast-and-aggregate pipeline runs unchanged.

- **SgrBroadcastWorker** (`sgr_broadcast`): fan out real queries to multiple APIs: airline GDS systems (Amadeus, Sabre), price comparison APIs, or multi-vendor product catalogs
- **SgrGather*Workers** (`sgr_gather_1/2/3`). Query real data sources: REST APIs with per-source auth, database replicas in different regions, or Elasticsearch clusters with different indices
- **SgrAggregateWorker** (`sgr_aggregate`): implement real aggregation: merge and deduplicate results, rank by price/relevance/freshness, and apply business rules (preferred vendors, margin thresholds)

Each gatherer's output contract stays fixed. Swap the demo price APIs for real airline or hotel booking APIs and the broadcast-aggregate pipeline runs unchanged.

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
scatter-gather/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/scattergather/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ScatterGatherExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SgrAggregateWorker.java
│       ├── SgrBroadcastWorker.java
│       ├── SgrGather1Worker.java
│       ├── SgrGather2Worker.java
│       └── SgrGather3Worker.java
└── src/test/java/scattergather/workers/
    ├── SgrAggregateWorkerTest.java        # 4 tests
    ├── SgrBroadcastWorkerTest.java        # 4 tests
    ├── SgrGather1WorkerTest.java        # 4 tests
    ├── SgrGather2WorkerTest.java        # 4 tests
    └── SgrGather3WorkerTest.java        # 4 tests

```
