# WAIT Task Completion via REST API in Java Using Conductor :  Prepare, WAIT for External HTTP Callback, SWITCH on Decision, and Route to Approved/Rejected Handlers

A Java Conductor workflow example demonstrating how external systems resume paused workflows via REST API. preparing a request, pausing at a WAIT task, and completing it when an external system sends `POST /tasks/{taskId}` with a decision payload. A SWITCH then routes based on the decision: "rejected" goes to a rejection handler, while the default path goes to an approval handler. Both paths use the same HandleDecisionWorker but receive different hardcoded decision values, demonstrating how a single worker can handle multiple SWITCH branches. Uses [Conductor](https://github.

## External Systems Need to Resume Workflows via REST API

Sometimes the signal to continue a workflow comes from an external system, a webhook, a third-party service callback, or an admin tool. The WAIT task pauses the workflow, and a REST API call completes it with the decision payload. The workflow prepares the request, pauses, then handles the decision when the external system calls back. If decision handling fails, you need to retry it without waiting for another external callback.

## The Solution

**You just write the request-preparation and decision-handling workers. Conductor handles the REST API callback endpoint and the durable pause for the external system.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

PrepareWorker sets up the callback context, and HandleDecisionWorker processes the approval outcome, the REST API endpoint that external systems call to resume the workflow is provided by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `wapi_prepare` | Prepares the workflow for the WAIT approval gate. sets up the context and returns ready=true to indicate the workflow is ready to receive an external callback |
| *WAIT task* | `WAIT` | Pauses the workflow until an external system sends a REST API call (`POST /tasks/{taskId}`) with a decision payload containing the approval outcome | Built-in Conductor WAIT. no worker needed |
| *SWITCH* | `decision_switch` | Routes based on the decision from the WAIT task output: "rejected" goes to a rejection handler, default (approved) goes to an approval handler | Built-in Conductor SWITCH. no worker needed |
| **HandleDecisionWorker** | `wapi_handle_decision` | Processes the approval outcome. reads the decision field from input and returns result="handled-{decision}" to confirm the decision was processed (used by both the approved and rejected SWITCH branches) |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wapi_prepare
    │
    ▼
WAIT [WAIT]
    │
    ▼
SWITCH (decision_switch_ref)
    ├── rejected: wapi_handle_decision
    └── default: wapi_handle_decision

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
java -jar target/wait-rest-api-1.0.0.jar

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
java -jar target/wait-rest-api-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wait_rest_api_demo \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wait_rest_api_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one side of the callback flow. connect your upstream system for request preparation and your webhook-triggered service for decision routing, and the REST-callback workflow stays the same.

- **HandleDecisionWorker** (`wapi_handle_decision`): route the external decision to downstream systems. Update a database, trigger fulfillment, or notify stakeholders
- **PrepareWorker** (`wapi_prepare`): set up the callback URL and authentication context for the external system, register a webhook endpoint

Wire up your real webhook endpoints and the REST API callback flow: prepare, wait, route by decision, continues without modification.

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
wait-rest-api/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/waitrestapi/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WaitRestApiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HandleDecisionWorker.java
│       └── PrepareWorker.java
└── src/test/java/waitrestapi/workers/
    ├── HandleDecisionWorkerTest.java        # 7 tests
    └── PrepareWorkerTest.java        # 4 tests

```
