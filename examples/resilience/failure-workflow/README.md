# Implementing Failure Workflow in Java with Conductor :  Automatic Error Handling When a Workflow Fails

A Java Conductor workflow example demonstrating Conductor's failure workflow feature. when the main processing workflow fails, a separate error handler workflow runs automatically to perform cleanup and send notifications.

## The Problem

You have a processing pipeline that can fail. When it does, you need automatic cleanup. release held resources, rollback partial changes, notify the team. The cleanup must happen reliably even if the main process crashed unexpectedly. And you need it to happen automatically, not rely on someone manually triggering a recovery script.

Without orchestration, failure handling lives in finally blocks and shutdown hooks that may not run if the process crashes. Cleanup logic is interleaved with business logic, making both harder to understand and test. Nobody can tell whether cleanup ran for a given failure.

## The Solution

**You just write the processing logic and error cleanup handlers. Conductor handles automatic failure detection, triggering the cleanup workflow with full error context, retries on cleanup steps, and tracking of every failure with its cleanup outcome.**

The main workflow runs the processing step. Conductor's failure workflow feature automatically triggers a separate error handler workflow when the main one fails. running cleanup and notification workers without any manual intervention. The failure workflow receives the original workflow's context (what failed, why, at which step), so cleanup workers have full information. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ProcessWorker executes the main business logic, and when it fails, Conductor's failure workflow automatically triggers CleanupWorker to release resources and NotifyFailureWorker to alert the team, all with the original failure context.

| Worker | Task | What It Does |
|---|---|---|
| **CleanupWorker** | `fw_cleanup` | Cleanup worker for the failure handler workflow. Runs after the main workflow fails, performing cleanup operations. R... |
| **NotifyFailureWorker** | `fw_notify_failure` | Notification worker for the failure handler workflow. Sends a failure alert after cleanup is done. Returns { notified... |
| **ProcessWorker** | `fw_process` | Processes the main workflow task. Takes a shouldFail flag. if true, fails the task to trigger the failure workflow. .. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
fw_process

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
java -jar target/failure-workflow-1.0.0.jar

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
java -jar target/failure-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow main_with_failure_handler \
  --version 1 \
  --input '{"shouldFail": "sample-shouldFail"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w main_with_failure_handler -s COMPLETED -c 5

```

## How to Extend

Each worker handles one concern. connect the processing worker to your real business logic, the cleanup worker to release held resources, the notifier to Slack or PagerDuty, and the process-plus-auto-cleanup-on-failure workflow stays the same.

- **CleanupWorker** (`fw_cleanup`): release held resources (database locks, reserved inventory, temporary files), rollback partial transactions
- **NotifyFailureWorker** (`fw_notify_failure`): send real failure alerts via Slack, PagerDuty, or email with the original workflow's error context and stack trace
- **ProcessWorker** (`fw_process`): replace with your real processing logic. data transformation, order fulfillment, batch job execution

Connect the processing worker to your real business logic and the cleanup worker to your resource management system, and the automatic failure handling works without modification.

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
failure-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/failureworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FailureWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CleanupWorker.java
│       ├── NotifyFailureWorker.java
│       └── ProcessWorker.java
└── src/test/java/failureworkflow/workers/
    ├── CleanupWorkerTest.java        # 4 tests
    ├── NotifyFailureWorkerTest.java        # 4 tests
    └── ProcessWorkerTest.java        # 7 tests

```
