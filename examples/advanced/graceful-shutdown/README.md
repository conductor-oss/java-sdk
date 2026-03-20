# Graceful Worker Shutdown in Java Using Conductor :  Signal, Drain, Complete, Checkpoint, Stop

A Java Conductor workflow example for graceful worker shutdown .  signaling a worker group to stop accepting new tasks, draining the task queue within a configurable timeout, completing all in-flight tasks, checkpointing the current state, and finally stopping the workers. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Killing Workers Loses Work

Sending `kill -9` to a worker process terminates it instantly .  any in-flight tasks get orphaned, partial results are lost, and the next worker restart has no idea where the previous instance left off. Messages get redelivered, duplicates appear, and the ops team spends an hour figuring out which tasks need to be manually retried.

Graceful shutdown means stopping new task pickup first (signal), giving the queue time to drain (with a configurable timeout so it doesn't hang forever), waiting for in-flight tasks to finish their current execution, checkpointing the state so the next instance knows exactly where to resume, and only then stopping the process. Getting this five-step sequence right .  especially under time pressure during a deploy ,  requires careful orchestration.

## The Solution

**You write the drain and checkpoint logic. Conductor handles the shutdown sequencing, retries, and state verification.**

`GshSignalWorker` notifies the worker group to stop polling for new tasks. `GshDrainTasksWorker` waits up to the configured `drainTimeoutSec` for the task queue to empty, tracking how many tasks were drained. `GshCompleteInflightWorker` ensures every in-flight task finishes and reports how many were completed. `GshCheckpointWorker` saves the current state .  checkpoint ID, completed task list ,  so the next startup can resume cleanly. `GshStopWorker` performs the final process termination. Conductor sequences these steps strictly, records the drain count, in-flight completion count, and checkpoint ID, and ensures nothing is lost during the shutdown.

### What You Write: Workers

Five workers enforce the shutdown protocol: signal broadcast, queue draining, in-flight completion, state checkpointing, and final stop, ensuring zero work is lost during deploys.

| Worker | Task | What It Does |
|---|---|---|
| **GshCheckpointWorker** | `gsh_checkpoint` | Saves a checkpoint of the current processing state so work can resume after restart |
| **GshCompleteInflightWorker** | `gsh_complete_inflight` | Waits for in-flight tasks to finish and reports how many were completed |
| **GshDrainTasksWorker** | `gsh_drain_tasks` | Stops accepting new tasks and drains the queue, reporting remaining in-flight tasks |
| **GshSignalWorker** | `gsh_signal` | Sends the shutdown signal to all worker instances with a timestamp |
| **GshStopWorker** | `gsh_stop` | Performs the final stop, confirming a clean shutdown with no data loss |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
gsh_signal
    │
    ▼
gsh_drain_tasks
    │
    ▼
gsh_complete_inflight
    │
    ▼
gsh_checkpoint
    │
    ▼
gsh_stop
```

## Example Output

```
=== Graceful Shutdown Demo ===

Step 1: Registering task definitions...
  Registered: gsh_signal, gsh_drain_tasks, gsh_complete_inflight, gsh_checkpoint, gsh_stop

Step 2: Registering workflow 'gsh_graceful_shutdown'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [checkpoint] Processing
  [complete] Processing
  [drain] Processing
  [signal] Processing
  [stop] Processing

  Status: COMPLETED
  Output: {checkpointId=..., completedCount=..., completedTasks=..., drainedCount=...}

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
java -jar target/graceful-shutdown-1.0.0.jar
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
java -jar target/graceful-shutdown-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gsh_graceful_shutdown \
  --version 1 \
  --input '{"workerGroup": "sample-workerGroup", "order-processors": "sample-order-processors", "drainTimeoutSec": "2025-01-15T10:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gsh_graceful_shutdown -s COMPLETED -c 5
```

## How to Extend

Each worker manages one shutdown phase .  replace the simulated drain and checkpoint calls with real queue APIs and state persistence and the graceful shutdown sequence runs unchanged.

- **GshDrainTasksWorker** (`gsh_drain_tasks`): integrate with Conductor's task queue API to poll remaining tasks and wait for the queue to empty, or monitor SQS `ApproximateNumberOfMessagesNotVisible`
- **GshCheckpointWorker** (`gsh_checkpoint`): write checkpoint state to Redis, DynamoDB, or a PostgreSQL `worker_checkpoints` table so the next startup can resume from the exact position
- **GshStopWorker** (`gsh_stop`): call Kubernetes `kubectl drain` for pod-level shutdown, or send a SIGTERM to the worker process group after confirming all tasks are checkpointed

The checkpoint and drain output contracts stay fixed. Swap the simulated signals for real Kubernetes preStop hooks or systemd notifications and the shutdown sequence runs unchanged.

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
graceful-shutdown/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gracefulshutdown/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GracefulShutdownExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GshCheckpointWorker.java
│       ├── GshCompleteInflightWorker.java
│       ├── GshDrainTasksWorker.java
│       ├── GshSignalWorker.java
│       └── GshStopWorker.java
└── src/test/java/gracefulshutdown/workers/
    ├── GshCheckpointWorkerTest.java        # 4 tests
    ├── GshCompleteInflightWorkerTest.java        # 4 tests
    ├── GshDrainTasksWorkerTest.java        # 4 tests
    ├── GshSignalWorkerTest.java        # 4 tests
    └── GshStopWorkerTest.java        # 4 tests
```
