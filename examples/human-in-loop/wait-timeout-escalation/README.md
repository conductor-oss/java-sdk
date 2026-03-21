# WAIT Task Timeout Escalation in Java Using Conductor :  Request Preparation, WAIT with Deadline, Timeout-Triggered Escalation to Manager, and Normal Response Processing

A Java Conductor workflow example for deadline-driven escalation. preparing a request, pausing at a WAIT task with a timeout, and routing to an escalation path if no one responds before the deadline. If the human responds in time, the workflow processes their response normally. If the WAIT task times out, the escalation worker notifies a manager (manager@company.com) and flags the request as escalated. This prevents approval requests from sitting indefinitely without action. Uses [Conductor](https://github.

## WAIT Tasks Should Escalate When No One Responds in Time

If a human does not respond to a WAIT task within a deadline, the workflow should not hang indefinitely. Instead, it should escalate. Notify a manager, auto-approve, or take a default action. The workflow prepares the request, pauses at a WAIT task with a timeout, and a SWITCH routes to the escalation path if the timeout fires or to the normal processing path if the human responds in time.

## The Solution

**You just write the request-preparation, escalation-notification, and response-processing workers. Conductor handles the deadline timeout and the escalation-vs-normal routing.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

WtePrepareWorker sets up the escalation context, WteProcessWorker handles timely responses, and WteEscalateWorker notifies the manager on timeout, the deadline enforcement and escalation-vs-normal routing are managed by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **WtePrepareWorker** | `wte_prepare` | Prepares the request before the WAIT deadline starts. takes the requestId, sets up the escalation context, and returns ready=true |
| *WAIT task* | `wait_for_response` | Pauses for human input with a deadline. if the human responds via `POST /tasks/{taskId}` before timeout, the response flows to normal processing; if the timeout fires, the workflow routes to escalation | Built-in Conductor WAIT,  no worker needed |
| **WteProcessWorker** | `wte_process` | Processes the human's response when they reply before the deadline. reads the response from the WAIT task output and returns the processed result |
| **WteEscalateWorker** | `wte_escalate` | Handles escalation when the WAIT task times out. notifies the manager (manager@company.com), flags the request as escalated, and returns escalated=true with the escalation target |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wte_prepare
    │
    ▼
wait_for_response [WAIT]
    │
    ▼
wte_process

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
java -jar target/wait-timeout-escalation-1.0.0.jar

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
java -jar target/wait-timeout-escalation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wait_timeout_escalation_demo \
  --version 1 \
  --input '{"requestId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wait_timeout_escalation_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one branch of the timeout flow. connect your notification service (email, Slack, PagerDuty) for escalation alerts and your business system for normal response processing, and the deadline-driven workflow stays the same.

- **WteEscalateWorker** (`wte_escalate`): send real escalation notifications via email/Slack/PagerDuty, auto-approve with an audit flag, or reassign to the next person in the chain
- **WtePrepareWorker** (`wte_prepare`): set up escalation rules from a configuration database. Who to escalate to, timeout duration, default action
- **WteProcessWorker** (`wte_process`): handle the human response. Push the decision to downstream systems and cancel any pending escalation timers

Connect PagerDuty or Slack for real escalation notifications and the deadline-driven timeout-vs-response routing operates unchanged.

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
wait-timeout-escalation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/waittimeoutescalation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WaitTimeoutEscalationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WteEscalateWorker.java
│       ├── WtePrepareWorker.java
│       └── WteProcessWorker.java
└── src/test/java/waittimeoutescalation/workers/
    ├── WteEscalateWorkerTest.java        # 5 tests
    ├── WtePrepareWorkerTest.java        # 4 tests
    └── WteProcessWorkerTest.java        # 5 tests

```
