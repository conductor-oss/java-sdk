# Multi-Level Approval Chain in Java Using Conductor :  Manager, Director, VP Sequential WAIT/SWITCH with Early Rejection Termination

A Java Conductor workflow example for sequential multi-level approval. routing a request through Manager, Director, and VP, each using a WAIT task for human input followed by a SWITCH to check the decision. If any level rejects, the workflow terminates immediately without advancing to the next level. All three must approve for finalization. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## High-Value Requests Must Pass Through Manager, Director, and VP Approval

Some requests require sequential approval from multiple levels of management. First a Manager, then a Director, then a VP. Each level uses a WAIT task for human input, followed by a SWITCH that checks the decision. If any level rejects, the workflow terminates immediately. If all three approve, the request is finalized. If finalization fails after VP approval, you need to retry it without asking all three approvers to re-approve.

## The Solution

**You just write the request-submission and finalization workers. Conductor handles the sequential Manager-Director-VP approval chain and the early rejection short-circuit.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

SubmitWorker identifies the Manager, Director, and VP in the chain, and FinalizeWorker records the complete approval history, the sequential WAIT/SWITCH gates at each level are orchestrated by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `mla_submit` | Submits the request for multi-level approval. validates the request and identifies the Manager, Director, and VP in the approval chain |
| *WAIT + SWITCH* | Manager level | WAIT pauses for the Manager's decision; SWITCH checks if approved (advance to Director) or rejected (terminate workflow) | Built-in Conductor WAIT + SWITCH. no worker needed |
| *WAIT + SWITCH* | Director level | WAIT pauses for the Director's decision; SWITCH checks if approved (advance to VP) or rejected (terminate workflow) | Built-in Conductor WAIT + SWITCH. no worker needed |
| *WAIT + SWITCH* | VP level | WAIT pauses for the VP's decision; SWITCH checks if approved (proceed to finalization) or rejected (terminate workflow) | Built-in Conductor WAIT + SWITCH. no worker needed |
| **FinalizeWorker** | `mla_finalize` | Finalizes the request after all three levels approve. records the complete approval chain and triggers downstream fulfillment |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
mla_submit
    │
    ▼
wait_manager_approval [WAIT]
    │
    ▼
SWITCH (check_manager_decision)
    ├── false: terminate_manager_rejected
    └── default: wait_director_approval -> check_director_decision

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
java -jar target/multi-level-approval-1.0.0.jar

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
java -jar target/multi-level-approval-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_level_approval \
  --version 1 \
  --input '{"requestId": "TEST-001", "requestor": "sample-requestor"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_level_approval -s COMPLETED -c 5

```

## How to Extend

Each worker handles one end of the multi-level chain. connect your request management system for submission and your downstream business system for finalization, and the three-tier approval workflow stays the same.

- **FinalizeWorker** (`mla_finalize`): execute the approved action. Create a purchase order, provision resources, or trigger a downstream workflow with the full approval chain as audit evidence
- **SubmitWorker** (`mla_submit`): pull request details from your business system, determine the approval chain from the org chart API, and notify the first approver

Connect your org chart and fulfillment system and the Manager-Director-VP sequential approval chain runs without modification.

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
multi-level-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multilevelapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiLevelApprovalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java
│       └── SubmitWorker.java
└── src/test/java/multilevelapproval/workers/
    ├── FinalizeWorkerTest.java        # 5 tests
    └── SubmitWorkerTest.java        # 5 tests

```
