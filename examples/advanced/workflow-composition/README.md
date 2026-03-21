# Workflow Composition in Java Using Conductor :  Compose Sub-Workflows into a Unified Order Pipeline

A Java Conductor workflow example for workflow composition. combining two independent sub-workflows (payment processing and inventory management) into a single unified order pipeline, where each sub-workflow has its own steps and the results are merged at the end. Uses [Conductor](https://github.

## Complex Processes Are Made of Simpler Ones

Fulfilling a customer order requires two independent processes: payment processing (validate card, charge amount) and inventory management (check stock, reserve items). Each process has its own steps, failure modes, and retry logic. Building them as a single monolithic workflow means a payment retry can block inventory reservation, and a stock check failure can prevent a valid payment from proceeding.

Workflow composition lets you build each sub-process independently, test it in isolation, and then compose them into a larger workflow. The order pipeline runs payment steps (sub-workflow A: validate then charge) and inventory steps (sub-workflow B: check then reserve), then merges the results to produce the final order status.

## The Solution

**You write each sub-workflow's steps. Conductor handles composition, cross-workflow retries, and result merging.**

`WcpSubAStep1Worker` and `WcpSubAStep2Worker` handle the first sub-workflow (e.g., payment validation and charging). `WcpSubBStep1Worker` and `WcpSubBStep2Worker` handle the second sub-workflow (e.g., inventory check and reservation). `WcpMergeWorker` combines the results from both sub-workflows into a unified order status. Conductor sequences the sub-workflows and their merge, recording the full lineage of both processes and their combined outcome.

### What You Write: Workers

Five workers span two sub-workflows. Order validation and processing in sub-workflow A, customer lookup and enrichment in sub-workflow B, plus a merge step that unifies both outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **WcpMergeWorker** | `wcp_merge` | Combines order processing and customer enrichment results into a unified output |
| **WcpSubAStep1Worker** | `wcp_sub_a_step1` | Validates the order by ID in sub-workflow A |
| **WcpSubAStep2Worker** | `wcp_sub_a_step2` | Processes the validated order and computes the total in sub-workflow A |
| **WcpSubBStep1Worker** | `wcp_sub_b_step1` | Looks up the customer profile (tier, tenure) by ID in sub-workflow B |
| **WcpSubBStep2Worker** | `wcp_sub_b_step2` | Enriches the customer data with discount eligibility in sub-workflow B |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
wcp_sub_a_step1
    │
    ▼
wcp_sub_a_step2
    │
    ▼
wcp_sub_b_step1
    │
    ▼
wcp_sub_b_step2
    │
    ▼
wcp_merge

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
java -jar target/workflow-composition-1.0.0.jar

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
java -jar target/workflow-composition-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow workflow_composition_demo \
  --version 1 \
  --input '{"orderId": "TEST-001", "customerId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_composition_demo -s COMPLETED -c 5

```

## How to Extend

Each worker implements one step of a sub-workflow. replace the simulated payment and inventory calls with real Stripe and warehouse APIs and the composed order pipeline runs unchanged.

- **WcpSubAStep1Worker / WcpSubAStep2Worker**: implement real payment processing: Stripe `paymentIntents.create()` for card validation and `paymentIntents.capture()` for charging
- **WcpSubBStep1Worker / WcpSubBStep2Worker**: implement real inventory management: query warehouse API for stock availability and call reservation service to hold items
- **WcpMergeWorker** (`wcp_merge`): produce a real order confirmation: write to the orders database, send confirmation email via SES, and publish order event to Kafka for downstream systems

Each sub-workflow's output contract stays fixed. Swap one sub-workflow's implementation (e.g., replace the payment provider) without touching the other sub-workflow or the merge step.

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
workflow-composition/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowcomposition/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowCompositionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WcpMergeWorker.java
│       ├── WcpSubAStep1Worker.java
│       ├── WcpSubAStep2Worker.java
│       ├── WcpSubBStep1Worker.java
│       └── WcpSubBStep2Worker.java
└── src/test/java/workflowcomposition/workers/
    ├── WcpMergeWorkerTest.java        # 4 tests
    ├── WcpSubAStep1WorkerTest.java        # 4 tests
    ├── WcpSubAStep2WorkerTest.java        # 4 tests
    ├── WcpSubBStep1WorkerTest.java        # 4 tests
    └── WcpSubBStep2WorkerTest.java        # 4 tests

```
