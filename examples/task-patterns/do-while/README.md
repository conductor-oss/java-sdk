# Do-While Loop in Java with Conductor

DO_WHILE loop demo: processes items in a batch one at a time, iterating until the batch is complete, then summarizes all results. Demonstrates Conductor's declarative loop construct where the orchestrator manages iteration state, progress tracking, and per-iteration retry. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to process items in a batch one at a time, where each item requires a separate task execution with its own retry policy and timeout. The batch size is determined at runtime (could be 5 items or 500), and if processing fails on item 47 of 100, you need to retry just item 47. . Not restart from item 1. After all items are processed, you need a summary of what happened.

Without orchestration, you'd write a for-loop in a single long-running process, managing iteration state manually, adding checkpointing logic to survive crashes, and building retry handling for individual item failures. If the process dies mid-batch, you either restart from scratch (wasting work on items 1-46) or build complex resume logic with database-backed state that is hard to test and fragile to maintain.

## The Solution

**You just write the per-item processing and summarization workers. Conductor handles the iteration, state tracking, and per-iteration retry.**

This example demonstrates Conductor's DO_WHILE loop task, a declarative iteration construct where the loop condition (`iteration < batchSize`) is evaluated by Conductor after each pass. ProcessItemWorker handles one item per iteration: it receives the current iteration number, processes the item, and returns the incremented iteration counter. If the worker fails on any iteration, Conductor retries that specific iteration without re-processing already-completed items. After the loop exits (when iteration >= batchSize), SummarizeWorker aggregates the results into a completion message with the total count. Conductor tracks every iteration with its own inputs and outputs, giving you full visibility into batch progress without any custom logging or state management.

### What You Write: Workers

Two workers handle the iterative batch: ProcessItemWorker processes one item per loop iteration and increments the counter, while SummarizeWorker aggregates results after all iterations complete.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessItemWorker** | `dw_process_item` | Processes a single item within the loop. Takes the current iteration number, increments it, and returns itemProcessed=true, the new iteration count, and a result string like "Item-3 processed". Defaults iteration to 0 if missing. |
| **SummarizeWorker** | `dw_summarize` | Runs after the loop completes. Takes totalIterations and returns totalProcessed and a summary string like "Processed 5 items successfully". Defaults to 0 if totalIterations is missing. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
DO_WHILE (loop condition: iteration < batchSize)
    └── dw_process_item (runs once per iteration)
    │
    ▼
dw_summarize (runs once after loop exits)

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
java -jar target/do-while-1.0.0.jar

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
java -jar target/do-while-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# Process a batch of 5 items
conductor workflow start \
  --workflow do_while_demo \
  --version 1 \
  --input '{"batchSize": 5}'

# Process a larger batch of 20 items
conductor workflow start \
  --workflow do_while_demo \
  --version 1 \
  --input '{"batchSize": 20}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w do_while_demo -s COMPLETED -c 5

```

## How to Extend

Replace the demo item processor with your real per-item logic. Database writes, API calls, or file operations, and the DO_WHILE loop workflow runs unchanged.

- **ProcessItemWorker** (`dw_process_item`): process real items from a queue or database: transform a CSV row, call an external API for enrichment, send an individual notification, or migrate a record from one system to another
- **SummarizeWorker** (`dw_summarize`): aggregate results from all iterations: count successes vs: failures, compute total processing time, generate a batch completion report, and write it to S3 or email it to stakeholders
- **Add a validation step**: add a second task inside the loopOver array (e.g., `dw_validate_item`) that runs before `dw_process_item` on each iteration; Conductor executes both tasks sequentially per iteration

Replacing the demo item processing with real per-record logic does not affect the DO_WHILE loop mechanics, as long as each iteration returns the updated iteration counter.

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
do-while/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition with DO_WHILE loop
├── src/main/java/dowhile/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DoWhileExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessItemWorker.java
│       └── SummarizeWorker.java
└── src/test/java/dowhile/workers/
    ├── ProcessItemWorkerTest.java   # 9 tests
    └── SummarizeWorkerTest.java     # 7 tests

```
