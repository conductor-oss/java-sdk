# Event Ttl in Java Using Conductor

Event TTL workflow that checks if an event has expired, processes it if still valid, or logs it if the TTL has passed. Uses SWITCH to branch on expiry status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to enforce time-to-live (TTL) on events so expired events are discarded rather than processed. When an event arrives, the workflow checks whether it was created within the allowed TTL window. If still valid, it proceeds to processing; if expired, it is logged and discarded. Processing stale events (e.g., a price update from 2 hours ago) can produce incorrect results when the data has already been superseded.

Without orchestration, you'd embed TTL checks in every consumer, calculate expiry from timestamps manually, route expired events with if/else, and risk inconsistent TTL enforcement across different consumers that use different clock sources or tolerance windows.

## The Solution

**You just write the expiry-check, event-processing, expired-logging, and acknowledgment workers. Conductor handles expiry-based SWITCH routing, guaranteed processing of valid events, and a complete record of every TTL decision.**

Each TTL concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of checking the event's age against its TTL, routing via a SWITCH task to processing (valid) or logging (expired), retrying processing if it fails, and tracking every event's TTL decision. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers enforce event expiration: CheckExpiryWorker evaluates the event's age against its TTL, ProcessEventWorker handles valid events, LogExpiredWorker discards stale ones, and AcknowledgeWorker confirms successful processing.

| Worker | Task | What It Does |
|---|---|---|
| **AcknowledgeWorker** | `xl_acknowledge` | Acknowledges a successfully processed event. |
| **CheckExpiryWorker** | `xl_check_expiry` | Checks whether an event has exceeded its TTL. |
| **LogExpiredWorker** | `xl_log_expired` | Logs an expired event that exceeded its TTL. |
| **ProcessEventWorker** | `xl_process_event` | Processes a valid (non-expired) event. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
xl_check_expiry
    │
    ▼
SWITCH (switch_ref)
    ├── valid: xl_process_event -> xl_acknowledge
    ├── expired: xl_log_expired
```

## Example Output

```
=== Event TTL Demo ===

Step 1: Registering task definitions...
  Registered: xl_check_expiry, xl_process_event, xl_acknowledge, xl_log_expired

Step 2: Registering workflow 'event_ttl'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [xl_acknowledge] Acknowledged event
  [xl_check_expiry] Event
  [xl_log_expired] Event
  [xl_process_event] Processing valid event

  Status: COMPLETED
  Output: {acknowledged=..., ageSeconds=..., ttlSeconds=..., logged=...}

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
java -jar target/event-ttl-1.0.0.jar
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
java -jar target/event-ttl-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_ttl \
  --version 1 \
  --input '{"eventId": "ttl-evt-400", "ttl-evt-400": "payload", "payload": {"key": "value"}, "2026-01-15T09:58:00Z": "sample-2026-01-15T09:58:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_ttl -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real TTL checker (clock + event timestamp), event processing pipeline, and expired-event archive, the check-expiry-process-or-log workflow stays exactly the same.

- **TTL checker**: implement precise TTL calculation using NTP-synchronized clocks, handle clock skew across distributed producers
- **Event processor**: implement your actual processing logic for valid events
- **Expired event handler**: route expired events to a dead-letter topic, emit metrics for TTL expiration rates, and alert on spikes that indicate producer delays

Adjusting the TTL window or expiration logic inside CheckExpiryWorker requires no changes to the TTL routing workflow.

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
event-ttl/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventttl/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventTtlExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcknowledgeWorker.java
│       ├── CheckExpiryWorker.java
│       ├── LogExpiredWorker.java
│       └── ProcessEventWorker.java
└── src/test/java/eventttl/workers/
    ├── AcknowledgeWorkerTest.java        # 8 tests
    ├── CheckExpiryWorkerTest.java        # 10 tests
    ├── LogExpiredWorkerTest.java        # 8 tests
    └── ProcessEventWorkerTest.java        # 8 tests
```
