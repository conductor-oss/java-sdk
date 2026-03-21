# Implementing Transient vs Permanent Error Detection in Java with Conductor :  Smart Error Classification for Retry Decisions

A Java Conductor workflow example demonstrating smart error classification .  a worker that distinguishes transient errors (network timeouts, 503s, connection resets) from permanent errors (404s, validation failures, authentication errors) and uses the appropriate failure strategy for each.

## The Problem

Not all errors are equal. A network timeout is transient .  retry and it will probably work. A 404 Not Found is permanent ,  retrying wastes time and resources. Your workers need to classify errors and signal to Conductor whether a retry makes sense. Retrying permanent errors wastes resources and delays failure detection. Not retrying transient errors causes unnecessary failures.

Without orchestration, error classification lives inside retry loops. Each caller decides independently whether to retry, using inconsistent criteria. Some retry 404s (wasteful), some don't retry 503s (premature failure). There's no centralized view of which errors are transient and which are permanent across the system.

## The Solution

The smart worker classifies each error as transient or permanent. For transient errors, it returns FAILED with a retryable flag so Conductor retries with backoff. For permanent errors, it returns FAILED_WITH_TERMINAL_ERROR so Conductor skips retries and fails immediately. This prevents wasted retries on permanent errors while ensuring transient errors get proper retry treatment. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

SmartTaskWorker classifies each error as transient (retryable via FAILED) or permanent (skip retries via FAILED_WITH_TERMINAL_ERROR), enabling Conductor to retry network timeouts and 503s while immediately failing on 404s and authentication errors.

| Worker | Task | What It Does |
|---|---|---|
| **SmartTaskWorker** | `tvp_smart_task` | Smart task worker (tvp_smart_task) that classifies errors as transient or permanent. Behavior based on the "errorType... |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
tvp_smart_task

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
java -jar target/transient-vs-permanent-1.0.0.jar

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
java -jar target/transient-vs-permanent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow transient_vs_permanent_demo \
  --version 1 \
  --input '{"errorType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w transient_vs_permanent_demo -s COMPLETED -c 5

```

## How to Extend

Each worker classifies real errors .  connect to your external APIs, use FAILED for transient errors (timeouts, 503s) and FAILED_WITH_TERMINAL_ERROR for permanent ones (404s, auth failures), and the smart retry-or-fail-fast behavior stays the same.

- **SmartTaskWorker** (`tvp_smart_task`): implement real error classification for your domain. HTTP status codes, database error codes, exception types, and return FAILED (retryable) or FAILED_WITH_TERMINAL_ERROR (permanent) accordingly

Implement real error classification for your domain's HTTP status codes or exception types, and the smart retry-or-fail-fast behavior works identically in production.

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
transient-vs-permanent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/transientvspermanent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TransientVsPermanentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── SmartTaskWorker.java
└── src/test/java/transientvspermanent/workers/
    └── SmartTaskWorkerTest.java        # 11 tests

```
