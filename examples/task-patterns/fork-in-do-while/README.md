# Fork In Do While in Java with Conductor

FORK inside DO_WHILE demo .  iterative parallel processing. Each iteration forks parallel batch-processing tasks, then a summary task reports after the loop. Uses [Conductor](https://github.## The Problem

You need to process multiple batches iteratively, where each batch contains tasks that can run in parallel. For example, processing 5 batches of data imports where each batch involves parallel validation, transformation, and loading steps. The loop runs until all batches are complete (iteration >= totalBatches), and within each iteration, the parallel tasks must all finish before the next iteration begins. After all batches complete, a summary step aggregates the results across every iteration.

Without orchestration, you'd write a for-loop around a thread pool, submitting parallel tasks per iteration, waiting on a barrier for each batch, and incrementing the counter manually. If the process crashes on batch 3 of 5, you restart from batch 1 because there is no checkpoint. There is no way to see which batches completed, which parallel tasks within a batch succeeded, or how long each iteration took.

## The Solution

**You just write the per-batch processing and summary workers. Conductor handles the loop iteration, parallel forking within each iteration, and checkpointing.**

This example demonstrates combining Conductor's DO_WHILE loop with FORK_JOIN inside the loop body .  iterative parallel processing. Each iteration of the loop forks parallel batch-processing tasks via FORK_JOIN, and a JOIN task waits for all parallel branches to complete before the loop condition is evaluated. The ProcessBatchWorker handles each parallel task within an iteration, returning a batchId and processing status. After the loop exits (when iteration >= totalBatches), the SummaryWorker aggregates results from all iterations into a final report. Conductor checkpoints every iteration, so if the process crashes on batch 3 of 5, it resumes from batch 3 ,  batches 1 and 2 are not re-processed.

### What You Write: Workers

Two workers handle iterative parallel processing: ProcessBatchWorker runs within each loop iteration's FORK_JOIN branches to process batch segments, and SummaryWorker aggregates results from all iterations after the loop completes.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessBatchWorker** | `fl_process_batch` | Processes a batch within the DO_WHILE loop. Takes the current iteration number and returns a batchId along with a pro... |
| **SummaryWorker** | `fl_summary` | Summarizes the results after the DO_WHILE loop completes. Takes the iterations count and produces a summary string. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
DO_WHILE
    └── fork_batch
    └── join_batch
    │
    ▼
fl_summary
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
java -jar target/fork-in-do-while-1.0.0.jar
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
java -jar target/fork-in-do-while-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fork_loop_demo \
  --version 1 \
  --input '{"totalBatches": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fork_loop_demo -s COMPLETED -c 5
```

## How to Extend

Replace the batch processing workers with your real parallel validation, transformation, or loading logic, and the iterative-parallel workflow runs unchanged.

- **ProcessBatchWorker** (`fl_process_batch`): execute real batch processing: import a page of database records, process a chunk of ETL data, or run a set of validation checks in parallel per iteration; each parallel branch can handle a different aspect (validate, transform, load)
- **SummaryWorker** (`fl_summary`): aggregate results across all loop iterations: total records processed, success/failure counts per batch, cumulative processing time, and write the summary to a dashboard, database, or notification channel

Replacing the batch processing logic with real data import or transformation work does not change the loop-with-fork structure, as long as each batch worker returns the expected batchId and processing status.

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
fork-in-do-while/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/forkindowhile/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ForkInDoWhileExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessBatchWorker.java
│       └── SummaryWorker.java
└── src/test/java/forkindowhile/workers/
    ├── ProcessBatchWorkerTest.java        # 9 tests
    └── SummaryWorkerTest.java        # 8 tests
```
