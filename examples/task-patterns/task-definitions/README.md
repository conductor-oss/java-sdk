# Task Definitions in Java with Conductor

Task definitions test .  runs td_fast_task to verify task definition configuration. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to configure per-task behavior: retry counts, retry strategies (FIXED vs EXPONENTIAL_BACKOFF), timeout durations, and response timeouts, independently from the workflow definition. Task definitions let you set these policies once and have them apply everywhere the task is used, across multiple workflows.

Without task definitions, retry and timeout policies are either hardcoded in the workflow JSON or scattered across worker code. Changing a timeout means editing every workflow that uses the task. Task definitions centralize these policies so they are consistent and easy to update.

## The Solution

**You just write the task worker. Conductor handles retries, timeouts, and backoff policies based on the task definition configuration.**

This example registers a task definition for `td_fast_task` with specific retry, timeout, and backoff policies, then runs a workflow that uses it. The FastTaskWorker is intentionally trivial: it just returns `{ done: true }`, because the point is the task definition, not the worker logic. The example code demonstrates creating a TaskDef with `retryCount`, `retryLogic` (FIXED or EXPONENTIAL_BACKOFF), `retryDelaySeconds`, `timeoutSeconds`, and `responseTimeoutSeconds`, registering it via the metadata API, and then running a workflow whose task inherits those policies automatically. Change the task definition once, and every workflow using that task picks up the new behavior.

### What You Write: Workers

One intentionally trivial worker demonstrates task definition configuration: FastTaskWorker returns `{ done: true }` so the focus stays on how retry counts, backoff strategies, and timeout policies are declared in the task definition rather than in worker code.

| Worker | Task | What It Does |
|---|---|---|
| **FastTaskWorker** | `td_fast_task` | Fast task worker for the task_def_test workflow. Simply returns { done: true } to confirm the task definition is work... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
td_fast_task
```

## Example Output

```
=== Task Definitions Demo: Configure Retries, Timeouts, Concurrency ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'task_def_test'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [td_fast_task] Executing fast task

  Status: COMPLETED
  Output: {done=...}

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
java -jar target/task-definitions-1.0.0.jar
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
java -jar target/task-definitions-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow task_def_test \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w task_def_test -s COMPLETED -c 5
```

## How to Extend

Replace the trivial task worker with your real processing logic, and the centralized retry, timeout, and backoff policies configured in the task definition apply automatically across all workflows.

- **FastTaskWorker** (`td_fast_task`): replace the trivial `done: true` response with real processing logic (e.g., a database write, an HTTP call to an external service, or a message queue publish) and tune the task definition's retry count, backoff strategy, and timeout values to match the real operation's characteristics

Replacing the stub with real processing logic and tuning the task definition's retry, backoff, and timeout values requires no workflow changes, the centralized policies apply automatically to every workflow that references the task.

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
task-definitions/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskdefinitions/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskDefinitionsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── FastTaskWorker.java
└── src/test/java/taskdefinitions/workers/
    └── FastTaskWorkerTest.java        # 6 tests
```
