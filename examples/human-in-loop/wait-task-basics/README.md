# WAIT Task Basics in Java Using Conductor :  Pre-WAIT Preparation, Pause for External Approval Signal, and Post-WAIT Processing with Approval Payload

A Java Conductor workflow example demonstrating the fundamental WAIT task pattern .  running a preparation step that validates the requestId, pausing at a WAIT task until an external signal (REST API, SDK, or UI) completes it with an approval payload, then processing the approval response. The post-WAIT worker receives both the original requestId and the approval value from the WAIT task's output, demonstrating how data flows through WAIT tasks: the external completer provides the output, and the next task can reference it via `${wait_for_approval_ref.output.approval}`. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Workflows Need to Pause and Wait for External Signals

The WAIT task is the fundamental building block for human-in-the-loop workflows. It pauses execution until an external signal (REST API call, SDK call, or UI interaction) completes the task with a payload. The workflow prepares data before the pause, waits, then processes the data provided when the WAIT task is completed. This example demonstrates the core WAIT task mechanics.

## The Solution

**You just write the preparation and post-approval processing workers. Conductor handles the external signal wait and the data flow through the WAIT task.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

WaitBeforeWorker validates the requestId and prepares context, and WaitAfterWorker processes the approval payload, the data flow through the WAIT task's output (like the approval field) is wired automatically by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **WaitBeforeWorker** | `wait_before` | Prepares the workflow before the pause .  takes the requestId from workflow input and returns prepared=true, setting up context for the WAIT task |
| *WAIT task* | `wait_for_approval` | Pauses the workflow until an external signal completes it via `POST /tasks/{taskId}` with an approval payload .  the output (including the approval field) becomes available to the next task | Built-in Conductor WAIT ,  no worker needed |
| **WaitAfterWorker** | `wait_after` | Processes the approval response .  receives the requestId from workflow input and the approval value from the WAIT task's output, then returns the final result |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
wait_before
    │
    ▼
wait_for_approval [WAIT]
    │
    ▼
wait_after
```

## Example Output

```
=== WAIT Task Basics: Pause and Resume a Workflow ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'wait_task_basics_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [wait_after] Processing after wait .  requestId=
  [wait_before] Preparing for wait...

  Status: COMPLETED
  Output: {result=..., prepared=...}

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
java -jar target/wait-task-basics-1.0.0.jar
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
java -jar target/wait-task-basics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wait_task_basics_demo \
  --version 1 \
  --input '{"requestId": "req-12345", "req-12345": "sample-req-12345"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wait_task_basics_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one side of the pause .  connect any upstream validation for preparation and any downstream business logic for post-approval processing, and the WAIT-based workflow stays the same.

- **WaitAfterWorker** (`wait_after`): process the WAIT task response. Validate the input, route based on the decision, and trigger downstream actions
- **WaitBeforeWorker** (`wait_before`): gather and validate all prerequisite data before pausing, so the WAIT task has complete context for the human or external system

Swap in real prerequisite gathering and decision routing and the WAIT task data flow: requestId in, approval payload out, stays the same.

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
wait-task-basics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/waittaskbasics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WaitTaskBasicsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WaitAfterWorker.java
│       └── WaitBeforeWorker.java
└── src/test/java/waittaskbasics/workers/
    ├── WaitAfterWorkerTest.java        # 7 tests
    └── WaitBeforeWorkerTest.java        # 4 tests
```
