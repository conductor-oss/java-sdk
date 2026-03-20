# Workflow Patterns Showcase in Java Using Conductor :  Chain, Fork-Join, and Loop in One Workflow

A Java Conductor workflow example showcasing multiple workflow patterns in a single definition .  a sequential chain step, a parallel fork-join that splits into two branches and merges results, and a `DO_WHILE` loop that iterates until a condition is met. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Real Workflows Combine Multiple Patterns

Real-world processes don't fit a single pattern. An ETL pipeline starts with a sequential validation step (chain), then splits data processing across multiple workers (fork-join), then iterates over remaining unprocessed records until the batch is complete (loop). Most workflow tutorials show one pattern at a time, but production workflows combine sequential steps, parallel fan-out, and iterative loops in the same definition.

This example shows how Conductor composes all three patterns .  chain, fork-join, and do-while loop ,  in a single workflow, demonstrating that you can mix patterns freely without special glue code.

## The Solution

**You write the chain, split, and loop logic. Conductor handles pattern composition, iteration control, and parallel branch management.**

`WpChainStepWorker` handles the initial sequential processing step. A `FORK_JOIN` then splits into two parallel branches .  `WpSplitAWorker` and `WpSplitBWorker` process different aspects of the data simultaneously. The `JOIN` waits for both branches. `WpMergeResultsWorker` combines the parallel outputs. Finally, a `DO_WHILE` loop runs `WpLoopIterationWorker` iteratively ,  processing remaining items until the iteration count reaches the configured limit. Conductor handles the sequential-to-parallel-to-iterative transitions seamlessly, tracking every iteration and branch.

### What You Write: Workers

Five workers demonstrate three patterns in one workflow: a chain step, two parallel fork branches, a result merger, and a loop iterator, showing how Conductor composes sequential, parallel, and iterative logic.

| Worker | Task | What It Does |
|---|---|---|
| **WpChainStepWorker** | `wp_chain_step` | Chain step: sequential processing in a chain pattern. |
| **WpLoopIterationWorker** | `wp_loop_iteration` | Loop iteration: processes one iteration in a DO_WHILE loop. |
| **WpMergeResultsWorker** | `wp_merge_results` | Merge results from fork branches A and B. |
| **WpSplitAWorker** | `wp_split_a` | Split branch A: parallel fork processing. |
| **WpSplitBWorker** | `wp_split_b` | Split branch B: parallel fork processing. |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Loop execution** | DO_WHILE repeats a set of tasks until a condition is met |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
wp_chain_step
    │
    ▼
FORK_JOIN
    ├── wp_split_a
    └── wp_split_b
    │
    ▼
JOIN (wait for all branches)
wp_merge_results
    │
    ▼
DO_WHILE
    └── wp_loop_iteration
```

## Example Output

```
=== Workflow Patterns Demo ===

Step 1: Registering task definitions...
  Registered: wp_chain_step, wp_split_a, wp_split_b, wp_merge_results, wp_loop_iteration

Step 2: Registering workflow 'workflow_patterns_demo'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [chain] Processing:
  [loop] Iteration
  [merge] Combining:
  [split-A] Branch A processing (from
  [split-B] Branch B processing (from

  Status: COMPLETED
  Output: {result=..., iteration=..., processed=..., combined=...}

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
java -jar target/workflow-patterns-1.0.0.jar
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
java -jar target/workflow-patterns-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow workflow_patterns_demo \
  --version 1 \
  --input '{"inputData": "sample-inputData", "sample_payload": "sample-sample-payload", "iterations": "sample-iterations"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_patterns_demo -s COMPLETED -c 5
```

## How to Extend

Each worker demonstrates one pattern role .  replace the simulated chain, fork, and loop steps with real ETL or data processing logic and the combined multi-pattern workflow runs unchanged.

- **WpChainStepWorker** (`wp_chain_step`): implement real sequential preprocessing: data validation, schema normalization, or authentication checks before the parallel phase begins
- **WpSplitAWorker / WpSplitBWorker** (`wp_split_a/b`): run real parallel workloads: split by data partition (A handles US records, B handles EU records), by concern (A does enrichment, B does scoring), or by destination
- **WpLoopIterationWorker** (`wp_loop_iteration`): implement real iterative processing: pagination through API results, batch processing with cursor-based continuation, or retry loops for transient failures

Each worker's output contract stays fixed. Swap any worker's implementation and the chain-fork-loop composition runs unchanged.

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
workflow-patterns/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowpatterns/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowPatternsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WpChainStepWorker.java
│       ├── WpLoopIterationWorker.java
│       ├── WpMergeResultsWorker.java
│       ├── WpSplitAWorker.java
│       └── WpSplitBWorker.java
└── src/test/java/workflowpatterns/workers/
    ├── WpChainStepWorkerTest.java        # 8 tests
    ├── WpLoopIterationWorkerTest.java        # 8 tests
    ├── WpMergeResultsWorkerTest.java        # 8 tests
    ├── WpSplitAWorkerTest.java        # 7 tests
    └── WpSplitBWorkerTest.java        # 7 tests
```
