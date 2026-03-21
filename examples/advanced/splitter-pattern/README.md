# Splitter Pattern in Java Using Conductor :  Receive Composite Message, Split, Process Parts in Parallel, Combine

A Java Conductor workflow example for the splitter pattern. receiving a composite message containing multiple items, splitting it into individual parts, processing each part independently and in parallel via `FORK_JOIN`, and combining the per-part results into a single unified response. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Composite Messages Contain Independent Items That Can Be Processed in Parallel

An order arrives with three line items. a laptop, a monitor, and a keyboard. Each item needs independent processing: inventory check, pricing lookup, tax calculation. Processing them sequentially triples the latency. Processing them in parallel requires splitting the order into individual items, dispatching each to its own processing pipeline, waiting for all three to complete, and reassembling the results into a single order response.

The splitter pattern decomposes a composite message into its constituent parts, processes each part independently (and in parallel when possible), then combines the results. This is fundamental to any system that receives batch requests, multi-item orders, or composite events.

## The Solution

**You write the per-item processing logic. Conductor handles the split, parallel execution, per-item retries, and recombination.**

`SplReceiveCompositeWorker` ingests the composite message. `SplSplitWorker` decomposes it into individual parts. A `FORK_JOIN` processes all three parts in parallel. `SplProcessPart1Worker`, `SplProcessPart2Worker`, and `SplProcessPart3Worker` each handle one item independently. The `JOIN` waits for all parts to finish. `SplCombineWorker` reassembles the per-part results into a single response. Conductor handles the split, parallel processing, and recombination, retrying any failed part without affecting the others.

### What You Write: Workers

Six workers implement the split-and-recombine pattern: composite message reception, item splitting, three parallel per-item processors, and result combination, each handling one line item independently.

| Worker | Task | What It Does |
|---|---|---|
| **SplCombineWorker** | `spl_combine` | Recombines the individually processed line items into a final order total and fulfillment count |
| **SplProcessPart1Worker** | `spl_process_part_1` | Fulfills the first line item (e.g., LAPTOP-15) and computes its subtotal |
| **SplProcessPart2Worker** | `spl_process_part_2` | Fulfills the second line item (e.g., MOUSE-WL) and computes its subtotal |
| **SplProcessPart3Worker** | `spl_process_part_3` | Fulfills the third line item (e.g., MONITOR-27) and computes its subtotal |
| **SplReceiveCompositeWorker** | `spl_receive_composite` | Receives the composite order message containing multiple line items |
| **SplSplitWorker** | `spl_split` | Splits the composite order into individual line items (SKU, quantity, price) for parallel processing |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
spl_receive_composite
    │
    ▼
spl_split
    │
    ▼
FORK_JOIN
    ├── spl_process_part_1
    ├── spl_process_part_2
    └── spl_process_part_3
    │
    ▼
JOIN (wait for all branches)
spl_combine

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
java -jar target/splitter-pattern-1.0.0.jar

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
java -jar target/splitter-pattern-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow spl_splitter_pattern \
  --version 1 \
  --input '{"compositeMessage": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w spl_splitter_pattern -s COMPLETED -c 5

```

## How to Extend

Each worker processes one item from the composite message. replace the demo per-item handlers with real inventory, pricing, or tax service calls and the split-process-combine pipeline runs unchanged.

- **SplSplitWorker** (`spl_split`): parse real composite messages: split multi-item orders from your e-commerce API, extract individual records from a batch CSV upload, or decompose a multi-part MIME message
- **SplProcessPart*Workers** (`spl_process_part_1/2/3`). run real per-item processing: inventory checks via your warehouse API, pricing lookups from a product catalog, or tax calculations via Avalara/TaxJar
- **SplCombineWorker** (`spl_combine`): reassemble into a real response: build an order confirmation with per-item status, generate a batch processing report, or create a composite API response

The per-item result contract stays fixed. Swap demo fulfillment for real inventory and pricing APIs and the split-process-combine pipeline runs unchanged.

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
splitter-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/splitterpattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SplitterPatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SplCombineWorker.java
│       ├── SplProcessPart1Worker.java
│       ├── SplProcessPart2Worker.java
│       ├── SplProcessPart3Worker.java
│       ├── SplReceiveCompositeWorker.java
│       └── SplSplitWorker.java
└── src/test/java/splitterpattern/workers/
    ├── SplCombineWorkerTest.java        # 4 tests
    ├── SplProcessPart1WorkerTest.java        # 4 tests
    ├── SplProcessPart2WorkerTest.java        # 4 tests
    ├── SplProcessPart3WorkerTest.java        # 4 tests
    ├── SplReceiveCompositeWorkerTest.java        # 4 tests
    └── SplSplitWorkerTest.java        # 4 tests

```
