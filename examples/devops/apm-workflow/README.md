# APM Workflow in Java with Conductor :  Analyze Latency, Report Metrics, Collect Traces, Detect Bottlenecks

Automates Application Performance Monitoring (APM) analysis using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects distributed traces for a service, analyzes latency percentiles (p50/p95/p99), detects performance bottlenecks like N+1 queries and large payload serialization, and generates an APM report with actionable recommendations. You write the performance analysis logic, Conductor handles retries, failure routing, durability, and observability for free.

## Finding the Slow Endpoints

Your checkout service handled 25,000 requests in the last hour. Most responded in 45ms, but the p99 is 520ms. Something is dragging the tail. Two endpoints are suspiciously slow: `/api/search` has an N+1 query problem, and `/api/export` is choking on large payload serialization. You need to collect the traces, crunch the latency numbers, pinpoint the bottlenecks, and produce a report the team can act on.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the trace analysis and bottleneck detection logic. Conductor handles the collect-analyze-detect-report pipeline and tracks every performance investigation.**

`AnalyzeLatencyWorker` examines request latency distributions across endpoints .  identifying which services and endpoints have degraded performance with p50/p95/p99 breakdowns. `ApmReportWorker` generates a performance report summarizing latency trends, error rates, throughput, and resource utilization. `CollectTracesWorker` gathers distributed traces for the slowest requests, showing the full request path across services. `DetectBottlenecksWorker` analyzes the traces to identify specific bottlenecks ,  slow database queries, overloaded services, or network latency. Conductor chains these four steps for automated performance investigation.

### What You Write: Workers

Four workers handle APM analysis. Collecting distributed traces, analyzing latency percentiles, detecting bottlenecks, and generating performance reports.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeLatency** | `apm_analyze_latency` | Analyzes latency percentiles from collected traces. |
| **ApmReport** | `apm_report` | Generates the final APM report summarizing bottlenecks and recommendations. |
| **CollectTraces** | `apm_collect_traces` | Collects application traces for the specified service and time range. |
| **DetectBottlenecks** | `apm_detect_bottlenecks` | Detects performance bottlenecks based on latency analysis. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> AnalyzeLatency -> ApmReport -> CollectTraces -> DetectBottlenecks -> Output
```

## Example Output

```
=== Example 421: APM Workflow ===

Step 1: Registering task definitions...
  Registered: apm_analyze_latency, apm_report, apm_collect_traces, apm_detect_bottlenecks

Step 2: Registering workflow 'apm_workflow_421'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [apm_analyze_latency] Analyzing latency from
  [apm_report] Generating APM report for
  [apm_collect_traces] Collecting traces for
  [apm_detect_bottlenecks] Detecting bottlenecks (p99:

  Status: COMPLETED
  Output: {p50Latency=..., p95Latency=..., p99Latency=..., meanLatency=...}

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
java -jar target/apm-workflow-1.0.0.jar
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
java -jar target/apm-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow apm_workflow \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w apm_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one APM analysis step .  replace the simulated calls with Jaeger trace queries, Datadog APM metrics, or New Relic span analysis, and the performance investigation workflow runs unchanged.

- **CollectTraces** (`apm_collect_traces`): integrate with Jaeger, Zipkin, or AWS X-Ray APIs to pull real distributed traces with span-level timing data for the specified service and time range
- **AnalyzeLatency** (`apm_analyze_latency`): query Prometheus `histogram_quantile()` or Datadog APM metrics for real p50/p95/p99 latency distributions, identifying slow endpoints like `/api/search` and `/api/export`
- **DetectBottlenecks** (`apm_detect_bottlenecks`): analyze trace spans to identify specific bottlenecks: N+1 query patterns from ORM logs, large payload serialization from response sizes, and upstream service timeouts from span durations
- **ApmReport** (`apm_report`): generate performance reports with bottleneck details and actionable recommendations, posting to Slack, Confluence, or a Grafana annotation for the team to review

Replace with real tracing backends like Jaeger or Datadog; the APM workflow operates with the same trace-analysis contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
apm-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apmworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApmWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeLatency.java
│       ├── ApmReport.java
│       ├── CollectTraces.java
│       └── DetectBottlenecks.java
└── src/test/java/apmworkflow/workers/
    ├── AnalyzeLatencyTest.java        # 8 tests
    ├── ApmReportTest.java        # 7 tests
    ├── CollectTracesTest.java        # 7 tests
    └── DetectBottlenecksTest.java        # 8 tests
```
