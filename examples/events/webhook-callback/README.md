# Webhook Callback in Java Using Conductor

Webhook Callback Workflow. receive an incoming webhook request, process the data, and notify the caller via callback URL. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to process an incoming webhook request and notify the caller of the result via a callback URL. The workflow receives the webhook payload, processes the data according to your business logic, and sends the result back to the caller's callback endpoint. If the callback fails, the caller never learns the outcome; if processing fails, you must still notify the caller of the failure.

Without orchestration, you'd handle the webhook in a servlet or controller, process the data inline, and make an HTTP POST to the callback URL. manually retrying failed callbacks, handling timeout and connection errors, and logging every callback attempt to debug delivery failures.

## The Solution

**You just write the request-receive, data-processing, and callback-notification workers. Conductor handles ordered request processing, callback delivery retry with backoff, and a durable record of every webhook callback attempt.**

Each webhook-callback concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the request, processing the data, and calling back the caller,  retrying failed callbacks with backoff, tracking every webhook's full lifecycle, and resuming if the process crashes after processing but before the callback. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers handle the callback lifecycle: ReceiveRequestWorker parses the incoming webhook, ProcessDataWorker applies business logic to the payload, and NotifyCallbackWorker posts the result back to the caller's URL.

| Worker | Task | What It Does |
|---|---|---|
| **NotifyCallbackWorker** | `wc_notify_callback` | Sends a callback notification to the partner's webhook URL with the processing result and completion status. |
| **ProcessDataWorker** | `wc_process_data` | Processes the parsed data from the received webhook request. Produces a processing result with record counts and status. |
| **ReceiveRequestWorker** | `wc_receive_request` | Receives an incoming webhook request, validates it, and parses the payload into a structured format for downstream pr... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
wc_receive_request
    │
    ▼
wc_process_data
    │
    ▼
wc_notify_callback

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
java -jar target/webhook-callback-1.0.0.jar

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
java -jar target/webhook-callback-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow webhook_callback_wf \
  --version 1 \
  --input '{"requestId": "TEST-001", "data": {"key": "value"}, "callbackUrl": "https://example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w webhook_callback_wf -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real webhook endpoint, processing logic, and HTTP callback client, the receive-process-notify callback workflow stays exactly the same.

- **Webhook receiver**: validate incoming webhook signatures (HMAC, JWT) and extract payload data
- **Processor**: implement your actual webhook processing logic (order updates, payment notifications, CI/CD triggers)
- **Callback sender**: make HTTP POST/PUT calls to callback URLs with configurable retry policies, timeout settings, and response validation

Replacing ProcessDataWorker with real business logic or pointing NotifyCallbackWorker at live callback URLs leaves the pipeline unchanged.

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
webhook-callback/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webhookcallback/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebhookCallbackExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── NotifyCallbackWorker.java
│       ├── ProcessDataWorker.java
│       └── ReceiveRequestWorker.java
└── src/test/java/webhookcallback/workers/
    ├── NotifyCallbackWorkerTest.java        # 9 tests
    ├── ProcessDataWorkerTest.java        # 10 tests
    └── ReceiveRequestWorkerTest.java        # 9 tests

```
