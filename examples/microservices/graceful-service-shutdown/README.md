# Graceful Service Shutdown in Java with Conductor

Orchestrates graceful shutdown: stop accepting new work, drain in-flight tasks, checkpoint state, deregister from service registry, and terminate. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Shutting down a service instance without dropping in-flight requests requires a careful sequence: stop accepting new work, drain all in-flight tasks to completion, checkpoint any pending state so it can be resumed elsewhere, and deregister the instance from the service registry. Skipping any step leads to dropped requests or stale registry entries.

Without orchestration, shutdown hooks are implemented as JVM ShutdownHook callbacks that run in unpredictable order, with no guarantee that draining completes before deregistration. There is no record of whether the shutdown was clean or if tasks were lost.

## The Solution

**You just write the stop-accepting, drain, checkpoint, and deregister workers. Conductor handles ordered shutdown steps, guaranteed completion of each phase, and a durable record proving the shutdown was clean.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers execute the shutdown sequence: StopAcceptingWorker halts new work intake, DrainTasksWorker waits for in-flight tasks to complete, CheckpointWorker persists pending state, and DeregisterWorker removes the instance from the service registry.

| Worker | Task | What It Does |
|---|---|---|
| **CheckpointWorker** | `gs_checkpoint` | Checkpoints the current state of the service instance before shutdown. |
| **DeregisterWorker** | `gs_deregister` | Deregisters the service instance from the service registry. |
| **DrainTasksWorker** | `gs_drain_tasks` | Drains in-flight tasks for the given instance, waiting for them to complete. |
| **StopAcceptingWorker** | `gs_stop_accepting` | Stops accepting new tasks for the given service instance. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
gs_stop_accepting
    │
    ▼
gs_drain_tasks
    │
    ▼
gs_checkpoint
    │
    ▼
gs_deregister

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
java -jar target/graceful-service-shutdown-1.0.0.jar

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
java -jar target/graceful-service-shutdown-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow graceful_shutdown_workflow \
  --version 1 \
  --input '{"serviceName": "test", "instanceId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w graceful_shutdown_workflow -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real connection pools, state-persistence layer (Redis, S3), and service registry (Consul, Eureka, Kubernetes), the stop-drain-checkpoint-deregister workflow stays exactly the same.

- **CheckpointWorker** (`gs_checkpoint`): persist in-flight task state to a durable store (Redis, S3, database) so another instance can resume
- **DeregisterWorker** (`gs_deregister`): remove the instance from Consul, Eureka, or your Kubernetes Service endpoint list
- **DrainTasksWorker** (`gs_drain_tasks`): wait for active HTTP connections, message consumers, or thread pools to drain

Replacing the checkpoint store or service registry backend has no impact on the stop-drain-checkpoint-deregister sequence.

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
graceful-service-shutdown/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gracefulserviceshutdown/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GracefulServiceShutdownExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckpointWorker.java
│       ├── DeregisterWorker.java
│       ├── DrainTasksWorker.java
│       └── StopAcceptingWorker.java
└── src/test/java/gracefulserviceshutdown/workers/
    ├── CheckpointWorkerTest.java        # 8 tests
    ├── DeregisterWorkerTest.java        # 8 tests
    ├── DrainTasksWorkerTest.java        # 8 tests
    └── StopAcceptingWorkerTest.java        # 8 tests

```
