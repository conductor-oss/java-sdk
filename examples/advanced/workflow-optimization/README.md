# Workflow Optimization in Java Using Conductor :  Analyze Execution, Find Waste, Parallelize, Benchmark

A Java Conductor workflow example for workflow optimization .  analyzing execution history to measure task durations, identifying wasted time (sequential tasks that could run in parallel, unnecessary waits), recommending parallelization opportunities, and benchmarking the optimized version against the original. Uses [Conductor](https://github.

## Slow Workflows Need Data-Driven Optimization, Not Guessing

Your order fulfillment workflow takes 45 seconds end-to-end, but the SLA is 30 seconds. Which tasks are the bottleneck? Are there sequential tasks with no data dependency that could run in parallel? Is there a task that always takes 10 seconds but only does a simple lookup .  suggesting it's waiting on a slow dependency?

Workflow optimization means analyzing real execution data (not guessing), identifying specific waste .  tasks that could be parallelized, unnecessary sequential dependencies, slow tasks that should be cached ,  recommending changes, and benchmarking the optimized workflow to measure actual improvement.

## The Solution

**You write the analysis and benchmarking logic. Conductor handles the optimization cycle, retries, and performance comparison tracking.**

`WfoAnalyzeExecutionWorker` processes the execution history to compute per-task duration statistics (mean, p95, p99) and identify the critical path. `WfoIdentifyWasteWorker` finds optimization opportunities: sequential tasks with no data dependency, tasks with high variance suggesting external bottlenecks, and unnecessary waits. `WfoParallelizeWorker` generates recommendations for which tasks to parallelize via `FORK_JOIN` to shorten the critical path. `WfoBenchmarkWorker` measures the expected improvement by comparing original and optimized execution profiles. Conductor records the analysis, waste identification, and benchmark results for every optimization cycle.

### What You Write: Workers

Four workers drive the optimization cycle: execution analysis, waste identification, parallelization recommendations, and before/after benchmarking, each producing data the next step needs.

| Worker | Task | What It Does |
|---|---|---|
| **WfoAnalyzeExecutionWorker** | `wfo_analyze_execution` | Builds a dependency graph of workflow tasks and measures per-task timings |
| **WfoBenchmarkWorker** | `wfo_benchmark` | Compares original vs. optimized execution times and reports improvement percentage |
| **WfoIdentifyWasteWorker** | `wfo_identify_waste` | Finds sequential tasks that could run in parallel and calculates wasted time |
| **WfoParallelizeWorker** | `wfo_parallelize` | Generates an optimized execution plan that groups independent tasks into parallel stages |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
wfo_analyze_execution
    │
    ▼
wfo_identify_waste
    │
    ▼
wfo_parallelize
    │
    ▼
wfo_benchmark

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
java -jar target/workflow-optimization-1.0.0.jar

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
java -jar target/workflow-optimization-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wfo_workflow_optimization \
  --version 1 \
  --input '{"workflowName": "test", "executionHistory": "sample-executionHistory"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wfo_workflow_optimization -s COMPLETED -c 5

```

## How to Extend

Each worker tackles one optimization concern .  replace the simulated execution analysis with real Conductor API metrics and the analyze-identify-benchmark pipeline runs unchanged.

- **WfoAnalyzeExecutionWorker** (`wfo_analyze_execution`): query real execution data from Conductor's `workflow/execution` API, parse task timing from OpenTelemetry traces, or aggregate metrics from Prometheus/Datadog
- **WfoIdentifyWasteWorker** (`wfo_identify_waste`): implement real dependency analysis: build a DAG of data dependencies between tasks and identify tasks with no incoming data edges that are currently sequential
- **WfoBenchmarkWorker** (`wfo_benchmark`): run real A/B benchmarks: execute original and optimized workflows with the same inputs, measure wall-clock time, and compute speedup with statistical confidence

The analysis and benchmark output contract stays fixed. Swap the simulated execution data for real Conductor execution history and the waste-identification pipeline runs unchanged.

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
workflow-optimization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowoptimization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowOptimizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WfoAnalyzeExecutionWorker.java
│       ├── WfoBenchmarkWorker.java
│       ├── WfoIdentifyWasteWorker.java
│       └── WfoParallelizeWorker.java
└── src/test/java/workflowoptimization/workers/
    ├── WfoAnalyzeExecutionWorkerTest.java        # 4 tests
    ├── WfoBenchmarkWorkerTest.java        # 4 tests
    ├── WfoIdentifyWasteWorkerTest.java        # 4 tests
    └── WfoParallelizeWorkerTest.java        # 4 tests

```
