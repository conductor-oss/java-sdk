# Escalation Timer in Java Using Conductor :  Request Submission, WAIT with Timeout for Auto-Approval, and Decision Processing

A Java Conductor workflow example demonstrating timeout-based auto-approval .  submitting a request, pausing at a WAIT task for human approval, and auto-approving if the approver does not respond within the configured deadline. The process worker handles both human-approved and auto-approved decisions identically, recording the method (human vs, auto) for audit purposes. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Pending approvals that sit indefinitely block business processes. If an approver is on vacation, overwhelmed, or simply forgets, the request should not languish forever. The workflow must submit the request, wait for a human decision, and if no decision arrives within the configured deadline (autoApproveAfterMs), automatically approve the request so work can proceed. The processing step must know whether the decision came from a human or was auto-approved, since auto-approved items may need additional audit scrutiny. Without timeout-based escalation, stale requests accumulate and SLAs are violated.

## The Solution

**You just write the request-submission and decision-processing workers. Conductor handles the timed wait and the auto-approval when the deadline expires.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

SubmitWorker prepares the request with a deadline, and ProcessWorker handles both human-approved and auto-approved decisions, the timeout-based auto-approval logic is managed by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `et_submit` | Submits the request for approval .  validates the request ID, records the auto-approve deadline, and marks it as ready for the WAIT task |
| *WAIT task* | `approval_wait` | Pauses for the approver's decision; an external timer checks the deadline and auto-completes the task with `{ "decision": "approved", "method": "auto" }` if no human acts in time | Built-in Conductor WAIT .  no worker needed |
| **ProcessWorker** | `et_process` | Processes the approval decision .  records whether it came from a human or auto-approval, flags auto-approved items for audit review, and triggers downstream actions |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
et_submit
    │
    ▼
approval_wait [WAIT]
    │
    ▼
et_process
```

## Example Output

```
=== Example 98: Escalation Timer ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'escalation_timer_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [et_process] Decision:
  [et_submit] Request

  Status: COMPLETED
  Output: {processed=..., submitted=...}

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
java -jar target/escalation-timer-1.0.0.jar
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
java -jar target/escalation-timer-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow escalation_timer_demo \
  --version 1 \
  --input '{"requestId": "REQ-ESC", "REQ-ESC": "autoApproveAfterMs", "autoApproveAfterMs": 3000}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w escalation_timer_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one end of the timeout flow .  swap in your request system for submission and your SLA enforcement service for decision processing, and the auto-approval-on-timeout workflow stays the same.

- **ProcessWorker** (`et_process`): route the decision to downstream systems. If auto-approved, flag it for audit review; if human-approved, proceed with standard fulfillment
- **SubmitWorker** (`et_submit`): enrich the submission with SLA deadlines from a configuration database and notify the approver via email/Slack/push notification

Integrate your request management system and the timeout-based auto-approval mechanism keeps functioning as configured.

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
escalation-timer/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/escalationtimer/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EscalationTimerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessWorker.java
│       └── SubmitWorker.java
└── src/test/java/escalationtimer/workers/
    ├── ProcessWorkerTest.java        # 10 tests
    └── SubmitWorkerTest.java        # 7 tests
```
