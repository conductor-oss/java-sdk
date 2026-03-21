# Exclusive Join in Java with Conductor

EXCLUSIVE_JOIN demo. query three vendors in parallel, wait for all responses, then select the best offer by lowest price and fastest response time. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to get price quotes from multiple vendors simultaneously and pick the best offer. A product query goes out to Vendor A, Vendor B, and Vendor C at the same time. Each vendor responds with a price and response time. After all three respond, you need to compare their offers and select the winner. lowest price wins, with fastest response time as the tiebreaker. The entire comparison must happen only after every vendor has replied, not as responses trickle in.

Without orchestration, you'd spawn three threads or async HTTP calls, manage a countdown latch or barrier to wait for all responses, handle the case where one vendor times out while the others succeed, and write comparison logic that runs only after the barrier releases. If the process crashes after two vendors respond, you lose those responses and have to re-query all three. There is no record of what each vendor quoted or why a particular vendor was selected.

## The Solution

**You just write the vendor query and best-offer selection workers. Conductor handles the parallel execution, join, and per-vendor retry.**

This example demonstrates Conductor's FORK_JOIN pattern for parallel vendor quoting. Three vendor workers (VendorA, VendorB, VendorC) run concurrently, each querying their respective pricing API with the same product query. A JOIN task waits until all three vendors have responded, then the SelectBestWorker compares all offers side by side. selecting the vendor with the lowest price, using response time as a tiebreaker when prices match. Conductor tracks each vendor's quote independently, so if Vendor B's API times out, Conductor retries just that branch while the other quotes remain safely stored.

### What You Write: Workers

Four workers implement the competitive quoting pattern: three vendor workers (A, B, C) query prices in parallel, and SelectBestWorker compares all offers to pick the lowest price with response time as a tiebreaker.

| Worker | Task | What It Does |
|---|---|---|
| **SelectBestWorker** | `ej_select_best` | Selects the best vendor from the three parallel vendor responses. Picks the vendor with the lowest price. If prices a... |
| **VendorAWorker** | `ej_vendor_a` | Simulates Vendor A responding to a product query. Returns deterministic price and response time data. |
| **VendorBWorker** | `ej_vendor_b` | Simulates Vendor B responding to a product query. Returns deterministic price and response time data. Vendor B has th... |
| **VendorCWorker** | `ej_vendor_c` | Simulates Vendor C responding to a product query. Returns deterministic price and response time data. Vendor C has th... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
FORK_JOIN
    ├── ej_vendor_a
    ├── ej_vendor_b
    └── ej_vendor_c
    │
    ▼
JOIN (wait for all branches)
ej_select_best

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
java -jar target/exclusive-join-1.0.0.jar

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
java -jar target/exclusive-join-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow exclusive_join_demo \
  --version 1 \
  --input '{"query": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w exclusive_join_demo -s COMPLETED -c 5

```

## How to Extend

Replace the simulated vendor workers with real pricing API calls to your actual suppliers, and the parallel quote-and-compare workflow runs unchanged.

- **VendorAWorker** (`ej_vendor_a`): call Vendor A's real pricing API (REST, SOAP, or EDI), authenticate with their credentials, parse the response into a normalized price/availability format
- **VendorBWorker** (`ej_vendor_b`): call Vendor B's pricing API, handling their specific response format, rate limits, and authentication scheme
- **SelectBestWorker** (`ej_select_best`): implement your real vendor selection logic: weighted scoring across price, lead time, shipping cost, and vendor reliability rating; write the winning quote to your procurement system or auto-generate a purchase order

Connecting vendor workers to real pricing APIs or adding a fourth vendor as another parallel branch does not change the fork-join-select workflow, as long as each vendor returns price and response time fields.

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
exclusive-join/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/exclusivejoin/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExclusiveJoinExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SelectBestWorker.java
│       ├── VendorAWorker.java
│       ├── VendorBWorker.java
│       └── VendorCWorker.java
└── src/test/java/exclusivejoin/workers/
    ├── SelectBestWorkerTest.java        # 7 tests
    ├── VendorAWorkerTest.java        # 7 tests
    ├── VendorBWorkerTest.java        # 7 tests
    └── VendorCWorkerTest.java        # 7 tests

```
