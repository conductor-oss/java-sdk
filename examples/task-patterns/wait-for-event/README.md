# Wait-for-Event in Java with Conductor

WAIT task demo. pauses a workflow durably until an external system sends a signal (approval, webhook callback, or third-party notification), then processes the signal data. The workflow survives process restarts while waiting. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need a workflow to pause and wait for an external event: a human approval, a webhook callback from a payment processor, or a notification from a third-party system, before continuing. The wait could last seconds, hours, or days. If the process hosting the workflow restarts while waiting, the workflow state must not be lost. Once the external signal arrives, the workflow must resume exactly where it left off, with the signal data available to downstream tasks.

Without orchestration, you'd poll a database for approval status in a loop, manage timeout logic manually, and lose the workflow context if the process restarts while waiting. You'd need to build durable state persistence, timeout handling, and a mechanism for external systems to resume the flow. Essentially re-implementing what a workflow engine already provides.

## The Solution

**You just write the request preparation and signal processing workers. Conductor handles the durable pause via WAIT, surviving restarts until the external signal arrives.**

This example demonstrates the WAIT task for pausing a workflow until an external system sends a signal. PrepareWorker takes a `requestId` and `requester`, logs the pending request, and returns preparation metadata (prepared=true, requestId, requester). The workflow then hits a WAIT task (`wait_for_signal`) that durably pauses execution: surviving process restarts, until an external system completes the task via Conductor's API with signal data (e.g., `{"decision": "approved", "signalData": "manager-notes"}`). Once the signal arrives, ProcessSignalWorker picks up the decision and signal data to finalize the request, returning processed=true, the requestId, and the decision. The WAIT task is a system task with no worker. Conductor manages the durable pause internally.

### What You Write: Workers

Two workers bookend the durable WAIT pause: PrepareWorker readies the request and returns preparation metadata before the workflow pauses, and ProcessSignalWorker acts on the external signal data (decision, notes) after the WAIT task resumes.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `we_prepare` | Prepares a request before the workflow pauses. Takes requestId and requester from workflow input, returns prepared=true along with both values echoed back. Passes through null values without error. |
| **ProcessSignalWorker** | `we_process_signal` | Processes the signal after the WAIT task completes. Takes requestId, signalData, and decision from the WAIT task output, returns processed=true with the requestId and decision. Handles null values gracefully. |

The WAIT task (`wait_for_signal`) is a Conductor system task.; no worker is needed. It pauses the workflow until an external system completes it via the Conductor API.

### The Workflow

```
we_prepare
    │
    ▼
wait_for_signal [WAIT]  ← pauses here until external API call
    │
    ▼
we_process_signal

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
java -jar target/wait-for-event-1.0.0.jar --workers

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
java -jar target/wait-for-event-1.0.0.jar --workers

```

Then in a separate terminal, start a workflow and complete the WAIT task:

```bash
# 1. Start the workflow
conductor workflow start \
  --workflow wait_event_demo \
  --version 1 \
  --input '{"requestId": "REQ-001", "requester": "alice"}'

# 2. Check workflow status: it will be RUNNING, paused at the WAIT task
conductor workflow status <workflow_id>

# 3. Find the WAIT task ID from the execution details
conductor workflow get-execution <workflow_id> -c

# 4. Complete the WAIT task with signal data via curl
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"taskId": "<WAIT_TASK_ID>", "status": "COMPLETED", "outputData": {"decision": "approved", "signalData": "manager-approved-2024"}}'

# 5. Check workflow status again: it should now be COMPLETED
conductor workflow status <workflow_id>

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wait_event_demo -s COMPLETED -c 5

```

## Example Output

```
=== WAIT Task: Pause Until External Signal ===

Step 1: Registering task definitions...
  Registered: we_prepare, we_process_signal
  Note: wait_for_signal is a system WAIT task.; no worker needed.

Step 2: Registering workflow 'wait_event_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Running in worker-only mode. Use the Conductor CLI to start workflows.
The WAIT task pauses execution until you complete it via the API.

Example:
  1. Start workflow:
     conductor workflow start --workflow wait_event_demo --version 1 \
       --input '{"requestId": "REQ-001", "requester": "alice"}'

  2. Find the WAIT task ID in the Conductor UI, then complete it:
     curl -X POST http://localhost:8080/api/tasks \
       -H 'Content-Type: application/json' \
       -d '{"taskId": "W-A-I-T_-T-A-S-K-001", "status": "COMPLETED", \
            "outputData": {"decision": "approved", "signalData": "external-data"}}'

--- After starting workflow and completing the WAIT task ---
  [prepare] Preparing request REQ-001 from alice
  [process_signal] Processing signal for request REQ-001: decision=approved, signalData=manager-approved-2024

  Status: COMPLETED
  Output: {requestId=REQ-001, decision=approved, processed=true}

```

## How to Extend

Connect the preparation step to a real notification system (email, Slack, PagerDuty) and the signal processing to your fulfillment service, and the durable WAIT pattern works unchanged.

- **PrepareWorker** (`we_prepare`): send a real notification (email, Slack, PagerDuty) to the approver with a deep link to an approval UI that calls Conductor's task completion API, or create a pending record in your ticketing system (Jira, ServiceNow)
- **ProcessSignalWorker** (`we_process_signal`): act on the approval decision: provision the requested resource, update the request status in your database, notify the requester of the outcome, or trigger a downstream fulfillment workflow
- **Add a timeout**: set `timeoutSeconds` on the WAIT task in workflow.json so that unanswered approvals automatically expire and route to an escalation path

Connecting the preparation step to a real notification system and the signal processor to your fulfillment service does not affect the durable WAIT pattern, since Conductor manages the pause and signal-to-workflow routing independently of what the workers do.

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
wait-for-event/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition with WAIT task
├── src/main/java/waitforevent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WaitForEventExample.java     # Main entry point (--workers mode recommended)
│   └── workers/
│       ├── PrepareWorker.java
│       └── ProcessSignalWorker.java
└── src/test/java/waitforevent/workers/
    ├── PrepareWorkerTest.java       # 6 tests
    └── ProcessSignalWorkerTest.java # 8 tests

```
