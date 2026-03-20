# Implementing Retry with Jitter in Java with Conductor :  Avoiding Thundering Herd on Retries

A Java Conductor workflow example demonstrating jitter in retry delays .  adding randomized delay before API calls to prevent multiple workers from retrying simultaneously and overwhelming a recovering service (the thundering herd problem).

## The Problem

When a shared dependency fails, all workers retry at the same time .  the dependency recovers, gets slammed by simultaneous retry requests, and fails again. This cycle repeats indefinitely. Jitter adds a random offset to each retry delay so workers spread their retries over time, giving the dependency a chance to handle them gradually.

Without orchestration, implementing jitter means adding Random.nextInt() to Thread.sleep() calculations in every retry loop. Each worker implements jitter differently (or not at all), the jitter range varies, and there's no visibility into the actual delays applied.

## The Solution

The worker makes the API call with a jitter delay built into its logic to spread concurrent retries. Conductor tracks each execution with timing, so you can verify that retries are spread across time rather than clustered. The thundering herd is avoided without complex coordination between workers. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

JitterApiCallWorker adds a randomized delay before each API call to spread concurrent retries over time, preventing the thundering herd problem where multiple workers slam a recovering service simultaneously.

| Worker | Task | What It Does |
|---|---|---|
| **JitterApiCallWorker** | `jitter_api_call` | Worker for jitter_api_call .  adds a deterministic jitter delay before processing to avoid the thundering herd problem.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
jitter_api_call
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
java -jar target/retry-jitter-1.0.0.jar
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
java -jar target/retry-jitter-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow retry_jitter_demo \
  --version 1 \
  --input '{"endpoint": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w retry_jitter_demo -s COMPLETED -c 5
```

## How to Extend

Each worker calls a real shared dependency .  connect to your external API or database, add jitter to the retry delay, and the thundering-herd prevention stays the same.

- **JitterApiCallWorker** (`jitter_api_call`): replace with your real API call that's shared across many concurrent workers .  the jitter prevents all workers from retrying a recovering service at the same instant

Swap in your real shared-dependency API call, and the jitter-based retry spreading prevents thundering herd without any orchestration changes.

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
retry-jitter/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/retryjitter/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RetryJitterExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── JitterApiCallWorker.java
└── src/test/java/retryjitter/workers/
    └── JitterApiCallWorkerTest.java        # 9 tests
```
