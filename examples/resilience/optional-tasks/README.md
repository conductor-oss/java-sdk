# Implementing Optional Tasks in Java with Conductor :  Required Processing with Non-Blocking Enrichment

A Java Conductor workflow example demonstrating optional tasks .  a required core task that must succeed, followed by an optional enrichment task that can fail without blocking the pipeline, and a summarize step that gracefully handles missing enrichment data.

## The Problem

Your pipeline has a required processing step and an optional enrichment step. The enrichment adds value (e.g., appending metadata, fetching supplementary data) but isn't critical .  if it fails, the pipeline should continue with whatever data is available. The summarize step must handle both the enriched case and the degraded case where enrichment data is missing.

Without orchestration, optional tasks are implemented with try/catch blocks that swallow errors. The summarize step has no way to know whether enrichment was skipped due to a genuine failure or was never attempted. Testing the degraded path requires manually breaking the enrichment service.

## The Solution

**You just write the core processing and optional enrichment logic. Conductor handles marking tasks as optional, continuing the pipeline when optional tasks fail, and clear tracking of whether enrichment succeeded or was skipped for every execution.**

The required worker runs first. The optional enrichment worker is configured to continue on failure. Conductor marks it as optional. The summarize worker checks whether enrichment data is present and adapts its output accordingly. Every execution shows clearly whether enrichment succeeded or was skipped, and why. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

RequiredWorker handles the core processing that must succeed, OptionalEnrichWorker adds supplementary data that can fail without blocking, and SummarizeWorker produces the final output while gracefully handling missing enrichment.

| Worker | Task | What It Does |
|---|---|---|
| **OptionalEnrichWorker** | `opt_optional_enrich` | Worker for opt_optional_enrich .  enriches data with additional information. This task is marked as optional in the wo.. |
| **RequiredWorker** | `opt_required` | Worker for opt_required .  processes the input data. Returns { result: "processed-{data}" } on success. |
| **SummarizeWorker** | `opt_summarize` | Worker for opt_summarize .  summarizes the workflow results. Checks whether the enrichment data is available. If enric.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
opt_required
    │
    ▼
opt_optional_enrich
    │
    ▼
opt_summarize
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
java -jar target/optional-tasks-1.0.0.jar
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
java -jar target/optional-tasks-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow optional_tasks_demo \
  --version 1 \
  --input '{"data": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w optional_tasks_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one concern .  connect the required worker to your core business service, the optional enrichment worker to a third-party data API, and the required-plus-optional-enrichment workflow stays the same.

- **OptionalEnrichWorker** (`opt_optional_enrich`): call a third-party API for supplementary data (geolocation, sentiment analysis, risk scoring) .  failure is acceptable
- **RequiredWorker** (`opt_required`): replace with your core processing logic that must succeed (data validation, transaction processing, record creation)
- **SummarizeWorker** (`opt_summarize`): produce the final output, including degradation metadata when enrichment was unavailable

Connect the required worker to your core service and the optional worker to a third-party enrichment API, and the graceful degradation behavior works identically in production.

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
optional-tasks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/optionaltasks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OptionalTasksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── OptionalEnrichWorker.java
│       ├── RequiredWorker.java
│       └── SummarizeWorker.java
└── src/test/java/optionaltasks/workers/
    ├── OptionalEnrichWorkerTest.java        # 5 tests
    ├── RequiredWorkerTest.java        # 6 tests
    └── SummarizeWorkerTest.java        # 8 tests
```
