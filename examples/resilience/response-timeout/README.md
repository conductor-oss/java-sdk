# Implementing Response Timeout in Java with Conductor :  Detecting Stuck Workers via Response Time Limits

A Java Conductor workflow example demonstrating the responseTimeoutSeconds setting .  detecting workers that pick up a task but take too long to respond, indicating they are stuck, deadlocked, or processing indefinitely.

## The Problem

A worker picks up a task but never returns a result .  it's stuck in an infinite loop, waiting on a deadlocked resource, or blocked on a network call that will never complete. Without a response timeout, Conductor waits forever, the workflow hangs, and downstream steps never execute. You need to detect stuck workers and trigger retry or failure handling within a bounded time.

Without orchestration, detecting stuck workers requires external watchdogs .  process-level timeouts, thread dumps, and manual restarts. The business logic must implement its own internal timeouts for every blocking call, and there's no centralized view of which workers are stuck across the system.

## The Solution

The task definition includes `responseTimeoutSeconds` .  if a worker doesn't complete within that window after picking up the task, Conductor marks it as timed out and retries or fails it as configured. Stuck workers are detected automatically without any timeout logic in the worker code. Every timeout event is recorded with timing details. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

RespTimeoutWorker processes tasks within the configured time limit, while Conductor's responseTimeoutSeconds setting automatically detects workers that pick up a task but never return a result due to deadlocks, infinite loops, or blocked network calls.

| Worker | Task | What It Does |
|---|---|---|
| **RespTimeoutWorker** | `resp_timeout_task` | Worker for the resp_timeout_task. Responds quickly within the 3-second response timeout. Tracks the attempt number ac... |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
resp_timeout_task
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
java -jar target/response-timeout-1.0.0.jar
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
java -jar target/response-timeout-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow resp_timeout_demo \
  --version 1 \
  --input '{"mode": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w resp_timeout_demo -s COMPLETED -c 5
```

## How to Extend

Each worker processes real tasks .  connect them to your business services, set responseTimeoutSeconds per task definition, and the automatic stuck-worker detection stays the same.

- **RespTimeoutWorker** (`resp_timeout_task`): replace with any worker that may hang .  external API calls, database queries, file processing ,  the response timeout is configured in the task definition

Connect any worker that might hang to your real services, and the response timeout configured in the task definition catches stuck workers automatically without any code-level timeouts.

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
response-timeout/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/responsetimeout/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ResponseTimeoutExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── RespTimeoutWorker.java
└── src/test/java/responsetimeout/workers/
    └── RespTimeoutWorkerTest.java        # 8 tests
```
