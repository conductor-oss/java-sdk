# Workflow Profiling in Java Using Conductor :  Instrument, Execute, Measure, Find Bottlenecks, Optimize

A Java Conductor workflow example for workflow profiling .  instrumenting a workflow to capture timing data, executing it across multiple iterations, measuring per-task execution times, identifying bottleneck tasks, and generating optimization recommendations. Uses [Conductor](https://github.

## You Can't Optimize What You Don't Measure

Your workflow runs in 30 seconds, but where does the time go? Is it the database query in step 3 (takes 12 seconds) or the API call in step 7 (takes 8 seconds)? Running the workflow once gives you one data point. Running it 100 times and profiling each task across iterations reveals the real bottleneck .  maybe step 3 averages 2 seconds but occasionally spikes to 12 seconds, while step 7 is consistently 8 seconds.

Workflow profiling means instrumenting tasks to capture high-resolution timing, running multiple iterations to build statistical profiles, computing per-task percentiles (p50, p95, p99), and identifying which tasks contribute most to end-to-end latency. Without this data, optimization efforts target the wrong tasks.

## The Solution

**You write the measurement and bottleneck analysis logic. Conductor handles multi-iteration execution, retries, and profiling session tracking.**

`WfpInstrumentWorker` adds timing hooks to the target workflow. `WfpExecuteWorker` runs the workflow across the configured number of iterations, collecting timing data for each run. `WfpMeasureTimesWorker` computes per-task statistics .  mean, median, p95, p99 ,  across all iterations. `WfpBottleneckWorker` identifies the tasks that dominate the critical path and have the highest latency variance. `WfpOptimizeWorker` generates concrete optimization recommendations (cache this lookup, parallelize these two tasks, increase timeout for this external call). Conductor records the full profiling session.

### What You Write: Workers

Five workers form the profiling pipeline: instrumentation, multi-iteration execution, per-task timing measurement, bottleneck detection, and optimization recommendations, each feeding statistical data to the next.

| Worker | Task | What It Does |
|---|---|---|
| **WfpBottleneckWorker** | `wfp_bottleneck` | Identifies the slowest tasks and ranks them by severity (critical, high) |
| **WfpExecuteWorker** | `wfp_execute` | Runs the instrumented workflow and collects per-iteration execution times |
| **WfpInstrumentWorker** | `wfp_instrument` | Adds profiling hooks (CPU time, wall time, memory usage) to the target workflow |
| **WfpMeasureTimesWorker** | `wfp_measure_times` | Measures average execution times for each task and computes total workflow duration |
| **WfpOptimizeWorker** | `wfp_optimize` | Produces optimization suggestions (e.g., batch writes, parallelize transforms) with expected speedup |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
wfp_instrument
    │
    ▼
wfp_execute
    │
    ▼
wfp_measure_times
    │
    ▼
wfp_bottleneck
    │
    ▼
wfp_optimize

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
java -jar target/workflow-profiling-1.0.0.jar

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
java -jar target/workflow-profiling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wfp_workflow_profiling \
  --version 1 \
  --input '{"workflowName": "test", "iterations": "sample-iterations"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wfp_workflow_profiling -s COMPLETED -c 5

```

## How to Extend

Each worker covers one profiling phase .  replace the simulated timing collection with real OpenTelemetry spans or Conductor execution APIs and the measure-bottleneck-optimize pipeline runs unchanged.

- **WfpInstrumentWorker** (`wfp_instrument`): add real instrumentation: inject OpenTelemetry spans, enable Conductor's built-in task timing metrics, or add custom Micrometer timers around task execution
- **WfpMeasureTimesWorker** (`wfp_measure_times`): compute real statistics using Apache Commons Math (`DescriptiveStatistics`) for percentile calculations across execution history queried from Conductor's API
- **WfpBottleneckWorker** (`wfp_bottleneck`): implement real bottleneck detection: critical path analysis using DAG traversal, variance analysis to find inconsistent tasks, and Amdahl's law calculations for parallelization potential

The timing and bottleneck output contract stays fixed. Swap the simulated profiler for real Micrometer or OpenTelemetry instrumentation and the measure-optimize pipeline runs unchanged.

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
workflow-profiling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowprofiling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowProfilingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WfpBottleneckWorker.java
│       ├── WfpExecuteWorker.java
│       ├── WfpInstrumentWorker.java
│       ├── WfpMeasureTimesWorker.java
│       └── WfpOptimizeWorker.java
└── src/test/java/workflowprofiling/workers/
    ├── WfpBottleneckWorkerTest.java        # 4 tests
    ├── WfpExecuteWorkerTest.java        # 4 tests
    ├── WfpInstrumentWorkerTest.java        # 4 tests
    ├── WfpMeasureTimesWorkerTest.java        # 4 tests
    └── WfpOptimizeWorkerTest.java        # 4 tests

```
