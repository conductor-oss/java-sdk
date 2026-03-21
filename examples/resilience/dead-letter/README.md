# Implementing Dead Letter Queue Pattern in Java with Conductor: Capture Poison Messages Instead of Losing Them

A Java Conductor workflow example demonstrating the dead letter queue pattern. Processing messages where some will inevitably fail (malformed data, missing fields, permanently unavailable dependencies), and routing persistently failed items to a dead letter handler for investigation instead of losing them silently.

## The Problem

You have a message processing pipeline where some messages are "poison". They will never succeed no matter how many times you retry. A malformed JSON payload, a reference to a deleted customer, a request for a discontinued product. After exhausting retries, those failed messages need to be captured somewhere durable (a dead letter queue) with full context about why they failed, instead of being silently dropped.

### What Goes Wrong Without Dead Letter Handling

Consider an order processing pipeline that ingests events from a queue:

1. Message arrives: `{orderId: "ORD-456", customerId: "DELETED-CUST"}`
2. Worker tries to process. **FAILS** (customer not found in database)
3. Conductor retries 3 times. **FAILS** each time (same error, customer is permanently deleted)
4. Message is exhausted and discarded

Without dead letter handling:
- The order is silently lost. Nobody knows it failed
- The customer may have already been charged but never received their order
- The operations team has no record of the failure to investigate
- The same class of error keeps happening without anyone noticing the pattern

With the dead letter pattern, the failed message and its full context (original input, error details, retry count, timestamps) are routed to a dead letter handler workflow. Operations gets an alert, the message is persisted for manual review, and patterns of failure become visible.

## The Solution

**You just write the message processor and dead letter handler. Conductor handles retry exhaustion detection, automatic routing of poison messages to the dead-letter handler workflow, and a complete record of every failed message with its error context and retry history.**

The processing worker handles the business logic, and when it fails with retries exhausted, Conductor's failure workflow mechanism captures the failed task with its full context. Original inputs, error details, retry history. A separate handler workflow receives the dead letter items and can log them, alert operators, or persist them for manual review. Every failed item is tracked with complete execution history, so you always know what failed, why, and how many times it was retried. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ProcessWorker handles message processing and reports success or failure, while HandleFailureWorker captures permanently failed messages with full context: original inputs, error details, and retry history, for investigation and manual review.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessWorker** | `dl_process` | Processes data based on mode input. When `mode="fail"`, returns FAILED with `{error: "Processing failed for data: order-456"}`. When mode is anything else (or missing), returns COMPLETED with `{result: "Processed: order-456"}`. Only accepts String inputs. Non-string mode defaults to "success", non-string data defaults to empty string. |
| **HandleFailureWorker** | `dl_handle_failure` | Handles dead letter entries by logging the failed task details. Receives `failedWorkflowId`, `failedTaskName`, and `error` as inputs. Returns `{handled: true, summary: "Failure handled for workflow wf-123, task dl_process: ..."}`. Always succeeds. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflows

**Main workflow** (`dead_letter_demo`):

```
dl_process
    |
    |-- success: workflow COMPLETED with {result: "Processed: order-123"}
    |-- failure: workflow FAILED (retries exhausted). Triggers dead letter handler

```

**Dead letter handler** (`dead_letter_handler`):

```
dl_handle_failure
    |
    v
(logs failure details, marks as handled, alerts operators)

```

The main workflow has `retryCount: 0` on the process task, so a failure immediately triggers the dead letter path. In production, you would set retries to 2-3 so that transient failures resolve before reaching the dead letter queue.

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
java -jar target/dead-letter-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Example Output

```
=== Dead Letter Queue Pattern: Capture and Retry Failed Tasks ===

Step 1: Registering task definitions...

  Registered: dl_process (no retries. Failures go to dead letter)
  Registered: dl_handle_failure
    Timeout: 60s total, 30s response

Step 2: Registering workflows...
  Workflows registered: dead_letter_demo, dead_letter_handler

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow (mode=success)...

  Workflow ID: a1b2c3d4-...
  [dl_process] mode=success, data=order-123
  [dl_process] Simulating failure
  [dl_process] Processing succeeded


  Status: COMPLETED
  Output: {result=<"Processed: " + data>, mode=fail}

Step 5: Starting workflow (mode=fail)...

  Workflow ID: e5f6a7b8-...
  Status: FAILED
  Output: {result=<"Processed: " + data>, mode=fail}

Step 6: Starting dead letter handler workflow...

  Handler Workflow ID: c9d0e1f2-...
    workflowId=e5f6a7b8-...
    taskName=dl_process
    error=Processing failed for data: order-456
  Status: COMPLETED
  Output: {result=<"Processed: " + data>, mode=fail}

Result: PASSED

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/dead-letter-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# Success path: message processes normally
conductor workflow start \
  --workflow dead_letter_demo \
  --version 1 \
  --input '{"mode": "success", "data": "order-123"}'

# Failure path: message fails (poison message)
conductor workflow start \
  --workflow dead_letter_demo \
  --version 1 \
  --input '{"mode": "fail", "data": "order-456-malformed"}'

# After a failure, manually start the dead letter handler:
conductor workflow start \
  --workflow dead_letter_handler \
  --version 1 \
  --input '{"failedWorkflowId": "<failed_wf_id>", "failedTaskName": "dl_process", "error": "Processing failed for data: order-456-malformed"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dead_letter_demo -s COMPLETED -c 5
conductor workflow search -w dead_letter_demo -s FAILED -c 5

```

## How to Extend

Each worker handles one message concern. Connect the processor to your real order or event handler, the dead letter worker to persist poison messages in a durable store (SQS DLQ, database), and the process-or-dead-letter workflow stays the same.

- **ProcessWorker** (`dl_process`): replace with your real message processing logic (parse events, validate schemas, call downstream APIs). Add retries (retryCount=3) so transient failures resolve before reaching the dead letter queue.
- **HandleFailureWorker** (`dl_handle_failure`): persist dead letter items to a DynamoDB/Postgres table, send alerts to Slack/PagerDuty, or publish to an SQS dead letter queue for manual review
- **Auto-trigger handler**: use Conductor's `failureWorkflow` field on the main workflow definition to automatically start the dead letter handler when the main workflow fails, instead of triggering it manually
- **Add retry-from-DLQ**: build a separate workflow that reads items from the dead letter store and retries them after the root cause is fixed

Swap in your real message processor and connect the dead-letter handler to a durable store, and the poison-message capture pipeline runs in production without modification.

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
dead-letter/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   ├── workflow.json                # Main workflow (dl_process)
│   └── dead-letter-handler.json     # Handler workflow (dl_handle_failure)
├── src/main/java/deadletter/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DeadLetterExample.java       # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HandleFailureWorker.java
│       └── ProcessWorker.java
└── src/test/java/deadletter/workers/
    ├── HandleFailureWorkerTest.java # 7 tests
    └── ProcessWorkerTest.java       # 9 tests

```
