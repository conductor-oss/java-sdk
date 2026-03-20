# Multi-Level Escalation Chain in Java Using Conductor: Request Submission, WAIT for Analyst/Manager/VP Approval, and Finalization

A Java Conductor workflow example for multi-level escalation: submitting a request, pausing at a WAIT task where the request starts with an analyst who can approve, reject, or escalate to a manager, who can in turn escalate to a VP. The decision and the level at which it was made flow into the finalization step. Demonstrates hierarchical escalation where any level can resolve the request. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need an approval process where requests escalate through a chain of authority. Analyst, then manager, then VP. The request starts with the analyst. If the analyst has the authority and context to decide, they approve or reject directly. If the request exceeds their authority or expertise, they escalate to their manager. The manager can likewise approve, reject, or escalate to the VP. The system must track at which level the decision was made and who made it. Without escalation tracking, there is no visibility into whether requests are being resolved at the appropriate level or whether analysts are escalating too frequently.

Without orchestration, you'd build a custom escalation system, the analyst receives a notification, clicks "escalate" in your UI, your backend reassigns the request, sends a new notification to the manager, and repeats. If the system crashes during escalation, the request is unassigned. There is no single view showing how many requests are pending at each level, the average time at each level, or the escalation rate.

## The Solution

**You just write the request submission and escalation finalization workers. Conductor handles the durable hold through the analyst-manager-VP chain.**

The WAIT task is the key pattern here. After submitting the request, the workflow pauses at a single WAIT task. The external escalation logic (analyst -> manager -> VP) occurs outside the workflow, each person in the chain either completes the WAIT task with their decision or hands it to the next level. When someone finally decides, they complete the WAIT task with the decision and the level (respondedAt) at which it was made. The finalize worker then processes the outcome. Conductor takes care of holding the request durably through the entire escalation chain, accepting the final decision with the responder's level, tracking the complete timeline from submission through resolution, and retrying finalization if downstream systems are temporarily unavailable. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

EscSubmitWorker prepares the request for the analyst-manager-VP chain, and EscFinalizeWorker records which level made the final decision, the escalation logic between them lives outside the workers.

| Worker | Task | What It Does |
|---|---|---|
| **EscSubmitWorker** | `esc_submit` | Submits the request for escalation-chain approval: validates the request ID, enriches with context, and marks it as ready for the first level (analyst) |
| *WAIT task* | `esc_approval` | Pauses the workflow until someone in the escalation chain (analyst, manager, or VP) makes a final decision, completing it via `POST /tasks/{taskId}` with the decision and the level at which it was made | Built-in Conductor WAIT.; no worker needed |
| **EscFinalizeWorker** | `esc_finalize` | Finalizes the escalation: records the decision, who made it, and at which level (analyst/manager/VP), then triggers downstream actions |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
esc_submit
    │
    ▼
esc_approval [WAIT]
    │
    ▼
esc_finalize
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
java -jar target/escalation-chain-1.0.0.jar
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

## Example Output

```
=== Multi-Level Escalation Chain: Analyst -> Manager -> VP ===

Step 1: Registering task definitions...
  Registered: esc_submit, esc_finalize
  (WAIT is a system task.; no task definition needed)

Step 2: Registering workflow 'escalation_chain_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  [esc_submit] REQ-CHAIN

  Workflow ID: d7e8f9a0-...

Step 5: Waiting for WAIT task to be reached...
  WAIT task reached (taskId: 5a6b7c8d-...)

Step 6: Simulating escalation chain...

  Level: Analyst (analyst@co.com)
  Waiting 1500ms for response...
  No response. Escalating to next level

  Level: Manager (manager@co.com)
  Waiting 1500ms for response...
  No response. Escalating to next level

  Level: VP (vp@co.com)
  Waiting 1500ms for response...
  VP responds. Approved!

Step 7: Waiting for workflow completion...
  [esc_finalize] approved at VP
  Status: COMPLETED
  Decision: approved
  Responded at: VP level

Result: PASSED
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/escalation-chain-1.0.0.jar --workers
```

