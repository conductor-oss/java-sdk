# Webhook Retry in Java Using Conductor

Webhook delivery workflow with DO_WHILE retry loop. Prepares the webhook, attempts delivery up to 3 times, checks each result, and records the final outcome. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to deliver webhook payloads to external URLs with automatic retry on failure. The workflow prepares the webhook payload, attempts delivery to the target URL, and retries up to a configurable number of times with backoff if delivery fails (network error, timeout, non-2xx response). After all attempts, the final outcome is recorded. Without retry, transient network issues cause permanent webhook delivery failures.

Without orchestration, you'd build a retry loop with exponential backoff, manually tracking attempt counts, handling different failure modes (connection refused vs. 500 vs: timeout), and persisting delivery status .  hoping the retry process itself does not crash and lose track of pending deliveries.

## The Solution

**You just write the payload-prepare, delivery-attempt, result-check, and outcome-recording workers. Conductor handles DO_WHILE retry loops, durable delivery state across attempts, and a complete record of every delivery attempt and final outcome.**

Each delivery concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of preparing the payload, attempting delivery in a DO_WHILE retry loop, checking each result, and recording the final outcome ,  with durable state that survives crashes and full visibility into every delivery attempt. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage retry-based delivery: PrepareWebhookWorker packages the payload, AttemptDeliveryWorker makes the HTTP call, CheckResultWorker evaluates the response, and RecordOutcomeWorker logs the final delivery status.

| Worker | Task | What It Does |
|---|---|---|
| **AttemptDeliveryWorker** | `wr_attempt_delivery` | Attempts to deliver the webhook payload to the target URL. Simulates transient failures for early attempts and succes... |
| **CheckResultWorker** | `wr_check_result` | Checks the result of a webhook delivery attempt. |
| **PrepareWebhookWorker** | `wr_prepare_webhook` | Prepares webhook delivery by validating and packaging the URL and payload. |
| **RecordOutcomeWorker** | `wr_record_outcome` | Records the final outcome of the webhook delivery process. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
wr_prepare_webhook
    │
    ▼
DO_WHILE
    └── wr_attempt_delivery
    └── wr_check_result
    │
    ▼
wr_record_outcome
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
java -jar target/webhook-retry-1.0.0.jar
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
java -jar target/webhook-retry-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow webhook_retry_wf \
  --version 1 \
  --input '{"webhookUrl": "https://example.com", "payload": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w webhook_retry_wf -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real HTTP delivery client, response validator, and delivery-status store, the prepare-attempt-check-record retry loop workflow stays exactly the same.

- **Payload preparer**: serialize the webhook payload, add headers (Content-Type, X-Webhook-ID, signature), and set timeout policies
- **Delivery worker**: make real HTTP POST calls to target URLs using HttpClient with configurable timeouts and connection pooling
- **Result checker**: implement response validation (status code checks, response body parsing) and decide whether to retry based on error type
- **Outcome recorder**: persist delivery outcomes (success, exhausted retries, permanent failure) to your webhook delivery tracking system

Connecting AttemptDeliveryWorker to a real HTTP client or changing the max retry count requires no changes to the delivery workflow.

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
webhook-retry/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webhookretry/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebhookRetryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AttemptDeliveryWorker.java
│       ├── CheckResultWorker.java
│       ├── PrepareWebhookWorker.java
│       └── RecordOutcomeWorker.java
└── src/test/java/webhookretry/workers/
    ├── AttemptDeliveryWorkerTest.java        # 8 tests
    ├── CheckResultWorkerTest.java        # 8 tests
    ├── PrepareWebhookWorkerTest.java        # 8 tests
    └── RecordOutcomeWorkerTest.java        # 8 tests
```
