# Sync Execution in Java with Conductor

Simple workflow for demonstrating sync vs async execution. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to execute a workflow and get the result back immediately in the same API call .  like a synchronous function call that happens to be a full workflow under the hood. For example, adding two numbers and returning the sum to the caller without them needing to poll for the result. The caller sends `{a: 5, b: 3}` and gets `{sum: 8}` back in the HTTP response, even though the computation ran through the full Conductor workflow engine with retries, durability, and tracking.

Without synchronous execution, the caller would start the workflow (getting back a workflow ID), then poll the status endpoint repeatedly until the workflow completes, then extract the output from the completed execution. That polling loop adds latency, complexity, and client-side retry logic for what is conceptually a simple request-response operation.

## The Solution

**You just write the computation worker. Conductor handles the synchronous blocking execution, durability, and result delivery.**

This example demonstrates synchronous workflow execution .  starting a workflow and waiting for the result in a single blocking call. The AddWorker takes two numbers (`a` and `b`) and returns their sum. The example code shows both async execution (start the workflow, get a workflow ID, poll for the result) and sync execution (start the workflow and get the output directly). Under the hood, the workflow runs through the full Conductor engine ,  with task tracking, retry capability, and execution history; but the caller gets a synchronous request-response experience. This pattern is ideal for short-lived workflows that back API endpoints or real-time computations.

### What You Write: Workers

A single worker demonstrates the synchronous execution pattern: AddWorker takes two numbers and returns their sum, showing how a trivial computation becomes a durable, tracked, retryable operation behind a synchronous API call.

| Worker | Task | What It Does |
|---|---|---|
| **AddWorker** | `sync_add` | SIMPLE worker that adds two numbers. Takes input { a, b } and returns { sum: a + b }. Demonstrates the simplest possi... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
sync_add
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
java -jar target/sync-execution-1.0.0.jar
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
java -jar target/sync-execution-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sync_exec_demo \
  --version 1 \
  --input '{"a": "test-value", "b": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sync_exec_demo -s COMPLETED -c 5
```

## How to Extend

Replace the add-two-numbers worker with your real computation logic, and the synchronous request-response execution pattern works unchanged.

- **AddWorker** (`sync_add`): implement any fast, request-response computation: price calculation, tax computation, eligibility check, or data validation that needs to be tracked and retryable but must return a result synchronously to the caller

Swapping in real computation logic (pricing, eligibility, tax calculation) does not change the synchronous request-response execution pattern, as long as the worker completes quickly enough for the blocking call.

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
sync-execution/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/syncexecution/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SyncExecutionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── AddWorker.java
└── src/test/java/syncexecution/workers/
    └── AddWorkerTest.java        # 10 tests
```
