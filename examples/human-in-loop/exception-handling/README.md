# Exception-Based Routing in Java Using Conductor :  Risk Analysis, SWITCH for Auto-Process vs. Human Review, and Finalization

A Java Conductor workflow example demonstrating exception-based human-in-the-loop routing .  analyzing an item's risk score, using SWITCH to route low-risk items to automatic processing or high-risk items (risk > 7) to a WAIT task for human review, and finalizing the result regardless of which path was taken. Demonstrates the pattern where automation handles the happy path and humans intervene only for exceptions. Uses [Conductor](https://github.## The Problem

Not every item needs human attention .  most can be auto-processed, but high-risk exceptions must be escalated to a person. The workflow analyzes the item's risk score (1-10) and routes accordingly: items scoring 7 or below go to automatic processing, while items above 7 pause at a WAIT task for human review. The human reviewer examines the flagged item and makes a decision. After either path completes (auto-processed or human-reviewed), a finalization step wraps up. Without this routing, you either manually review everything (expensive and slow) or auto-process everything (dangerous for high-risk items).

## The Solution

**You just write the risk-analysis, auto-processing, and finalization workers. Conductor handles the score-based routing and the human review hold for exceptions.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

AnalyzeWorker scores risk on a 1-10 scale, AutoProcessWorker handles low-risk items, and FinalizeWorker wraps up, the SWITCH routing between auto-processing and human review is declarative.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `eh_analyze` | Evaluates the item's risk score .  scores above 7 return route="human_review" for the SWITCH, scores 7 or below return route="auto_process" for automatic handling |
| *SWITCH task* | `route_switch` | Routes based on the analyze worker's output .  "human_review" sends to a WAIT task for human examination, default sends to auto-processing | Built-in Conductor SWITCH ,  no worker needed |
| **AutoProcessWorker** | `eh_auto_process` | Automatically processes low-risk items .  executes the standard business logic without human intervention |
| *WAIT task* | `human_review_wait` | Pauses for a human reviewer to examine the high-risk item and submit their decision via `POST /tasks/{taskId}` | Built-in Conductor WAIT .  no worker needed |
| **FinalizeWorker** | `eh_finalize` | Finalizes the workflow after either auto-processing or human review, recording the outcome and the path taken |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
eh_analyze
    │
    ▼
SWITCH (route_switch)
    ├── human_review: human_review_wait
    └── default: eh_auto_process
    │
    ▼
eh_finalize
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
java -jar target/exception-handling-1.0.0.jar
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
java -jar target/exception-handling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow exception_handling_demo \
  --version 1 \
  --input '{"risk": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w exception_handling_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one step of the exception flow .  connect your risk scoring engine for analysis and your processing backend for finalization, and the exception-routing workflow stays the same.

- **AnalyzeWorker** (`eh_analyze`): connect to a risk scoring engine, fraud detection API, or ML model to compute real risk scores based on transaction patterns
- **AutoProcessWorker** (`eh_auto_process`): execute the automated business process. Post a transaction, update inventory, or trigger a downstream workflow
- **FinalizeWorker** (`eh_finalize`): send confirmation notifications, update audit logs, and trigger post-processing hooks

Swap in a production risk-scoring engine or ML model and the auto-process vs. human-review routing logic stays the same.

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
exception-handling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/exceptionhandling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExceptionHandlingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── AutoProcessWorker.java
│       └── FinalizeWorker.java
└── src/test/java/exceptionhandling/workers/
    ├── AnalyzeWorkerTest.java        # 9 tests
    ├── AutoProcessWorkerTest.java        # 4 tests
    └── FinalizeWorkerTest.java        # 4 tests
```
