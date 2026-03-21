# Worker Pool Management in Java Using Conductor :  Categorize, Assign Pool, Execute, Return

A Java Conductor workflow example for worker pool management. categorizing incoming tasks by type, assigning them to the appropriate specialized worker pool, executing the task on a worker from that pool, and returning the worker to the pool when done. Uses [Conductor](https://github.

## Specialized Tasks Need Specialized Workers

A video transcoding task needs workers with high-CPU instances and FFmpeg installed. An image recognition task needs GPU workers with CUDA drivers. A PDF generation task just needs a basic worker with LibreOffice. Sending all tasks to a single general-purpose pool wastes expensive GPU time on PDF generation and leaves transcoding tasks waiting behind image recognition jobs.

Worker pool management means categorizing each task to determine what kind of worker it needs, selecting the right pool (GPU pool, high-CPU pool, general pool), executing the task on a worker from that pool, and returning the worker for reuse when the task finishes.

## The Solution

**You write the categorization and pool assignment logic. Conductor handles task dispatch, retries, and pool utilization tracking.**

`WplCategorizeTaskWorker` examines the task payload and category to determine what resources it needs. `WplAssignPoolWorker` maps the task category to the appropriate worker pool. `WplExecuteTaskWorker` runs the task on a worker from the assigned pool. `WplReturnToPoolWorker` releases the worker back to the pool for reuse. Conductor sequences these steps, retries if execution fails, and records which pool handled each task. enabling pool utilization analysis.

### What You Write: Workers

Four workers handle pool-based dispatch. Task categorization, pool assignment by resource profile, execution on the assigned worker, and pool return for reuse.

| Worker | Task | What It Does |
|---|---|---|
| **WplAssignPoolWorker** | `wpl_assign_pool` | Assigns a specific worker from the matching pool based on the resource profile (CPU, memory, GPU) |
| **WplCategorizeTaskWorker** | `wpl_categorize_task` | Classifies the task category and determines the required resource profile (cpu-heavy, memory-heavy, balanced) |
| **WplExecuteTaskWorker** | `wpl_execute_task` | Runs the task on the assigned worker and reports execution duration |
| **WplReturnToPoolWorker** | `wpl_return_to_pool` | Returns the worker back to its pool after task completion, updating availability |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
wpl_categorize_task
    │
    ▼
wpl_assign_pool
    │
    ▼
wpl_execute_task
    │
    ▼
wpl_return_to_pool

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
java -jar target/worker-pools-1.0.0.jar

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
java -jar target/worker-pools-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wpl_worker_pools \
  --version 1 \
  --input '{"taskPayload": {"key": "value"}, "taskCategory": "general"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wpl_worker_pools -s COMPLETED -c 5

```

## How to Extend

Each worker handles one pool management step. replace the simulated pool assignments with real Kubernetes node selectors or cloud instance group APIs and the categorize-assign-execute cycle runs unchanged.

- **WplCategorizeTaskWorker** (`wpl_categorize_task`): classify tasks based on real resource needs: parse Docker image requirements, inspect task metadata for GPU/memory/CPU annotations, or look up task-to-pool mappings in a config database
- **WplAssignPoolWorker** (`wpl_assign_pool`): query real pool availability via Kubernetes node pools (`kubectl get nodes -l pool=gpu`), AWS Auto Scaling groups, or a custom pool broker with capacity tracking
- **WplExecuteTaskWorker** (`wpl_execute_task`): run on real specialized infrastructure: submit to a GPU-equipped Kubernetes pod, dispatch to an ECS task with specific instance requirements, or invoke a containerized worker via Docker API

The categorization and pool-assignment contract stays fixed. Swap the simulated resource matcher for real Kubernetes resource quotas or Nomad task groups and the dispatch pipeline runs unchanged.

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
worker-pools/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workerpools/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkerPoolsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WplAssignPoolWorker.java
│       ├── WplCategorizeTaskWorker.java
│       ├── WplExecuteTaskWorker.java
│       └── WplReturnToPoolWorker.java
└── src/test/java/workerpools/workers/
    ├── WplAssignPoolWorkerTest.java        # 4 tests
    ├── WplCategorizeTaskWorkerTest.java        # 4 tests
    ├── WplExecuteTaskWorkerTest.java        # 4 tests
    └── WplReturnToPoolWorkerTest.java        # 4 tests

```
