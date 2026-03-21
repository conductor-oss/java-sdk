# Implementing Partial Failure Recovery in Java with Conductor :  Resume from Last Successful Step

A Java Conductor workflow example demonstrating partial failure recovery .  a three-step sequential pipeline where step 2 fails on the first attempt, showing how Conductor's retry endpoint resumes from the last successful step rather than restarting the entire workflow.

## The Problem

You have a multi-step pipeline (validate, process, finalize) where an intermediate step fails due to a transient issue. When you retry, you don't want to re-execute steps that already succeeded .  step 1's side effects (database writes, API calls) would be duplicated. You need to resume from exactly the point of failure, skipping already-completed steps.

Without orchestration, partial failure recovery requires building custom checkpoint logic. Each step must record its completion state, and the retry mechanism must read those checkpoints to know where to start. This is fragile, error-prone, and means every pipeline needs its own checkpointing implementation.

## The Solution

Each step is an independent worker. When step 2 fails, Conductor records which steps completed and which didn't. Calling Conductor's retry endpoint (`POST /workflow/{id}/retry`) resumes from exactly the failed step .  step 1 is skipped because it already succeeded. No checkpointing code needed. Every execution shows exactly which steps ran, which were skipped on retry, and what their results were. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Step1Worker validates input, Step2Worker performs the main processing (which may fail transiently), and Step3Worker finalizes the result, with Conductor's retry endpoint resuming from the exact point of failure without re-executing completed steps.

| Worker | Task | What It Does |
|---|---|---|
| **Step1Worker** | `pfr_step1` | Worker for pfr_step1 .  first step in the partial failure recovery pipeline. Takes a "data" input and returns { result.. |
| **Step2Worker** | `pfr_step2` | Worker for pfr_step2 .  second step that simulates a transient failure. Fails on the first attempt, then succeeds on r.. |
| **Step3Worker** | `pfr_step3` | Worker for pfr_step3 .  third and final step in the partial failure recovery pipeline. Takes a "prev" input and return.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
pfr_step1
    │
    ▼
pfr_step2
    │
    ▼
pfr_step3

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
java -jar target/partial-failure-recovery-1.0.0.jar

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
java -jar target/partial-failure-recovery-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow partial_failure_recovery_demo \
  --version 1 \
  --input '{"data": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w partial_failure_recovery_demo -s COMPLETED -c 5

```

## How to Extend

Each worker performs one pipeline step .  connect them to your real validation, processing, and finalization services, and the resume-from-failure-point behavior stays the same.

- **Step1Worker** (`pfr_step1`): replace with your first processing step .  data validation, input parsing, resource acquisition
- **Step2Worker** (`pfr_step2`): replace with your second step that may fail transiently .  external API call, database write, file processing
- **Step3Worker** (`pfr_step3`): replace with your final step .  result aggregation, notification, cleanup

Replace each step with your real validation, processing, and finalization services, and the resume-from-failure-point behavior carries over with no orchestration changes.

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
partial-failure-recovery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/partialfailurerecovery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PartialFailureRecoveryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Step1Worker.java
│       ├── Step2Worker.java
│       └── Step3Worker.java
└── src/test/java/partialfailurerecovery/workers/
    ├── Step1WorkerTest.java        # 4 tests
    ├── Step2WorkerTest.java        # 7 tests
    └── Step3WorkerTest.java        # 4 tests

```
