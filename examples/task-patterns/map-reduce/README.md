# Map Reduce in Java with Conductor

MapReduce Pattern. Splits log files into parallel analysis tasks using FORK_JOIN_DYNAMIC, then aggregates results in a reduce step. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to analyze a batch of log files for error and warning counts, where each log file can be analyzed independently and the results must be aggregated into a single report. The number of log files varies per run .  sometimes 3 files, sometimes 30. Each file has a name and line count, and the analysis must count errors and warnings within that file. After all files are analyzed in parallel, the results must be reduced into a summary report with total errors, total warnings, and per-file breakdowns.

Without orchestration, you'd write a thread pool to process log files in parallel, submit each file as a task, collect futures, and aggregate results manually. If the analysis crashes after processing 8 of 10 files, you lose all 8 results and restart from scratch. There is no visibility into which files have been analyzed, which are still in progress, or what the per-file error counts were before the crash.

## The Solution

**You just write the map, per-file analysis, and reduce workers. Conductor handles the dynamic fanout, parallel execution, and join.**

This example implements the classic MapReduce pattern using Conductor's FORK_JOIN_DYNAMIC. The MapWorker (map phase) takes the input list of log files and generates one `mr_analyze_log` task per file, each with a unique reference name. Conductor fans out to N parallel branches. Each AnalyzeLogWorker (map function) analyzes a single log file, counting errors and warnings by line count. A JOIN task waits for all analyses to complete, then the ReduceWorker (reduce phase) aggregates all per-file results from the join output into a single report with total error count, total warning count, and per-file breakdowns. If one log file analysis fails, Conductor retries just that branch while the other results remain safely stored.

### What You Write: Workers

Three workers implement the MapReduce pattern: MapWorker generates one analysis task per log file, AnalyzeLogWorker counts errors and warnings in each file independently, and ReduceWorker aggregates all per-file results into a summary report.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeLogWorker** | `mr_analyze_log` | Analyzes a single log file and returns error/warning counts. Takes { logFile: {name, lineCount}, index } and returns ... |
| **MapWorker** | `mr_map` | MAP phase: splits log files into parallel analysis tasks. Takes a list of log files and generates: - dynamicTasks: ar... |
| **ReduceWorker** | `mr_reduce` | REDUCE phase: aggregates results from all parallel log analysis tasks. Takes the joinOutput map (keyed by taskReferen... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Dynamic parallelism** | FORK_JOIN_DYNAMIC creates parallel branches at runtime based on input data |
| **Automatic joining** | JOIN waits for all dynamically forked tasks to complete |

### The Workflow

```
mr_map
    │
    ▼
FORK_JOIN_DYNAMIC (parallel, determined at runtime)
    │
    ▼
JOIN (wait for all branches)
mr_reduce
```

## Example Output

```
=== MapReduce: Log File Analysis ===

Step 1: Registering task definitions...
  Registered: mr_map, mr_analyze_log, mr_reduce

Step 2: Registering workflow 'map_reduce_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [mr_analyze_log] Analyzing
  [mr_map] Splitting
  [mr_reduce] Aggregated

  Status: COMPLETED
  Output: {fileName=..., errorCount=..., warningCount=..., lineCount=...}

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
java -jar target/map-reduce-1.0.0.jar
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
java -jar target/map-reduce-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow map_reduce_demo \
  --version 1 \
  --input '{"logFiles": "sample-logFiles", "name": "sample-name", "api-server.log": "sample-api-server.log", "lineCount": 5, "auth-service.log": "sample-auth-service.log", "payment-gateway.log": "sample-payment-gateway.log", "notification-service.log": "sample-notification-service.log"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w map_reduce_demo -s COMPLETED -c 5
```

## How to Extend

Replace the log analyzer with your real per-file analysis logic and aggregation rules, and the MapReduce-style parallel workflow runs unchanged regardless of how many files are submitted.

- **MapWorker** (`mr_map`): list log files from S3, GCS, or a shared filesystem; filter by date range, service name, or log level; and generate the dynamic task definitions with per-file configuration (file path, size, expected format)
- **AnalyzeLogWorker** (`mr_analyze_log`): parse real log files using regex patterns or a log parsing library (Logstash patterns, Grok), count errors/warnings/exceptions by severity, extract stack traces, and identify error categories
- **ReduceWorker** (`mr_reduce`): aggregate per-file results into a comprehensive report: total errors by severity, top error categories, error rate trends, and write the report to Elasticsearch, CloudWatch, or a monitoring dashboard

Replacing simulated log analysis with real file parsing does not change the map-analyze-reduce workflow, as long as each analyzer returns the expected error and warning count fields.

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
map-reduce/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mapreduce/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MapReduceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeLogWorker.java
│       ├── MapWorker.java
│       └── ReduceWorker.java
└── src/test/java/mapreduce/workers/
    ├── AnalyzeLogWorkerTest.java        # 8 tests
    ├── MapWorkerTest.java        # 7 tests
    └── ReduceWorkerTest.java        # 11 tests
```
