# Workflow Debugging in Java Using Conductor :  Instrument, Execute, Trace, Analyze, Report

A Java Conductor workflow example for workflow debugging .  instrumenting a workflow with debug hooks, executing it with tracing enabled, collecting execution traces, analyzing the trace data for anomalies and bottlenecks, and generating a debug report. Uses [Conductor](https://github.## When Workflows Fail, You Need More Than a Stack Trace

A 20-step workflow fails at step 14. The error says "null pointer exception." But the real question is: what were the inputs at step 14? What did step 13 output? How long did each step take? Did step 7 produce unexpected output that propagated silently until step 14 crashed? You need distributed tracing across the entire workflow execution .  not just the error at the failure point.

Workflow debugging means instrumenting the workflow to capture detailed state at each step, executing with tracing enabled at a configurable debug level, collecting the full execution trace including inputs, outputs, and timing, analyzing the trace to find the root cause (data anomalies, unexpected branches, performance bottlenecks), and producing a human-readable debug report.

## The Solution

**You write the instrumentation and analysis logic. Conductor handles trace collection, retries, and debug session tracking.**

`WfdInstrumentWorker` adds debug hooks to the target workflow at the specified debug level (minimal, standard, verbose). `WfdExecuteWorker` runs the instrumented workflow and captures detailed execution data. `WfdCollectTraceWorker` gathers the distributed execution trace .  every task's inputs, outputs, timing, and status. `WfdAnalyzeWorker` examines the trace for anomalies: null values that shouldn't be null, execution times outside expected ranges, unexpected branch selections. `WfdReportWorker` produces the final debug report. Conductor records the full debugging session for replay.

### What You Write: Workers

Five workers form the debug cycle: instrumentation, traced execution, trace collection, anomaly analysis, and report generation, each capturing one layer of diagnostic data.

| Worker | Task | What It Does |
|---|---|---|
| `WfdInstrumentWorker` | `wfd_instrument` | Takes a workflow name and debug level (e.g., INFO), injects trace points (timing hooks at task start/end, data capture at decision branches) into the workflow, and returns the instrumented workflow name with the list of trace points |
| `WfdExecuteWorker` | `wfd_execute` | Runs the instrumented workflow and returns an execution ID, total duration in milliseconds, and the number of tasks executed |
| `WfdCollectTraceWorker` | `wfd_collect_trace` | Gathers trace data from the execution by reading each trace point .  captures timestamps and recorded values (timing measurements, branch selections) for every instrumented location |
| `WfdAnalyzeWorker` | `wfd_analyze` | Examines collected trace data for anomalies (e.g., tasks exceeding duration thresholds), identifies performance bottlenecks, and produces a summary of findings |
| `WfdReportWorker` | `wfd_report` | Generates a final debug report containing the workflow name, timestamp, anomaly count, and actionable recommendations (e.g., "investigate slow task that exceeded threshold by 1800ms") |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
wfd_instrument
    │
    ▼
wfd_execute
    │
    ▼
wfd_collect_trace
    │
    ▼
wfd_analyze
    │
    ▼
wfd_report
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
java -jar target/workflow-debugging-1.0.0.jar
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
java -jar target/workflow-debugging-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wfd_workflow_debugging \
  --version 1 \
  --input '{"workflowName": "test", "debugLevel": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wfd_workflow_debugging -s COMPLETED -c 5
```

## How to Extend

Each worker covers one debugging phase .  replace the simulated trace collection with real distributed tracing APIs like Jaeger or OpenTelemetry and the instrument-analyze-report pipeline runs unchanged.

The trace and report output contract stays fixed. Swap the simulated instrumentation for real OpenTelemetry spans or Jaeger traces and the analyze-report pipeline runs unchanged.

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
workflow-debugging/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowdebugging/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowDebuggingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
```