Then use the CLI in a separate terminal to start and manage workflows.

### Start a workflow run

```bash
conductor workflow start \
  --workflow escalation_chain_demo \
  --version 1 \
  --input '{"requestId": "REQ-2026-0089"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w escalation_chain_demo -s COMPLETED -c 5
```

### Completing the WAIT task (escalation chain approval)

When the workflow hits the `esc_approval` WAIT task, it pauses until someone in the escalation chain makes a decision. The escalation logic (analyst -> manager -> VP) happens outside the workflow. Your notification system routes the request through each level with timeouts. When someone decides, they complete the WAIT task with their decision and the level at which it was made.

**Step 1: Find the WAIT task ID**

```bash
# Get the execution details: look for the task named "esc_approval"
conductor workflow get-execution <workflow_id> -c
```

The task ID is in the `taskId` field of the `wait_ref` task.

**Step 2: Analyst approves directly**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "workflowInstanceId": "<workflow_id>",
    "taskId": "<task_id>",
    "status": "COMPLETED",
    "outputData": {
      "decision": "approved",
      "respondedAt": "Analyst",
      "respondedBy": "analyst@example.com",
      "escalationLevel": 1
    }
  }'
```

**Step 2 (alternative): Manager approves after escalation**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "workflowInstanceId": "<workflow_id>",
    "taskId": "<task_id>",
    "status": "COMPLETED",
    "outputData": {
      "decision": "approved",
      "respondedAt": "Manager",
      "respondedBy": "manager@example.com",
      "escalationLevel": 2
    }
  }'
```

**Step 2 (alternative): VP rejects after full escalation**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "workflowInstanceId": "<workflow_id>",
    "taskId": "<task_id>",
    "status": "COMPLETED",
    "outputData": {
      "decision": "rejected",
      "respondedAt": "VP",
      "respondedBy": "vp@example.com",
      "escalationLevel": 3,
      "reason": "Budget allocation not justified for Q2"
    }
  }'
```

After the WAIT task is completed, the `esc_finalize` worker records the decision and the level at which it was made.

## How to Extend

Each worker covers one end of the escalation flow. Plug in your ticketing system (Jira, ServiceNow) for submission and your notification service (Slack, PagerDuty) for escalation alerts, and the multi-level workflow doesn't change.

- **EscSubmitWorker** (`esc_submit`): pull request details from a ticketing system like Jira or ServiceNow, enrich with priority and SLA data, and trigger the first-level notification (email, Slack, PagerDuty) to the assigned analyst
- **EscFinalizeWorker** (`esc_finalize`): push the final decision to downstream systems: update the ticket status, notify the requester via email/Slack with the decision and who made it, and log the complete escalation chain (levels tried, response times) for audit and SLA reporting
- **WAIT task**: integrate with your escalation notification service: the analyst's dashboard, Slack approval buttons, or PagerDuty alerts each call `POST /tasks/{taskId}` with the decision. Build a timeout service that escalates to the next level if no response within the SLA window
- **Add SLA tracking**: insert a worker after `esc_submit` that records the SLA deadline for each level, and use Conductor's timeout features to auto-escalate if the WAIT task isn't completed within the level's SLA

Integrate a real ticketing system and the analyst-manager-VP escalation chain keeps working without modification.

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
escalation-chain/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/escalationchain/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EscalationChainExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EscFinalizeWorker.java   # Records decision and escalation level
│       └── EscSubmitWorker.java     # Submits request for escalation chain
└── src/test/java/escalationchain/workers/
    ├── EscFinalizeWorkerTest.java   # 5 tests. Analyst/manager/VP levels, null inputs
    └── EscSubmitWorkerTest.java     # 4 tests. Completion, null requestId, output shape
```
