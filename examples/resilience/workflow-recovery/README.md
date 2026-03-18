# Implementing Workflow Recovery in Java with Conductor: Durability Across Server Restarts

A Java Conductor workflow example demonstrating workflow recovery. Showing that Conductor persists workflow state so that in-flight workflows survive server restarts, crashes, and scaling events, resuming from exactly where they left off without re-executing completed tasks.

## The Problem

Your Conductor server restarts. Planned maintenance, crash, scaling event, OOM kill. In-flight workflows that were mid-execution must not be lost. A workflow that was between step 2 and step 3 of a 5-step pipeline should resume at step 3 after the restart, not re-run from the beginning or disappear entirely. If step 2 charged a credit card, re-running it would double-charge the customer.

### What Goes Wrong Without Durable State

Consider a nightly batch processing pipeline that processes 10,000 orders:

1. Workflow starts processing batch `batch-2024-001`
2. After 4,000 orders, the server crashes (OOM, hardware failure, bad deploy)
3. Server restarts

Without durable workflow state:
- **Best case**: The batch restarts from order 1. 4,000 orders are processed twice (duplicate charges, duplicate emails, duplicate inventory deductions)
- **Worst case**: The batch is silently lost. 6,000 orders are never processed, and nobody notices until customers complain

With Conductor's persistence, the workflow resumes at order 4,001 automatically. No duplicates, no lost work, no manual intervention.

## The Solution

**You just write the processing steps. Conductor handles durability, crash recovery, and resume-from-checkpoint for free.**

Conductor persists every workflow execution to durable storage. When the server restarts, all in-flight workflows are recovered automatically and resume from their last completed task. No data is lost, no steps are re-executed, and no manual intervention is needed. The example demonstrates this by running a workflow, verifying it completes, then looking up the same workflow by ID to confirm the state was persisted. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

DurableTaskWorker processes batches while Conductor persists every execution to durable storage, ensuring workflows survive server restarts and resume from their last completed step without re-executing or losing progress.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **DurableTaskWorker** | `wr_durable_task` | Processes a batch by name. Accepts `{batch: "batch-001"}`, returns `{processed: true, batch: "batch-001"}`. Handles null/missing batch input gracefully by defaulting to empty string. | Simulated |

The simulated worker produces a realistic output shape so the workflow runs end-to-end. To go to production, replace the simulation with the real batch processing logic, the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
wr_durable_task
    |
    v
(workflow output persisted to durable storage)
```

The key insight is not the workflow structure. It is intentionally simple to highlight that **any** workflow, no matter how complex, gets durability for free. The persistence guarantee applies equally to a single-task workflow and a 50-task pipeline with forks, joins, and switches.

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
java -jar target/workflow-recovery-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Example Output

```
=== Workflow Recovery After Server Restart Demo ===

Step 1: Registering task definition for wr_durable_task...

  Registered: wr_durable_task
    Timeout: 60s total, 30s response

Step 2: Registering workflow 'workflow_recovery_demo'...
  Workflow registered.

Step 3: Starting workers...
  1 worker polling.

Step 4: Starting workflow with batch='batch-001'...

  Workflow ID: c4d5e6f7-...

Step 5: Waiting for completion...
  [wr_durable_task] Processing batch: batch-001
  Status: COMPLETED
  Output: {processed=true, batch=batch-001}

Step 6: Looking up workflow to verify persistence...
  Persisted status: COMPLETED
  Persisted output: {processed=true, batch=batch-001}

Result: PASSED. Workflow completed and state persisted successfully.
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/workflow-recovery-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Start a workflow and note the returned workflow ID
conductor workflow start \
  --workflow workflow_recovery_demo \
  --version 1 \
  --input '{"batch": "nightly-2024-03-15"}'

# After the workflow completes, look it up to verify persistence
conductor workflow status <workflow_id>

# Restart Conductor, then look up the same workflow: it is still there
conductor workflow get-execution <workflow_id> -c
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_recovery_demo -s COMPLETED -c 5
```

## How to Extend

Each worker represents a real pipeline step. Connect them to your order processing, payment charging, or data ETL services, and the durable recovery-after-restart behavior stays the same.

- **DurableTaskWorker** (`wr_durable_task`): replace with any long-running or multi-step worker (ETL batch, report generation, data migration). The durability guarantee applies to all workflows regardless of worker implementation.
- **Add more tasks**: chain multiple durable tasks together. If the server crashes between task 3 and task 4, Conductor resumes at task 4 automatically.
- **Idempotent workers**: for production, make workers idempotent (use idempotency keys) so that even if a task is retried after a crash, the side effect only happens once.

Replace with your real batch processing or ETL logic, and the crash-recovery and resume-from-checkpoint guarantees apply automatically without any code changes.

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
workflow-recovery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowrecovery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowRecoveryExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       └── DurableTaskWorker.java
└── src/test/java/workflowrecovery/workers/
    └── DurableTaskWorkerTest.java   # 8 tests
```
