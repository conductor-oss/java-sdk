# Publish-Subscribe in Java Using Conductor :  Publish Event, Fan Out to Subscribers in Parallel, Confirm

A Java Conductor workflow example for publish-subscribe .  publishing an event to a topic, fanning it out to multiple subscribers in parallel via `FORK_JOIN`, and confirming that all subscribers received and processed the event. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## One Event, Multiple Subscribers, All Must Succeed

A user signs up and three things need to happen: the welcome email service sends an onboarding email, the analytics service records the signup event, and the provisioning service creates the user's workspace. These are independent subscribers .  none depends on the others; but all three must eventually succeed. If the email service is down, the signup shouldn't block provisioning, and you need to know which subscribers processed the event and which didn't.

Building pub-sub fan-out manually means spawning threads for each subscriber, implementing independent retry logic for each, tracking which subscribers acknowledged, and deciding what to do when one fails permanently while the others succeeded.

## The Solution

**You write each subscriber's handler. Conductor handles parallel fan-out, per-subscriber retries, and delivery confirmation.**

`PbsPublishWorker` accepts the event and topic, preparing it for distribution. A `FORK_JOIN` fans the event out to all three subscribers in parallel .  `PbsSubscriber1Worker`, `PbsSubscriber2Worker`, and `PbsSubscriber3Worker` each process the event independently. The `JOIN` waits for all subscribers to complete. `PbsConfirmWorker` verifies that every subscriber processed the event and produces a confirmation summary. Conductor retries any subscriber that fails without affecting the others, and records which subscribers succeeded and which needed retries.

### What You Write: Workers

Five workers implement the pub-sub fan-out: event publishing, three parallel subscriber handlers (analytics, notification, audit), and delivery confirmation, each subscriber processing the event independently.

| Worker | Task | What It Does |
|---|---|---|
| **PbsConfirmWorker** | `pbs_confirm` | Verifies that all subscribers received the event and reports delivery status |
| **PbsPublishWorker** | `pbs_publish` | Publishes an event with a unique ID to the topic for all subscribers |
| **PbsSubscriber1Worker** | `pbs_subscriber_1` | Receives the event and logs analytics data |
| **PbsSubscriber2Worker** | `pbs_subscriber_2` | Receives the event and sends a notification |
| **PbsSubscriber3Worker** | `pbs_subscriber_3` | Receives the event and writes an audit log entry |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
pbs_publish
    │
    ▼
FORK_JOIN
    ├── pbs_subscriber_1
    ├── pbs_subscriber_2
    └── pbs_subscriber_3
    │
    ▼
JOIN (wait for all branches)
pbs_confirm
```

## Example Output

```
=== Publish-Subscribe Demo ===

Step 1: Registering task definitions...
  Registered: pbs_publish, pbs_subscriber_1, pbs_subscriber_2, pbs_subscriber_3, pbs_confirm

Step 2: Registering workflow 'pbs_publish_subscribe'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [confirm] Processing
  [publish] Processing
  [sub-1] Processing
  [sub-2] Processing
  [sub-3] Processing

  Status: COMPLETED
  Output: {subscribersNotified=..., allDelivered=..., eventId=..., published=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/publish-subscribe-1.0.0.jar
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
java -jar target/publish-subscribe-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pbs_publish_subscribe \
  --version 1 \
  --input '{"event": {"key": "value"}, "order_events": "sample-order-events"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pbs_publish_subscribe -s COMPLETED -c 5
```

## How to Extend

Each worker represents one subscriber's processing logic .  replace the simulated event handlers with real email, analytics, or provisioning service calls and the fan-out pipeline runs unchanged.

- **PbsPublishWorker** (`pbs_publish`): publish to a real SNS topic, Kafka topic, or Redis Pub/Sub channel instead of simulating the event publish
- **PbsSubscriber*Workers** (`pbs_subscriber_1/2/3`) .  implement real subscriber logic: send emails via SES, write analytics events to Segment/Mixpanel, provision resources via cloud APIs
- **PbsConfirmWorker** (`pbs_confirm`): query subscriber acknowledgment stores (DynamoDB, Redis) to verify all subscribers processed the event, and alert on permanent failures

Each subscriber's output contract stays fixed. Adding a new subscriber means adding one fork branch and one worker, not modifying existing subscribers.

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
publish-subscribe/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/publishsubscribe/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PublishSubscribeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PbsConfirmWorker.java
│       ├── PbsPublishWorker.java
│       ├── PbsSubscriber1Worker.java
│       ├── PbsSubscriber2Worker.java
│       └── PbsSubscriber3Worker.java
└── src/test/java/publishsubscribe/workers/
    ├── PbsConfirmWorkerTest.java        # 4 tests
    ├── PbsPublishWorkerTest.java        # 4 tests
    ├── PbsSubscriber1WorkerTest.java        # 4 tests
    ├── PbsSubscriber2WorkerTest.java        # 4 tests
    └── PbsSubscriber3WorkerTest.java        # 4 tests
```
