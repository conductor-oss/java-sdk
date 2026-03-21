# Batch Scheduling in Java Using Conductor: Job Prioritization, Resource Allocation, and Batch Execution

Every night at 2 AM, four cron jobs fire simultaneously: the ETL import, the report generator, the data warehouse sync, and the ML model retraining. They all compete for the same database connections and CPU. The ETL job grabs all the connections, the report generator retries in a tight loop until it gets one, the data warehouse sync times out silently, and the ML job crashes with an OOM because the report generator leaked memory during its retry storm. Nobody knows it failed until the VP asks why the Monday morning dashboard is blank. You check the cron logs. Four jobs, no coordination, no priority, no resource awareness, no single place to see which one succeeded and which one didn't.

## The Problem

You have a queue of batch jobs: data imports, report generation, ETL pipelines, that need to be scheduled and executed. Jobs have different priorities (urgent customer exports before routine backups), different resource requirements (GPU-intensive ML training vs lightweight CSV parsing), and you have a limited number of execution slots. Jobs must be prioritized, resources allocated, and the batch executed without exceeding concurrency limits.

Without orchestration, batch scheduling is a cron-triggered script that runs everything serially or a custom job queue that's hard to monitor. Job prioritization is hardcoded, resource allocation doesn't consider current load, and there's no visibility into which jobs are running, queued, or failed.

## The Solution

**You just write the job prioritization and resource allocation logic. Conductor handles the prioritize-allocate-execute sequence, retries if a resource allocation fails, and visibility into which jobs ran, their ordering, and execution outcomes.**

Each scheduling concern is an independent worker. Job prioritization, resource allocation, and batch execution. Conductor runs them in sequence: prioritize the queue, allocate resources, then execute. Every batch run is tracked with job ordering, resource assignments, and execution results. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers manage each batch run: PrioritizeJobsWorker orders the queue by urgency and resource needs, AllocateResourcesWorker assigns compute slots based on capacity, and ExecuteBatchWorker runs the prioritized jobs within concurrency limits.

| Worker | Task | What It Does |
|---|---|---|
| **AllocateResourcesWorker** | `bs_allocate_resources` | Allocates compute resources for the batch execution. |
| **ExecuteBatchWorker** | `bs_execute_batch` | Executes the batch with allocated resources. |
| **PrioritizeJobsWorker** | `bs_prioritize_jobs` | Prioritizes jobs in a batch based on priority weighting. |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic, the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
bs_prioritize_jobs
    │
    ▼
bs_allocate_resources
    │
    ▼
bs_execute_batch

```

## Example Output

```
=== Example 404: Batch Scheduling ===

Step 1: Registering task definitions...
  Registered: bs_prioritize_jobs, bs_allocate_resources, bs_execute_batch

Step 2: Registering workflow 'batch_scheduling_404'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: e1670598-aa90-363e-8974-c1a7e7561f29

  [prioritize] Prioritizing 3 jobs in batch batch-20260308-001...
  [allocate] Allocating resources for batch batch-20260308-001 (max concurrency: 4)...
  [execute] Executing batch batch-20260308-001 with allocated resources...


  Status: COMPLETED
  Output: {totalJobs=3, resourcesAllocated=True, jobsCompleted=3, totalDurationMs=11200}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/batch-scheduling-1.0.0.jar

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
java -jar target/batch-scheduling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow batch_scheduling_404 \
  --version 1 \
  --input '{"batchId": "batch-20260308-001", "jobs": ["etl-import", "data-transform", "report-gen"], "maxConcurrency": 4}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w batch_scheduling_404 -s COMPLETED -c 5

```

## How to Extend

Each worker handles one scheduling phase. Connect the prioritizer to your job queue (SQS, Redis), the executor to your compute environment (Kubernetes Jobs, AWS Batch), and the prioritize-allocate-execute workflow stays the same.

- **AllocateResourcesWorker** (`bs_allocate_resources`): check Kubernetes cluster capacity, cloud instance availability, or on-prem resource pools before allocation
- **ExecuteBatchWorker** (`bs_execute_batch`): launch real jobs via Kubernetes Jobs, AWS Batch, or your internal execution engine with concurrency controls
- **PrioritizeJobsWorker** (`bs_prioritize_jobs`): implement real priority scoring based on SLA deadlines, customer tier, job age, and resource requirements

Wire in your real job queue and compute resource APIs, and the prioritize-allocate-execute orchestration runs without modification.

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
batch-scheduling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/batchscheduling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BatchSchedulingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AllocateResourcesWorker.java
│       ├── ExecuteBatchWorker.java
│       └── PrioritizeJobsWorker.java
└── src/test/java/batchscheduling/workers/
    ├── AllocateResourcesWorkerTest.java        # 3 tests
    ├── ExecuteBatchWorkerTest.java        # 3 tests
    └── PrioritizeJobsWorkerTest.java        # 4 tests

```
