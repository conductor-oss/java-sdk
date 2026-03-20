# Implementing Error Classification in Java with Conductor :  Retryable vs Non-Retryable Error Routing

A Java Conductor workflow example demonstrating error classification .  distinguishing retryable errors (429 Too Many Requests, 503 Service Unavailable) from non-retryable errors (security-posture Bad Request) and routing each to the appropriate handling path. Uses [Conductor](https://github.## The Problem

You call an external API that returns different error codes .  429 (rate limited), 503 (service temporarily down), security-posture (bad request data). Each error type demands a different response: retryable errors should be retried with backoff, while non-retryable errors should be routed to an error handler that logs the issue, alerts the team, and prevents wasted retry attempts on requests that will never succeed.

Without orchestration, error classification is buried in nested if/else chains inside every API caller. Each service classifies errors differently, retries non-retryable errors wastefully, or fails to retry retryable ones. When a new error code appears, every caller must be updated independently.

## The Solution

**You just write the API call and error classification logic. Conductor handles SWITCH-based routing by error type, automatic retries for transient failures, and a full record of every classification decision showing which errors were retried and which were sent to the handler.**

The API call worker makes the request and classifies errors. Conductor's SWITCH task routes retryable errors back through retry logic and non-retryable errors to a dedicated error handler. Every error classification decision is recorded .  you can see exactly which errors were retried, which were routed to the handler, and what the outcome was. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ApiCallWorker makes the external request and classifies the response error type, then Conductor's SWITCH task routes retryable errors back through retry logic and non-retryable errors to ErrorHandlerWorker for logging and alerting.

| Worker | Task | What It Does |
|---|---|---|
| **ApiCallWorker** | `ec_api_call` | Worker for ec_api_call .  simulates different HTTP error codes based on the simulateError input parameter. Behavior by.. |
| **ErrorHandlerWorker** | `ec_handle_error` | Worker for ec_handle_error .  logs the error type and details from the upstream API call, then completes with a handle.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
ec_api_call
    │
    ▼
SWITCH (error_type_switch_ref)
    ├── non_retryable: ec_handle_error
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
java -jar target/error-classification-1.0.0.jar
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
java -jar target/error-classification-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow error_classification_demo \
  --version 1 \
  --input '{"triggerError": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w error_classification_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one error path .  connect the API caller to your real external service, the error handler to log to Splunk or alert via PagerDuty, and the call-classify-route workflow stays the same.

- **ApiCallWorker** (`ec_api_call`): call your real HTTP/gRPC service, returning the actual HTTP status code and error type (retryable vs non-retryable) for Conductor to route
- **ErrorHandlerWorker** (`ec_handle_error`): log errors to your observability stack (Datadog, PagerDuty), create incident tickets, or send alerts based on error type and severity

Replace the simulated API call with your real service endpoint, and the classification-driven routing operates in production unchanged.

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
error-classification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/errorclassification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ErrorClassificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApiCallWorker.java
│       └── ErrorHandlerWorker.java
└── src/test/java/errorclassification/workers/
    ├── ApiCallWorkerTest.java        # 9 tests
    └── ErrorHandlerWorkerTest.java        # 4 tests
```
