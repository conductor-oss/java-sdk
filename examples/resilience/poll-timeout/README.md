# Implementing Poll Timeout in Java with Conductor :  Detecting Absent Workers via Queue Wait Limits

A Java Conductor workflow example demonstrating the pollTimeoutSeconds setting .  defining how long a task waits in the queue for a worker to pick it up before Conductor marks it as timed out, detecting scenarios where workers are down or not polling.

## The Problem

You schedule a task, but no worker picks it up .  the worker process crashed, the deployment failed, or the worker is polling a different task queue. Without a poll timeout, the task sits in the queue indefinitely and the workflow hangs forever. You need to detect when no worker is available within a reasonable time window and take action (alert, fail the workflow, route to a fallback).

Without orchestration, detecting absent workers requires custom health check infrastructure .  heartbeat monitoring, process supervisors, and manual alerting when tasks are stuck. Each task queue needs its own monitoring, and the detection logic is separate from the task definition.

## The Solution

**You just write the task logic and set poll timeout thresholds. Conductor handles absent-worker detection for free.**

The task definition includes `pollTimeoutSeconds` .  if no worker picks up the task within that window, Conductor automatically marks it as timed out. This triggers retry logic or failure handling as configured. Every poll timeout event is recorded, so you can see exactly when and why a task was not picked up. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

PollNormalTaskWorker processes tasks normally, while Conductor's pollTimeoutSeconds setting automatically detects when no worker picks up a task within the configured window. Indicating crashed or missing workers.

| Worker | Task | What It Does |
|---|---|---|
| **PollNormalTaskWorker** | `poll_normal_task` | Worker for the poll_normal_task task. Picks up the task and processes it immediately. Takes a mode input and returns ... |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
poll_normal_task
```

## Example Output

```
=== Poll Timeout Demo: Handle Missing Workers ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'poll_timeout_demo'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [poll_normal_task] Processing with mode:

  Status: COMPLETED
  Output: {result=...}

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
java -jar target/poll-timeout-1.0.0.jar
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
java -jar target/poll-timeout-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow poll_timeout_demo \
  --version 1 \
  --input '{"mode": "sample-mode"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w poll_timeout_demo -s COMPLETED -c 5
```

## How to Extend

Each worker polls for real tasks .  connect them to your business services, set pollTimeoutSeconds per task definition, and the automatic absent-worker detection stays the same.

- **PollNormalTaskWorker** (`poll_normal_task`): replace with any real worker .  the poll timeout configuration is in the task definition, not the worker code

Replace with any real worker, and the poll timeout configuration in the task definition provides absent-worker detection without any code changes.

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
poll-timeout/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/polltimeout/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PollTimeoutExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── PollNormalTaskWorker.java
└── src/test/java/polltimeout/workers/
    └── PollNormalTaskWorkerTest.java        # 8 tests
```
