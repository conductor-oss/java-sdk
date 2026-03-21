# Implementing Workflow Timeout in Java with Conductor :  Bounding Total Workflow Execution Time

A Java Conductor workflow example demonstrating workflow-level timeouts. setting a maximum execution time (30 seconds) for the entire workflow, ensuring that even if individual tasks complete quickly, the overall workflow doesn't run indefinitely.

## The Problem

Individual task timeouts prevent hung workers, but you also need a ceiling on total workflow execution time. A workflow with 10 tasks might have each task complete in 5 seconds; but if something causes the workflow to loop or stall between tasks, it could run for hours. A workflow-level timeout ensures the entire execution completes within a bounded time.

Without orchestration, total execution time limits require starting a timer at the beginning and checking it between every step. If the timer fires while a task is running, there's no clean way to abort it. Workflow-level timeouts are nearly impossible to implement correctly in hand-rolled orchestration.

## The Solution

The workflow definition includes `timeoutSeconds: 30`. if the entire workflow hasn't completed within that window, Conductor marks it as timed out. This catches scenarios that per-task timeouts miss: long queues between tasks, stuck decision logic, or unexpected loops. The timeout is configured in the workflow definition, not in code. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

FastWorker completes its processing quickly, while the workflow-level timeoutSeconds setting ensures the entire workflow execution is bounded. Catching scenarios that per-task timeouts miss, such as stuck logic or long queue delays between tasks.

| Worker | Task | What It Does |
|---|---|---|
| **FastWorker** | `wft_fast` | Fast worker that completes immediately. Returns { result: "done-{mode}" } based on the input mode. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
wft_fast

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
java -jar target/workflow-timeout-1.0.0.jar

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
java -jar target/workflow-timeout-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wf_timeout_demo \
  --version 1 \
  --input '{"mode": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wf_timeout_demo -s COMPLETED -c 5

```

## How to Extend

Each worker runs real processing. connect them to your business services, set timeoutSeconds on the workflow definition, and the automatic total-execution-time enforcement stays the same.

- **FastWorker** (`wft_fast`): replace with any real worker. the workflow timeout applies at the workflow level regardless of how fast or slow individual tasks are

Connect any real workers, and the workflow-level timeout configured in the definition enforces total execution time limits automatically.

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
workflow-timeout/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowtimeout/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowTimeoutExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── FastWorker.java
└── src/test/java/workflowtimeout/workers/
    └── FastWorkerTest.java        # 6 tests

```
