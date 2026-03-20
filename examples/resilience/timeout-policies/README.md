# Implementing Timeout Policies in Java with Conductor :  TIME_OUT_WF, RETRY, and ALERT Behaviors

A Java Conductor workflow example demonstrating Conductor's timeout policies .  configuring different timeout behaviors per task: TIME_OUT_WF (fail the entire workflow), RETRY (retry the timed-out task), and ALERT (continue but flag the timeout).

## The Problem

Different tasks need different timeout behaviors. A critical payment task should fail the entire workflow if it times out (TIME_OUT_WF) .  you can't proceed without payment confirmation. A flaky enrichment task should be retried on timeout (RETRY) ,  it usually works on the second attempt. An analytics task can silently timeout (ALERT) ,  the data will be backfilled later. One-size-fits-all timeout handling doesn't work.

Without orchestration, implementing different timeout policies per task means wrapping each call in its own timeout handler with custom logic .  one throws an exception, one retries, one logs and continues. The timeout behavior is buried in code rather than declared in configuration.

## The Solution

**You just write the task logic and declare timeout policies in the workflow definition. Conductor handles per-task timeout detection with configurable policy enforcement (TIME_OUT_WF, RETRY, ALERT_ONLY), and a record of every timeout event showing which policy was applied and what action was taken.**

Each task's timeout policy is declared in the task definition .  `timeoutPolicy: TIME_OUT_WF` for critical tasks, `RETRY` for flaky tasks, and `ALERT_ONLY` for non-critical tasks. The workers just do their work; Conductor handles the timeout detection and policy enforcement. Changing a task's timeout behavior is a config change, not a code change. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CriticalWorker uses TIME_OUT_WF to fail the entire workflow on timeout, RetryableWorker uses RETRY to automatically retry on timeout, and OptionalWorker uses ALERT_ONLY to continue the pipeline while flagging the timeout event.

| Worker | Task | What It Does |
|---|---|---|
| **CriticalWorker** | `tp_critical` | Worker for the tp_critical task. The task definition uses timeoutPolicy: TIME_OUT_WF. If this worker does not respond... |
| **OptionalWorker** | `tp_optional` | Worker for the tp_optional task. The task definition uses timeoutPolicy: ALERT_ONLY. If this worker does not respond ... |
| **RetryableWorker** | `tp_retryable` | Worker for the tp_retryable task. The task definition uses timeoutPolicy: RETRY. If this worker does not respond with... |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
tp_critical
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
java -jar target/timeout-policies-1.0.0.jar
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
java -jar target/timeout-policies-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow timeout_policy_demo \
  --version 1 \
  --input '{"mode": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w timeout_policy_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles its task .  connect them to your real services, declare the timeout policy (TIME_OUT_WF, RETRY, or ALERT_ONLY) per task in config, and the differentiated timeout behavior stays the same.

- **CriticalWorker** (`tp_critical`): replace with your mission-critical task (payment processing, order confirmation) where a timeout must fail the workflow
- **OptionalWorker** (`tp_optional`): replace with a non-critical task (analytics, logging, enrichment) where timeouts should be tolerated
- **RetryableWorker** (`tp_retryable`): replace with a flaky dependency call (third-party API, legacy system) that benefits from retry on timeout

Replace each worker with your real services, and changing timeout policies is a JSON config change without touching any worker code.

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
timeout-policies/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/timeoutpolicies/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TimeoutPoliciesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CriticalWorker.java
│       ├── OptionalWorker.java
│       └── RetryableWorker.java
└── src/test/java/timeoutpolicies/workers/
    ├── CriticalWorkerTest.java        # 6 tests
    ├── OptionalWorkerTest.java        # 6 tests
    └── RetryableWorkerTest.java        # 6 tests
```
