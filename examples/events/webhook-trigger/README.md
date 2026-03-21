# Webhook Trigger in Java Using Conductor

Webhook Trigger .  process incoming webhook event, validate payload, transform data, and store the result through a sequential pipeline. Uses [Conductor](https://github.

## The Problem

You need to process incoming webhook events through a validation and transformation pipeline. When a webhook arrives, the payload must be validated for required fields and correct structure, transformed into your internal data format, and stored for downstream consumption. Storing invalid or untransformed webhook data pollutes your data store and breaks downstream consumers.

Without orchestration, you'd handle webhooks in a controller that validates, transforms, and stores inline .  mixing validation logic with transformation logic with persistence logic, manually handling validation failures, and logging every step to debug why a webhook's data was stored incorrectly.

## The Solution

**You just write the event-process, payload-validate, data-transform, and store workers. Conductor handles sequential validation-to-storage execution, retry on storage failures, and full webhook processing traceability.**

Each webhook concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of processing the event, validating the payload, transforming the data, and storing the result ,  retrying if the storage backend is unavailable, tracking every webhook through the pipeline, and resuming if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the webhook ingestion pipeline: ProcessEventWorker parses the incoming payload, ValidatePayloadWorker checks required fields and structure, TransformDataWorker converts to internal format, and StoreResultWorker persists the result.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessEventWorker** | `wt_process_event` | Processes an incoming webhook event by extracting and parsing the payload into structured data with a known schema. |
| **StoreResultWorker** | `wt_store_result` | Stores the transformed data into the target destination and returns a confirmation record with the storage details. |
| **TransformDataWorker** | `wt_transform_data` | Transforms validated data into a canonical format suitable for storage, normalizing field names and structure. |
| **ValidatePayloadWorker** | `wt_validate_payload` | Validates the parsed event payload against the expected schema, performing structural and value checks. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
wt_process_event
    │
    ▼
wt_validate_payload
    │
    ▼
wt_transform_data
    │
    ▼
wt_store_result

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
java -jar target/webhook-trigger-1.0.0.jar

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
java -jar target/webhook-trigger-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow webhook_trigger_wf \
  --version 1 \
  --input '{"eventType": "standard", "payload": {"key": "value"}, "source": "api", "timestamp": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w webhook_trigger_wf -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real webhook parser, JSON Schema validator, data transformation engine, and storage backend (database, S3, data lake), the process-validate-transform-store trigger workflow stays exactly the same.

- **Event processor**: extract webhook metadata (source, event type, timestamp) and normalize across different webhook providers (Stripe, GitHub, Twilio)
- **Payload validator**: validate against JSON Schema or custom rules; reject malformed webhooks with descriptive error responses
- **Data transformer**: map webhook payloads to your internal event schema using a transformation engine or custom mappers
- **Storage worker**: persist transformed events to your event store (database, S3, Kafka) with idempotency keys to handle duplicate deliveries

Connecting ValidatePayloadWorker to a real schema or StoreResultWorker to a production database requires no pipeline modifications.

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
webhook-trigger/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webhooktrigger/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebhookTriggerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessEventWorker.java
│       ├── StoreResultWorker.java
│       ├── TransformDataWorker.java
│       └── ValidatePayloadWorker.java
└── src/test/java/webhooktrigger/workers/
    ├── ProcessEventWorkerTest.java        # 9 tests
    ├── StoreResultWorkerTest.java        # 9 tests
    ├── TransformDataWorkerTest.java        # 9 tests
    └── ValidatePayloadWorkerTest.java        # 9 tests

```
