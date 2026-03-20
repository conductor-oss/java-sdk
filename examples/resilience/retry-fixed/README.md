# Implementing Fixed Retry in Java with Conductor :  Constant-Delay Retries for Transient Failures

A Java Conductor workflow example demonstrating fixed retry strategy .  retrying a failing task with a constant 1-second delay between attempts, suitable for transient errors where the recovery time is predictable.

## The Problem

You have a task that fails due to transient issues .  a database connection dropped, a file lock is temporarily held, a service is restarting. The recovery time is predictable (1-2 seconds), so exponential backoff would waste time with unnecessarily long delays. You need simple, fixed-interval retries: try, wait 1 second, try again.

Without orchestration, fixed retries mean a for-loop with Thread.sleep(1000) inside every task that might fail. The retry count is hardcoded, changing the delay requires a code change, and there's no record of how many retries were needed for a given execution.

## The Solution

The worker does its job and returns failure if the transient issue persists. Conductor retries with a fixed 1-second delay as configured in the task definition. Every retry attempt is tracked with timing. Changing the delay or retry count is a JSON config change. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

RetryFixedWorker performs the task and reports success or failure, while Conductor handles fixed-interval retries with a constant 1-second delay between attempts. Ideal for transient errors with predictable recovery times.

| Worker | Task | What It Does |
|---|---|---|
| **RetryFixedWorker** | `retry_fixed_task` | Worker for retry_fixed_task .  simulates transient failures. Uses an attempt counter keyed by workflow ID. Fails the f.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
retry_fixed_task
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
java -jar target/retry-fixed-1.0.0.jar
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
java -jar target/retry-fixed-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow retry_fixed_demo \
  --version 1 \
  --input '{"failCount": 10}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w retry_fixed_demo -s COMPLETED -c 5
```

## How to Extend

Each worker calls a real service .  connect to your database or internal API, configure the fixed retry delay in the task definition, and the constant-interval retry behavior stays the same.

- **RetryFixedWorker** (`retry_fixed_task`): replace with any task that experiences predictable transient failures .  database reconnection, file lock contention, service restart detection

Connect to your real database or internal service, and the fixed-interval retry behavior is purely a task definition setting requiring no code changes.

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
retry-fixed/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/retryfixed/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RetryFixedExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── RetryFixedWorker.java
└── src/test/java/retryfixed/workers/
    └── RetryFixedWorkerTest.java        # 9 tests
```
