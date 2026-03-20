# Worker Auto-Scaling in Java Using Conductor :  Monitor Queue, Calculate Capacity, Scale, Verify

A Java Conductor workflow example for worker auto-scaling .  monitoring queue depth and latency, calculating the number of workers needed to meet the target latency SLA, scaling the worker fleet up or down, and verifying the scaling action took effect. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Fixed Worker Counts Waste Money or Miss SLAs

Your task queue processes 100 messages per second during business hours and 5 per second overnight. Running enough workers for peak load 24/7 wastes 95% of compute during off-hours. Running for average load means queue depth explodes during peaks and latency exceeds your SLA. You need to dynamically scale workers based on current queue depth and target latency.

Auto-scaling means monitoring the queue to measure current depth and processing rate, calculating how many workers are needed to drain the queue within the target latency, scaling the fleet to match, and verifying that the new worker count actually reduced queue depth and latency. Each step feeds the next .  you can't calculate without monitoring data, and you can't verify without knowing what scaling action was taken.

## The Solution

**You write the monitoring and scaling logic. Conductor handles the scaling cycle, retries, and capacity verification.**

`WksMonitorQueueWorker` samples the queue depth and current worker count for the specified queue. `WksCalculateNeededWorker` computes the required worker count based on the current depth, processing rate, and target latency SLA. `WksScaleWorkersWorker` adjusts the worker fleet to the calculated count .  scaling up or down. `WksVerifyScalingWorker` checks that the scaling action took effect and the queue is draining toward the target latency. Conductor records the monitoring snapshot, scaling decision, and verification result for every scaling cycle.

### What You Write: Workers

Four workers form the auto-scaling loop. Queue monitoring, capacity calculation based on target latency, fleet scaling, and verification that the scaling action reduced queue depth.

| Worker | Task | What It Does |
|---|---|---|
| **WksCalculateNeededWorker** | `wks_calculate_needed` | Computes the desired worker count based on queue depth, average processing time, and target latency |
| **WksMonitorQueueWorker** | `wks_monitor_queue` | Samples current queue depth, average processing time, and in-flight task count |
| **WksScaleWorkersWorker** | `wks_scale_workers` | Scales the worker pool up or down to match the desired count, reporting the scaling action |
| **WksVerifyScalingWorker** | `wks_verify_scaling` | Confirms all scaled workers are healthy and ready to process tasks |

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
wks_monitor_queue
    │
    ▼
wks_calculate_needed
    │
    ▼
wks_scale_workers
    │
    ▼
wks_verify_scaling
```

## Example Output

```
=== Worker Scaling Demo ===

Step 1: Registering task definitions...
  Registered: wks_monitor_queue, wks_calculate_needed, wks_scale_workers, wks_verify_scaling

Step 2: Registering workflow 'wks_worker_scaling'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [calculate] Processing
  [monitor] Processing
  [scale] Processing
  [verify] Processing

  Status: COMPLETED
  Output: {desiredWorkers=..., scaleFactor=..., queueDepth=..., avgProcessingMs=...}

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
java -jar target/worker-scaling-1.0.0.jar
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
java -jar target/worker-scaling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wks_worker_scaling \
  --version 1 \
  --input '{"queueName": "sample-name", "order_processing_queue": "sample-order-processing-queue", "currentWorkers": "sample-currentWorkers", "targetLatencyMs": "sample-targetLatencyMs"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wks_worker_scaling -s COMPLETED -c 5
```

## How to Extend

Each worker covers one auto-scaling concern .  replace the simulated queue monitoring with real CloudWatch or Prometheus metrics APIs and the monitor-calculate-scale loop runs unchanged.

- **WksMonitorQueueWorker** (`wks_monitor_queue`): query real queue metrics: SQS `getQueueAttributes()` for depth, CloudWatch for processing rate, or Conductor's own task queue metrics via the API
- **WksScaleWorkersWorker** (`wks_scale_workers`): call real scaling APIs: Kubernetes HPA (`kubectl scale`), AWS Auto Scaling `setDesiredCapacity()`, or ECS `updateService()` to adjust task count
- **WksVerifyScalingWorker** (`wks_verify_scaling`): poll the scaling target to confirm new workers are running: check Kubernetes pod count, EC2 instance status, or queue depth trending downward

The monitoring and scaling contract stays fixed. Swap the simulated queue metrics for real CloudWatch or Prometheus queries and the calculate-scale-verify cycle runs unchanged.

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
worker-scaling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workerscaling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkerScalingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WksCalculateNeededWorker.java
│       ├── WksMonitorQueueWorker.java
│       ├── WksScaleWorkersWorker.java
│       └── WksVerifyScalingWorker.java
└── src/test/java/workerscaling/workers/
    ├── WksCalculateNeededWorkerTest.java        # 4 tests
    ├── WksMonitorQueueWorkerTest.java        # 4 tests
    ├── WksScaleWorkersWorkerTest.java        # 4 tests
    └── WksVerifyScalingWorkerTest.java        # 4 tests
```
