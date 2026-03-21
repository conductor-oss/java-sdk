# WAIT Task Completion via SDK (TaskClient) in Java Using Conductor :  Ticket Initialization, WAIT for Programmatic Resolution, and Ticket Finalization

A Java Conductor workflow example demonstrating how to resume paused workflows programmatically from Java code using the Conductor SDK's TaskClient. initializing a support ticket with status "open", pausing at a WAIT task until a separate Java process (a background service, scheduled job, or another microservice) completes it via the SDK, then finalizing and closing the ticket. Unlike the REST API approach, the SDK method provides type-safe Java objects and integrates naturally into existing Java services without HTTP client setup. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Programmatic Workflow Resumption via the Conductor SDK

Instead of using REST APIs directly, the Conductor SDK's TaskClient can complete WAIT tasks programmatically from Java code. This is useful when another Java service or a scheduled job needs to resume a paused workflow. The workflow initializes a ticket, pauses at a WAIT task, and a separate process uses the SDK to complete it. The finalization step closes the ticket.

## The Solution

**You just write the ticket-initialization and finalization workers. Conductor handles the durable pause and the type-safe SDK-based task completion.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

InitWorker opens the support ticket, and FinalizeWorker closes it after resolution, the type-safe SDK-based task completion from a separate Java process is coordinated through Conductor's TaskClient.

| Worker | Task | What It Does |
|---|---|---|
| **InitWorker** | `wsdk_init` | Initializes a support ticket. takes the ticketId from workflow input and sets its status to "open", making the ticket available for resolution |
| *WAIT task* | `WAIT` | Pauses with the ticketId until a separate Java service completes this task using the Conductor SDK's TaskClient. the resolving service calls `taskClient.updateTask(...)` to resume the workflow | Built-in Conductor WAIT,  no worker needed |
| **FinalizeWorker** | `wsdk_finalize` | Closes the ticket after the WAIT task is resolved. marks the ticket as complete and returns the final result |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wsdk_init
    │
    ▼
WAIT [WAIT]
    │
    ▼
wsdk_finalize

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
java -jar target/wait-sdk-1.0.0.jar

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
java -jar target/wait-sdk-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wait_sdk_demo \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wait_sdk_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one end of the ticket lifecycle. connect your support ticketing system (Jira, ServiceNow) for initialization and your resolution backend for closure, and the SDK-driven workflow stays the same.

- **FinalizeWorker** (`wsdk_finalize`): close the ticket in your helpdesk system, send resolution confirmation, and update SLA metrics
- **InitWorker** (`wsdk_init`): create tickets in a helpdesk system like Zendesk or Jira Service Management, and set up monitoring for SLA deadlines

Replace the simulated helpdesk with Zendesk or Jira Service Management and the SDK-based programmatic resolution flow keeps working as defined.

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
wait-sdk/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/waitsdk/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WaitSdkExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java
│       └── InitWorker.java
└── src/test/java/waitsdk/workers/
    ├── FinalizeWorkerTest.java        # 6 tests
    └── InitWorkerTest.java        # 5 tests

```
