# Switch Plus Fork in Java with Conductor

SWITCH + FORK demo. conditional parallel execution. Batch type triggers parallel lanes A and B; default runs single processing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process items differently based on whether they arrive as a batch or individually. Batch processing requires two parallel lanes (Lane A and Lane B) that split the work and process simultaneously, then join before continuing. Single-item processing just runs one task. The decision. parallel batch processing or sequential single-item processing,  must be made at runtime based on the input type. After either path completes, a common finalization step runs.

Without orchestration, you'd write an if/else that either spawns threads for parallel processing or runs a single function. Managing the thread pool, barrier, and join logic only when the batch path is taken adds conditional complexity. There is no way to see whether a given request took the batch or single path, and if one parallel lane fails in batch mode, you need custom logic to cancel or wait for the other lane.

## The Solution

**You just write the parallel lane processing, single-item processing, and finalization workers. Conductor handles the conditional branching and parallel execution.**

This example demonstrates combining Conductor's SWITCH and FORK_JOIN in a single workflow. conditional parallel execution. A JavaScript SWITCH evaluator checks the input `type`: if it equals `batch`, the workflow enters a FORK_JOIN with two parallel lanes (ProcessAWorker and ProcessBWorker processing items simultaneously), followed by a JOIN. If the type is anything else (default case), SingleProcessWorker handles the items sequentially. After either the batch fork/join or the single-item processing completes, FinalizeWorker runs as a common finalization step. Conductor records which branch was taken and, for batch mode, the results from both parallel lanes independently.

### What You Write: Workers

Four workers support conditional parallel execution: ProcessAWorker and ProcessBWorker run in parallel lanes for batch mode, SingleProcessWorker handles sequential items in default mode, and FinalizeWorker runs after either path completes.

| Worker | Task | What It Does |
|---|---|---|
| **FinalizeWorker** | `sf_finalize` | Finalizes and computes done |
| **ProcessAWorker** | `sf_process_a` | Lane A batch processor. Processes items in parallel lane A and returns the lane identifier and item count. |
| **ProcessBWorker** | `sf_process_b` | Lane B batch processor. Processes items in parallel lane B and returns the lane identifier and item count. |
| **SingleProcessWorker** | `sf_single_process` | Single item processor for the default (non-batch) case. Returns mode: "single" to indicate single-item processing was... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_by_type_ref)
    ├── batch: fork_batch -> join_batch
    └── default: sf_single_process
    │
    ▼
sf_finalize

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
java -jar target/switch-plus-fork-1.0.0.jar

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
java -jar target/switch-plus-fork-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow switch_fork_demo \
  --version 1 \
  --input '{"type": "standard", "items": [{"id": "ITEM-001", "quantity": 2}]}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w switch_fork_demo -s COMPLETED -c 5

```

## How to Extend

Replace the lane workers with your real parallel processing logic and the single-item worker with your sequential handler, the conditional SWITCH+FORK workflow runs unchanged.

- **ProcessAWorker** (`sf_process_a`): handle one aspect of batch processing in parallel (e.g., data validation, image processing, or the first half of a split workload)
- **ProcessBWorker** (`sf_process_b`): handle the other aspect in parallel (e.g., enrichment, notification, or the second half of the workload)
- **SingleProcessWorker** (`sf_single_process`): handle individual items that don't need parallel processing (e.g., single order fulfillment, one-off data import)
- **FinalizeWorker** (`sf_finalize`): common completion logic: update status records, send completion notifications, and trigger downstream workflows

Replacing the lane workers with real batch processing logic or adding more parallel lanes does not affect the SWITCH + FORK_JOIN workflow structure, as long as each worker returns its expected processing result.

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
switch-plus-fork/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/switchplusfork/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SwitchPlusForkExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java
│       ├── ProcessAWorker.java
│       ├── ProcessBWorker.java
│       └── SingleProcessWorker.java
└── src/test/java/switchplusfork/workers/
    ├── FinalizeWorkerTest.java        # 5 tests
    ├── ProcessAWorkerTest.java        # 7 tests
    ├── ProcessBWorkerTest.java        # 7 tests
    └── SingleProcessWorkerTest.java        # 5 tests

```
