# Scheduled Event in Java Using Conductor

Sequential scheduled-event workflow: queue_event -> check_schedule -> wait_until_ready -> execute_event -> confirm. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process events at a scheduled future time. An event is queued with a target execution time, the system checks whether it is time to execute, waits until the scheduled moment, executes the event's action, and confirms completion. Use cases include scheduled notifications, time-delayed order cancellations, and appointment reminders. Executing before the scheduled time violates business requirements; losing the event during the wait period means it never fires.

Without orchestration, you'd store scheduled events in a database, poll for due events with a timer loop, execute them, and mark them complete .  manually handling clock drift, managing the polling interval vs: precision tradeoff, and recovering events that were due during a service outage.

## The Solution

**You just write the event-queue, schedule-check, wait, execute, and confirmation workers. Conductor handles durable scheduling that survives restarts, ordered post-wait execution, and a complete record of every scheduled event lifecycle.**

Each scheduling concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of queuing the event, checking the schedule, waiting durably until the execution time (surviving restarts), executing the event action, and confirming completion. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage scheduled execution: QueueEventWorker registers the event, CheckScheduleWorker calculates the delay, WaitUntilReadyWorker holds until the target time, ExecuteEventWorker runs the action, and ConfirmWorker stamps completion.

| Worker | Task | What It Does |
|---|---|---|
| **CheckScheduleWorker** | `se_check_schedule` | Checks the schedule and determines the delay until execution. |
| **ConfirmWorker** | `se_confirm` | Confirms that the scheduled event was executed successfully. |
| **ExecuteEventWorker** | `se_execute_event` | Executes the scheduled event. |
| **QueueEventWorker** | `se_queue_event` | Queues an event for scheduled execution. |
| **WaitUntilReadyWorker** | `se_wait_until_ready` | Waits until the scheduled event is ready for execution. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
se_queue_event
    │
    ▼
se_check_schedule
    │
    ▼
se_wait_until_ready
    │
    ▼
se_execute_event
    │
    ▼
se_confirm

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
java -jar target/scheduled-event-1.0.0.jar

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
java -jar target/scheduled-event-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow scheduled_event_wf \
  --version 1 \
  --input '{"eventId": "TEST-001", "payload": {"key": "value"}, "scheduledTime": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w scheduled_event_wf -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real job queue (Redis sorted sets, DynamoDB), scheduler with timezone awareness, and event execution logic, the queue-check-wait-execute-confirm scheduling workflow stays exactly the same.

- **Queue worker**: persist scheduled events to your database or job queue (Redis sorted sets, DynamoDB with TTL, database with scheduled_at column)
- **Schedule checker**: compute precise wait times with timezone awareness and DST handling
- **Wait worker**: use Conductor's WAIT task for durable, crash-resistant waits instead of Thread.sleep()
- **Executor**: implement the actual scheduled action (send reminder email, cancel expired order, trigger report generation)

Replacing the simulated wait with a real timer service or connecting ExecuteEventWorker to live job logic requires no pipeline changes.

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
scheduled-event/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/scheduledevent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ScheduledEventExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckScheduleWorker.java
│       ├── ConfirmWorker.java
│       ├── ExecuteEventWorker.java
│       ├── QueueEventWorker.java
│       └── WaitUntilReadyWorker.java
└── src/test/java/scheduledevent/workers/
    ├── CheckScheduleWorkerTest.java        # 8 tests
    ├── ConfirmWorkerTest.java        # 8 tests
    ├── ExecuteEventWorkerTest.java        # 8 tests
    ├── QueueEventWorkerTest.java        # 9 tests
    └── WaitUntilReadyWorkerTest.java        # 8 tests

```
