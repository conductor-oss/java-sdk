# Workflow Archival in Java with Conductor

Archival demo workflow .  single task for demonstrating cleanup policies. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage the lifecycle of completed workflow executions. Archiving old runs to keep the active database lean and queries fast. This demo shows a simple batch-processing workflow whose completed executions can be archived using Conductor's archival APIs and cleanup policies.

Without archival, completed workflow data accumulates indefinitely, slowing down queries and consuming storage. Conductor's archival lets you move completed executions to long-term storage while keeping the active dataset manageable.

## The Solution

**You just write the batch processing worker. Conductor handles execution history storage, archival to long-term storage, and cleanup of old runs.**

This example runs a simple batch-processing workflow and then demonstrates Conductor's archival APIs for managing completed execution data. ArchivalTaskWorker takes a `batch` identifier and returns `{ done: true }`. It is intentionally minimal because the focus is on what happens after execution. The example code shows how to run multiple workflow instances, then use Conductor's archival and removal APIs to move completed executions to long-term storage or purge them entirely. This keeps the active execution database lean so that status queries, search, and the Conductor UI remain fast even after millions of workflow runs.

### What You Write: Workers

One minimal worker demonstrates the archival lifecycle: ArchivalTaskWorker processes a batch and returns `{ done: true }`, keeping the focus on how Conductor's archival APIs manage completed execution data rather than on the processing logic itself.

| Worker | Task | What It Does |
|---|---|---|
| **ArchivalTaskWorker** | `arch_task` | Worker for the archival demo workflow. Takes a batch identifier and returns done: true. |

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
arch_task
```

## Example Output

```
=== Workflow Archival: Cleanup Policies for Completed Workflows ===

Step 1: Registering task definitions...
  Registered: arch_task

Step 2: Registering workflow 'archival_demo'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [arch_task] Processing batch:

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
java -jar target/workflow-archival-1.0.0.jar
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
java -jar target/workflow-archival-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow archival_demo \
  --version 1 \
  --input '{"batch": "sample-batch"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w archival_demo -s COMPLETED -c 5
```

## How to Extend

Replace the trivial batch task with real processing logic (ETL, aggregation, report generation), and the archival and cleanup lifecycle management works unchanged.

- **ArchivalTaskWorker** (`arch_task`): implement real batch processing logic (e.g., ETL transformations, data aggregation, report generation) whose completed workflow executions demonstrate Conductor's archival and cleanup capabilities

Replacing the trivial batch task with real ETL or report generation logic does not affect the archival and cleanup lifecycle, since Conductor manages execution history storage independently of what the worker processes.

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
workflow-archival/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowarchival/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowArchivalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── ArchivalTaskWorker.java
└── src/test/java/workflowarchival/workers/
    └── ArchivalTaskWorkerTest.java        # 6 tests
```
