# CDC Pipeline in Java Using Conductor

A customer updates their shipping address at 2:03 PM. The downstream cache still shows the old address at 2:18 PM because the sync job runs on a 15-minute cron. The warehouse ships to the wrong address. The real-time price update your marketing team pushed to the products table at 11:00 AM doesn't reach the storefront until 11:15. after 200 customers have already checked out at the old price. Every minute your CDC pipeline lags is a minute your downstream systems are lying to users. This example builds a change-data-capture pipeline with Conductor that detects INSERTs, UPDATEs, and DELETEs from a source table, transforms them into structured events, publishes downstream, and confirms delivery, all orchestrated with retries and a full audit trail. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to capture every INSERT, UPDATE, and DELETE from a source database table and propagate those changes downstream as structured events. The pipeline must detect changes since a given timestamp, transform raw change records into normalized event payloads (with entity IDs, before/after values, and operation types), publish them to a downstream topic, and confirm that every message was successfully delivered. Missing a change means downstream systems go out of sync; publishing without confirmation means you cannot guarantee delivery.

Without orchestration, you'd build a single CDC polling service that queries the database change log, transforms rows inline, pushes to Kafka, and checks consumer offsets. Manually handling partial publishes when the broker is unavailable, retrying failed deliveries without re-publishing duplicates, and logging every step to debug why downstream data is stale.

## The Solution

**You just write the change-detection, transform, publish, and delivery-confirmation workers. Conductor handles pipeline sequencing, automatic retry when the broker is unavailable, and a durable record of every CDC run.**

Each CDC concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (detect changes, transform, publish, confirm), retrying when the message broker is temporarily unavailable, tracking every pipeline run with full change-record details, and resuming from the last successful step if the process crashes mid-publish. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the CDC pipeline: DetectChangesWorker polls a source table for inserts, updates, and deletes; TransformChangesWorker normalizes raw change records into structured events; PublishDownstreamWorker sends them to a topic; and ConfirmDeliveryWorker verifies all messages landed.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmDeliveryWorker** | `cd_confirm_delivery` | Confirms that all published CDC messages were successfully delivered. Returns a deterministic delivery report. |
| **DetectChangesWorker** | `cd_detect_changes` | Detects CDC changes from a source table since a given timestamp. Returns a set of 4 change records: INSERT, UPD |
| **PublishDownstreamWorker** | `cd_publish_downstream` | Publishes transformed CDC changes to a downstream topic. Returns fixed message IDs for deterministic behavior. |
| **TransformChangesWorker** | `cd_transform_changes` | Transforms raw CDC change records into structured event payloads with eventType, entityId, payload, previousPayload,  |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
cd_detect_changes
    │
    ▼
cd_transform_changes
    │
    ▼
cd_publish_downstream
    │
    ▼
cd_confirm_delivery

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
java -jar target/cdc-pipeline-1.0.0.jar

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
java -jar target/cdc-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cdc_pipeline_wf \
  --version 1 \
  --input '{"sourceTable": "users", "sinceTimestamp": "2026-03-08T10:00:00Z", "targetTopic": "cdc.users.changes"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cdc_pipeline_wf -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real database change log (Debezium, DMS), transformation logic, and Kafka broker, the detect-transform-publish-confirm CDC pipeline workflow stays exactly the same.

- **DetectChangesWorker** (`cd_detect_changes`): read from your database's change log (Debezium connector for MySQL/Postgres binlog, DynamoDB Streams, or SQL Server Change Tracking)
- **TransformChangesWorker** (`cd_transform_changes`): normalize change records into your downstream event schema; handle schema evolution and field mapping
- **PublishDownstreamWorker** (`cd_publish_downstream`): publish events to Kafka, AWS SNS/SQS, or Google Pub/Sub using the appropriate client SDK
- **ConfirmDeliveryWorker** (`cd_confirm_delivery`): verify consumer offsets or check delivery receipts to confirm all messages reached their destination

Switching from a simulated change log to Debezium or AWS DMS requires no modifications to the pipeline workflow.

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
cdc-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cdcpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CdcPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmDeliveryWorker.java
│       ├── DetectChangesWorker.java
│       ├── PublishDownstreamWorker.java
│       └── TransformChangesWorker.java
└── src/test/java/cdcpipeline/workers/
    ├── ConfirmDeliveryWorkerTest.java        # 9 tests
    ├── DetectChangesWorkerTest.java        # 10 tests
    ├── PublishDownstreamWorkerTest.java        # 8 tests
    └── TransformChangesWorkerTest.java        # 10 tests

```
