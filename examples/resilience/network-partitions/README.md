# Implementing Network Partition Handling in Java with Conductor :  Resilient Workers with Reconnection Tracking

A Java Conductor workflow example demonstrating resilience to network partitions .  a worker that tracks connection attempts and handles reconnection gracefully when network connectivity is interrupted between the worker and the Conductor server.

## The Problem

In distributed systems, network partitions happen .  the worker loses connectivity to the Conductor server due to infrastructure issues, DNS failures, or cloud provider outages. During a partition, the worker can't poll for tasks or report results. When connectivity resumes, the worker must reconnect and resume processing without duplicating work or losing progress.

Without orchestration, network partition handling means custom reconnection logic, heartbeat monitoring, and manual state reconciliation. Each worker implements its own retry-on-disconnect behavior. Some workers crash on disconnect, some silently stop processing, and some lose in-flight work.

## The Solution

**You just write the task processing logic. Conductor handles reconnection, partition tolerance, and task resumption for free.**

The worker tracks connection attempts and handles reconnection state. Conductor's polling model is inherently partition-tolerant .  when the network heals, the worker simply resumes polling. Tasks that timed out during the partition are retried automatically. The full history of connection attempts and task executions is preserved. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

NetworkPartitionWorker tracks connection attempts and processes tasks resiliently, automatically resuming work when connectivity to the Conductor server is restored after a network interruption.

| Worker | Task | What It Does |
|---|---|---|
| **NetworkPartitionWorker** | `np_resilient_task` | Worker for np_resilient_task. Demonstrates handling network partitions. Tracks attempt count using an AtomicInteger.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
np_resilient_task
```

## Example Output

```
=== Network Partitions Demo: Handling Network Partitions ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'network_partitions_demo'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [np_resilient_task] Processing attempt

  Status: COMPLETED
  Output: {result=..., attempt=...}

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
java -jar target/network-partitions-1.0.0.jar
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
java -jar target/network-partitions-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow network_partitions_demo \
  --version 1 \
  --input '{"data": "sample-data"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w network_partitions_demo -s COMPLETED -c 5
```

## How to Extend

Each worker processes real tasks with built-in partition tolerance .  connect to your actual business services, and the automatic reconnection and task resumption after network interruptions stays the same.

- **NetworkPartitionWorker** (`np_resilient_task`): add real health checks (ping Conductor, verify DNS resolution, test network routes) and reconnection logic with circuit breaker state

Add your real business logic to the partition-tolerant worker, and the automatic reconnection and task resumption behavior carries over without changes.

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
network-partitions/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/networkpartitions/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NetworkPartitionsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── NetworkPartitionWorker.java
└── src/test/java/networkpartitions/workers/
    └── NetworkPartitionWorkerTest.java        # 9 tests
```
