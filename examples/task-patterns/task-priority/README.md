# Task Priority in Java with Conductor

Workflow priority demo .  priority levels 0-99 (higher = more important). Uses [Conductor](https://github.## The Problem

You need high-priority requests to be processed before low-priority ones when workers are busy. Conductor's task priority (0-99, higher = more important) controls which tasks workers pick up first from the queue. When the worker pool is saturated, a priority-99 request jumps ahead of a priority-0 request without requiring separate queues or worker pools.

Without task priority, you'd build separate queues for different priority levels, deploy separate worker pools, and write custom routing logic. Conductor's built-in priority system handles this with a single workflow and a single worker pool. Just set the priority on the workflow or task.

## The Solution

**You just write the request processing worker. Conductor handles priority-based queue ordering, ensuring high-priority tasks are picked up first when workers are busy.**

This example starts multiple workflows with different priority levels (0-99) and shows how Conductor's task queue prioritization determines processing order. The PriProcessWorker handles a `pri_process` task that takes a `requestId` and `priority`, returning `{ processed: true }`. The example code fires off several workflow instances concurrently. Some at priority 0 (lowest) and others at priority 99 (highest). When the worker pool is saturated, the priority-99 tasks jump ahead of priority-0 tasks in the queue. The worker itself does not need to know about priority; Conductor manages the queue ordering transparently.

### What You Write: Workers

A single worker demonstrates priority-based queue ordering: PriProcessWorker processes requests without any priority awareness in its code, while Conductor's task queue ensures high-priority tasks (0-99 scale) are picked up before low-priority ones when workers are busy.

| Worker | Task | What It Does |
|---|---|---|
| **PriProcessWorker** | `pri_process` | Processes a request with priority awareness. Takes requestId and priority, returns { processed: true }. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
pri_process
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
java -jar target/task-priority-1.0.0.jar
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
java -jar target/task-priority-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow priority_demo \
  --version 1 \
  --input '{"requestId": "TEST-001", "priority": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w priority_demo -s COMPLETED -c 5
```

## How to Extend

Replace the stub request processor with your real workload (order fulfillment, report generation, data export), and the priority-based queue ordering works unchanged.

- **PriProcessWorker** (`pri_process`): replace the stub with real request processing (e.g., order fulfillment, report generation, or data export) and use the priority field to implement tiered SLAs. Priority 99 for real-time customer-facing requests, priority 50 for internal batch jobs, priority 0 for background maintenance tasks

Replacing the stub with real workload processing and adjusting priority values to match your SLA tiers requires no changes to the worker code or workflow definition. Conductor manages the queue ordering transparently.

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
task-priority/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskpriority/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskPriorityExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── PriProcessWorker.java
└── src/test/java/taskpriority/workers/
    └── PriProcessWorkerTest.java        # 9 tests
```
