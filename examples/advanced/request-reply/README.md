# Request-Reply in Java Using Conductor :  Send Request, Wait for Response, Correlate, Deliver

A Java Conductor workflow example for the request-reply pattern .  sending an asynchronous request to a target service, waiting for the response with a configurable timeout, correlating the response back to the original request, and delivering the result to the caller. Uses [Conductor](https://github.

## Asynchronous Request-Reply Needs Correlation and Timeout Handling

You send a credit check request to an external bureau, but the response comes back asynchronously .  minutes later, on a different channel (a callback webhook or a reply queue). Your system needs to hold the request context, wait for the response within a timeout window, match the response to the original request using a correlation ID, and deliver the result. If the response never arrives, you need to handle the timeout gracefully rather than waiting forever.

Building request-reply manually means storing pending requests in a database, implementing a timeout mechanism (scheduled tasks, TTLs), matching responses to requests by correlation ID, and handling edge cases like duplicate responses or responses that arrive after the timeout.

## The Solution

**You write the send and correlate logic. Conductor handles timeout management, retries, and response matching.**

`RqrSendRequestWorker` dispatches the request to the target service with a correlation ID and the configured timeout. `RqrWaitResponseWorker` waits for the response, respecting the timeout window. `RqrCorrelateWorker` matches the incoming response to the original request using the correlation ID, verifying they belong together. `RqrDeliverWorker` returns the correlated result to the caller. Conductor manages the timeout, retries the send if it fails, and records the full request-response lifecycle .  send time, wait duration, correlation match, and delivery status.

### What You Write: Workers

Four workers manage the request-response lifecycle: sending with a correlation ID, waiting with a timeout, response matching, and result delivery, each handling one phase of asynchronous communication.

| Worker | Task | What It Does |
|---|---|---|
| **RqrCorrelateWorker** | `rqr_correlate` | Matches the response to the original request by comparing correlation IDs |
| **RqrDeliverWorker** | `rqr_deliver` | Delivers the correlated response to the caller if the correlation matched |
| **RqrSendRequestWorker** | `rqr_send_request` | Sends the request with a unique correlation ID and reply queue address |
| **RqrWaitResponseWorker** | `rqr_wait_response` | Waits for a response on the reply queue and returns it with latency measurements |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
rqr_send_request
    │
    ▼
rqr_wait_response
    │
    ▼
rqr_correlate
    │
    ▼
rqr_deliver

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
java -jar target/request-reply-1.0.0.jar

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
java -jar target/request-reply-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rqr_request_reply \
  --version 1 \
  --input '{"requestPayload": {"key": "value"}, "targetService": "production", "timeoutMs": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rqr_request_reply -s COMPLETED -c 5

```

## How to Extend

Each worker manages one phase of the request-response lifecycle .  replace the simulated send and wait calls with real HTTP or callback-based service integrations and the correlation logic runs unchanged.

- **RqrSendRequestWorker** (`rqr_send_request`): send real HTTP requests to external services (credit bureaus, payment processors), publish to a request queue (SQS, RabbitMQ), or call gRPC endpoints
- **RqrWaitResponseWorker** (`rqr_wait_response`): use Conductor's WAIT task type for real async waiting, or poll a response queue/webhook callback store with configurable timeout
- **RqrCorrelateWorker** (`rqr_correlate`): look up the original request in a correlation store (Redis hash, DynamoDB) by correlation ID and merge the response data with the request context

The correlation and response contract stays fixed. Swap the simulated service call for a real HTTP callback or reply queue and the send-wait-correlate pipeline runs unchanged.

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
request-reply/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/requestreply/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RequestReplyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── RqrCorrelateWorker.java
│       ├── RqrDeliverWorker.java
│       ├── RqrSendRequestWorker.java
│       └── RqrWaitResponseWorker.java
└── src/test/java/requestreply/workers/
    ├── RqrCorrelateWorkerTest.java        # 4 tests
    ├── RqrDeliverWorkerTest.java        # 4 tests
    ├── RqrSendRequestWorkerTest.java        # 4 tests
    └── RqrWaitResponseWorkerTest.java        # 4 tests

```
