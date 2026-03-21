# Delayed Event in Java Using Conductor

Delayed event processing workflow that receives an event, computes a delay, applies it, processes the event, and logs completion. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process events after a configurable delay. When an event arrives, the system must compute the appropriate wait time (based on business rules, priority, or a fixed delay), hold the event for that duration, and then process it. Use cases include scheduled notifications, rate-limited API calls, and time-delayed order confirmations. Processing an event before its delay expires violates business timing requirements.

Without orchestration, you'd build a delay queue with Thread.sleep() or scheduled executors, manually tracking which events are waiting, handling JVM restarts that lose in-memory timers, and logging everything to debug why a delayed notification fired immediately or not at all.

## The Solution

**You just write the event-receive, delay-compute, delay-apply, and event-processing workers. Conductor handles durable delay that survives restarts, ordered post-delay execution, and full lifecycle tracking per event.**

Each delayed-processing concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the event, computing the delay, waiting the specified duration (durably,  surviving restarts), processing the event after the delay, and logging completion. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage time-delayed processing: ReceiveEventWorker ingests the event, ComputeDelayWorker calculates the wait duration, ApplyDelayWorker holds execution, ProcessEventWorker handles the event after the delay, and LogCompletionWorker records the outcome.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyDelayWorker** | `de_apply_delay` | Applies the computed delay (simulated, instant in mock). |
| **ComputeDelayWorker** | `de_compute_delay` | Computes the delay duration in milliseconds from delaySeconds. |
| **LogCompletionWorker** | `de_log_completion` | Logs the completion of the delayed event processing. |
| **ProcessEventWorker** | `de_process_event` | Processes the delayed event. |
| **ReceiveEventWorker** | `de_receive_event` | Receives an incoming event and marks it as received. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
de_receive_event
    │
    ▼
de_compute_delay
    │
    ▼
de_apply_delay
    │
    ▼
de_process_event
    │
    ▼
de_log_completion

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
java -jar target/delayed-event-1.0.0.jar

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
java -jar target/delayed-event-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow delayed_event \
  --version 1 \
  --input '{"eventId": "TEST-001", "payload": {"key": "value"}, "delaySeconds": "sample-delaySeconds"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w delayed_event -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real event source, delay-computation rules, and downstream processing logic, the receive-delay-process workflow stays exactly the same.

- **DeReceiveEventWorker** (`de_receive_event`): consume events from your message broker and extract timing metadata
- **DeComputeDelayWorker** (`de_compute_delay`): implement business logic for dynamic delay calculation (priority-based, rate-limit backoff, scheduled delivery windows)
- **DeApplyDelayWorker** (`de_apply_delay`): use Conductor's WAIT task or a timer service for example-grade durable delays that survive process restarts
- **DeProcessEventWorker** (`de_process_event`): execute the actual delayed action (send notification, trigger API call, update database)
- **DeLogCompletionWorker** (`de_log_completion`): record completion metrics to your observability platform for delay accuracy monitoring

Replacing the delay mechanism with a real timer service or changing the delay computation preserves the receive-delay-process pipeline.

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
delayed-event/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/delayedevent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DelayedEventExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyDelayWorker.java
│       ├── ComputeDelayWorker.java
│       ├── LogCompletionWorker.java
│       ├── ProcessEventWorker.java
│       └── ReceiveEventWorker.java
└── src/test/java/delayedevent/workers/
    ├── ApplyDelayWorkerTest.java        # 8 tests
    ├── ComputeDelayWorkerTest.java        # 8 tests
    ├── LogCompletionWorkerTest.java        # 8 tests
    ├── ProcessEventWorkerTest.java        # 8 tests
    └── ReceiveEventWorkerTest.java        # 8 tests

```
