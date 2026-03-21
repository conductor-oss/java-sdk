# Event Fanout in Java Using Conductor

Event fan-out workflow that receives an event, fans out to analytics, storage, and notification processing in parallel via FORK_JOIN, then aggregates the results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to distribute a single event to multiple downstream consumers simultaneously. When an event arrives, it must be processed by analytics (for tracking), storage (for persistence), and notification (for alerting). all in parallel so no single slow consumer delays the others. After all consumers finish, results must be aggregated into a unified response.

Without orchestration, you'd spawn threads for each consumer, manage a CountDownLatch or CompletableFuture for synchronization, handle partial failures when one consumer crashes while others succeed, and manually aggregate results from different threads.

## The Solution

**You just write the event-receive, analytics, storage, notification, and aggregation workers. Conductor handles parallel fan-out via FORK_JOIN, per-consumer retry isolation, and automatic aggregation after all consumers complete.**

Each downstream consumer is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of fanning out to all three consumers in parallel via FORK_JOIN, waiting for all to complete, aggregating results, retrying any failed consumer independently, and tracking the entire fanout operation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement parallel event distribution: ReceiveEventWorker ingests the event, then AnalyticsWorker, StorageWorker, and NotificationWorker process it simultaneously via FORK_JOIN, and AggregateWorker combines all outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `fo_aggregate` | Aggregates results from the three parallel fan-out branches. |
| **AnalyticsWorker** | `fo_analytics` | Tracks event analytics and updates metrics. |
| **NotificationWorker** | `fo_notification` | Sends a notification about the event. |
| **ReceiveEventWorker** | `fo_receive_event` | Receives an incoming event and passes it through for fan-out processing. |
| **StorageWorker** | `fo_storage` | Stores the event payload to a data lake. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
fo_receive_event
    │
    ▼
FORK_JOIN
    ├── fo_analytics
    ├── fo_storage
    └── fo_notification
    │
    ▼
JOIN (wait for all branches)
fo_aggregate

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
java -jar target/event-fanout-1.0.0.jar

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
java -jar target/event-fanout-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_fanout_wf \
  --version 1 \
  --input '{"eventId": "TEST-001", "eventType": "standard", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_fanout_wf -s COMPLETED -c 5

```

## How to Extend

Connect each branch worker to your real analytics service, data lake (S3, BigQuery), and notification channel (email, push), the parallel fan-out and aggregation workflow stays exactly the same.

- **Analytics processor**: send event data to your analytics platform (Segment, Mixpanel, Amplitude) for tracking and reporting
- **Storage processor**: persist events to your data lake (S3, BigQuery, Snowflake) or event store (EventStoreDB, Kafka)
- **Notification processor**: route alerts via Slack, PagerDuty, or email based on event severity and subscriber preferences

Adding a new downstream consumer means adding one worker and a FORK_JOIN branch. Existing consumers are unaffected.

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
event-fanout/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventfanout/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventFanoutExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AnalyticsWorker.java
│       ├── NotificationWorker.java
│       ├── ReceiveEventWorker.java
│       └── StorageWorker.java
└── src/test/java/eventfanout/workers/
    ├── AggregateWorkerTest.java        # 8 tests
    ├── AnalyticsWorkerTest.java        # 8 tests
    ├── NotificationWorkerTest.java        # 8 tests
    ├── ReceiveEventWorkerTest.java        # 9 tests
    └── StorageWorkerTest.java        # 8 tests

```
