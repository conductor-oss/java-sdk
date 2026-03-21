# Implementing Linear Backoff Retry in Java with Conductor :  Linearly Increasing Delays Between Retries

A Java Conductor workflow example demonstrating linear backoff retry .  delays increase linearly with each attempt (delay * attempt number), providing a middle ground between fixed retries and aggressive exponential backoff.

## The Problem

You need retry behavior between fixed (same delay every time) and exponential (doubling delays). Linear backoff increases delays proportionally: 2s, 4s, 6s, 8s. This gives a recovering service progressively more time without the aggressive delay growth of exponential backoff, which can lead to very long waits after just a few retries.

Without orchestration, linear backoff means calculating delay * attemptNumber in each retry loop. Getting the math wrong means either too-aggressive retries (overwhelming the service) or too-conservative delays (unnecessary wait times).

## The Solution

The worker makes the call and returns success or failure. Conductor handles the linear backoff calculation automatically via the LINEAR_BACKOFF retry logic setting. Every retry attempt is recorded with the exact delay applied. Switching between linear, fixed, or exponential backoff is a config change. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

RetryLinearWorker makes the service call and returns success or failure, while Conductor applies linearly increasing delays (2s, 4s, 6s, 8s) between retries, a middle ground between fixed and exponential backoff.

| Worker | Task | What It Does |
|---|---|---|
| **RetryLinearWorker** | `retry_linear_task` | Worker that simulates a service that is unavailable for the first 3 attempts and succeeds on the 4th attempt, demonst... |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
retry_linear_task

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
java -jar target/retry-linear-1.0.0.jar

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
java -jar target/retry-linear-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow retry_linear_demo \
  --version 1 \
  --input '{"service": "order-service"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w retry_linear_demo -s COMPLETED -c 5

```

## How to Extend

Each worker calls a real service .  connect to your API or database, configure LINEAR_BACKOFF in the task definition, and the linearly increasing retry delays stay the same.

- **RetryLinearWorker** (`retry_linear_task`): replace with your API call where linear backoff is the right strategy .  services with predictable recovery curves, batch job queues, rate-limited endpoints with known cooldown periods

Replace with your real API call, and switching between linear, fixed, or exponential backoff is a single config change in the task definition.

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
retry-linear/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/retrylinear/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RetryLinearExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── RetryLinearWorker.java
└── src/test/java/retrylinear/workers/
    └── RetryLinearWorkerTest.java        # 8 tests

```
