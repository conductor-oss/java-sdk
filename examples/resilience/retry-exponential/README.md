# Implementing Exponential Backoff Retry in Java with Conductor :  Doubling Delays for Rate-Limited APIs

A Java Conductor workflow example demonstrating exponential backoff retry .  a worker that simulates 429 (Too Many Requests) errors, with Conductor automatically retrying using doubling delays (1s, 2s, 4s, 8s) to give the rate-limited API time to recover.

## The Problem

You call an API that enforces rate limits and returns 429 errors when you exceed them. Retrying immediately makes the situation worse .  you burn through your rate limit quota even faster. Exponential backoff gives the API progressively more time to recover: wait 1 second after the first failure, 2 seconds after the second, 4 seconds after the third, and so on.

Without orchestration, exponential backoff means Thread.sleep() calls with manual delay calculations inside retry loops. Each API caller implements backoff differently, some use linear delays, some forget to cap the maximum delay, and none of them track the retry history for debugging.

## The Solution

The worker makes the API call and returns success or failure. Conductor handles the exponential backoff automatically .  configured via retryDelaySeconds and backoffRate in the task definition. Every retry attempt is recorded with the exact delay applied, so you can see the backoff progression. Changing the backoff rate or max retries is a config change, not a code change. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

RetryExpoTaskWorker makes the API call and reports success or failure, while Conductor automatically applies doubling delays (1s, 2s, 4s, 8s) between retries to give the rate-limited service time to recover.

| Worker | Task | What It Does |
|---|---|---|
| **RetryExpoTaskWorker** | `retry_expo_task` | Simulates an API that returns 429 (Too Many Requests) for the first 2 calls, then succeeds on the 3rd attempt. Conduc... |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
retry_expo_task
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
java -jar target/retry-exponential-1.0.0.jar
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
java -jar target/retry-exponential-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow retry_expo_demo \
  --version 1 \
  --input '{"apiUrl": "https://example.com"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w retry_expo_demo -s COMPLETED -c 5
```

## How to Extend

Each worker calls a real service .  connect to your rate-limited API, configure retryDelaySeconds and backoffRate in the task definition, and the exponential backoff retry behavior stays the same.

- **RetryExpoTaskWorker** (`retry_expo_task`): replace with your real rate-limited API call (Stripe, Twilio, Twitter, any API with 429 responses) .  the exponential backoff is pure configuration

Replace with your real rate-limited API call, and the exponential backoff behavior is purely a task definition setting requiring no code changes.

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
retry-exponential/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/retryexponential/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RetryExponentialExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── RetryExpoTaskWorker.java
└── src/test/java/retryexponential/workers/
    └── RetryExpoTaskWorkerTest.java        # 12 tests
```
