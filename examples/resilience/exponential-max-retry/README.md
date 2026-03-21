# Implementing Exponential Backoff with Max Retries in Java with Conductor :  Unreliable API Calls with Dead Letter Fallback

A Java Conductor workflow example demonstrating exponential backoff retry with a maximum retry limit. calling an unreliable API with doubling delays between retries, and routing to a dead letter handler when all retries are exhausted.

## The Problem

You call an external API that intermittently fails. rate limits, temporary outages, network blips. Retrying immediately causes a thundering herd that makes things worse. You need exponential backoff (1s, 2s, 4s, 8s) to give the service time to recover. But you also need a maximum retry limit,  if the API is down for hours, you can't retry forever. After exhausting retries, the failed request must be captured in a dead letter handler rather than silently lost.

Without orchestration, exponential backoff means writing retry loops with Thread.sleep(), tracking attempt counts, and manually routing to a dead letter queue after max retries. Each API caller implements backoff slightly differently, some forget the max retry cap, and dead letter routing is often missing entirely.

## The Solution

**You just write the API call and dead letter capture logic. Conductor handles exponential backoff retries with configurable delay and max attempts, automatic dead-letter routing when retries are exhausted, and a complete log of every retry attempt with timing and error details.**

The unreliable API worker makes the call and reports success or failure. Conductor handles exponential backoff retries automatically. configured per task with retry count, delay, and backoff rate. When retries are exhausted, the failure workflow routes to the dead letter handler. Every retry attempt is tracked with timing and error details. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

UnreliableApiWorker makes the external call and reports success or failure, while DeadLetterLogWorker captures permanently failed requests after all retry attempts are exhausted.

| Worker | Task | What It Does |
|---|---|---|
| **DeadLetterLogWorker** | `emr_dead_letter_log` | Worker for emr_dead_letter_log. logs details of a failed workflow. Receives failed workflow details (workflowId, rea.. |
| **UnreliableApiWorker** | `emr_unreliable_api` | Worker for emr_unreliable_api. simulates an unreliable API call. If shouldSucceed=true, returns success with status=.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
emr_unreliable_api

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
java -jar target/exponential-max-retry-1.0.0.jar

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
java -jar target/exponential-max-retry-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow emr_exponential_max_retry \
  --version 1 \
  --input '{"shouldSucceed": "sample-shouldSucceed"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w emr_exponential_max_retry -s COMPLETED -c 5

```

## How to Extend

Each worker does one thing. connect the API worker to your real external service, the dead letter handler to a durable store (SQS DLQ, database table), and the retry-with-backoff-then-dead-letter workflow stays the same.

- **DeadLetterLogWorker** (`emr_dead_letter_log`): persist failed requests to DynamoDB/Postgres for manual review, alert ops via Slack/PagerDuty, or publish to an SQS dead letter queue
- **UnreliableApiWorker** (`emr_unreliable_api`): replace with your real API call (payment gateway, shipping API, external data feed). the worker just makes the call and returns success/failure

Swap in your real API call and dead-letter store, and the exponential backoff with dead-letter routing carries over with no workflow changes.

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
exponential-max-retry/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/exponentialmaxretry/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExponentialMaxRetryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeadLetterLogWorker.java
│       └── UnreliableApiWorker.java
└── src/test/java/exponentialmaxretry/workers/
    ├── DeadLetterLogWorkerTest.java        # 7 tests
    └── UnreliableApiWorkerTest.java        # 8 tests

```
