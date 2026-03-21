# Four-Eyes Approval in Java Using Conductor: Dual Independent Approvals via Parallel WAIT Tasks in FORK/JOIN

A Java Conductor workflow example implementing the four-eyes principle: submitting a request, then running two independent WAIT tasks in parallel via FORK/JOIN so two different approvers must both approve before finalization proceeds. Neither approver can see the other's decision until both have acted, ensuring independence. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Some Decisions Require Two Independent Approvals (Four-Eyes Principle)

Regulated industries require the four-eyes principle: no single person can approve a critical action alone. The workflow submits the request, then two independent WAIT tasks run in parallel (via FORK/JOIN), each assigned to a different approver. Both must approve before finalization proceeds. If finalization fails, you need to retry it without asking both approvers to re-approve.

## The Solution

**You just write the request submission and finalization workers. Conductor handles the parallel dual-approval gates and the join.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. Your code handles the decision logic.

### What You Write: Workers

SubmitWorker prepares the request for dual review, and FinalizeWorker records both approvers' decisions, each runs independently within the FORK/JOIN approval structure.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `fep_submit` | Submits the request for dual approval. validates the request ID and marks it as ready for the parallel FORK/JOIN approval gates |
| *FORK/JOIN* | `fork_approvers` | Launches two parallel WAIT tasks. One for each independent approver, and waits until both have responded | Built-in Conductor FORK/JOIN.; no worker needed |
| *WAIT task* | `approver_1` | Pauses for the first approver's independent decision via `POST /tasks/{taskId}` with `{ "approval": true/false }` | Built-in Conductor WAIT.; no worker needed |
| *WAIT task* | `approver_2` | Pauses for the second approver's independent decision, running in parallel with approver 1 | Built-in Conductor WAIT.; no worker needed |
| **FinalizeWorker** | `fep_finalize` | Receives both approvers' decisions and finalizes: proceeds only if both approved, otherwise rejects and records which approver dissented |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
fep_submit
    │
    ▼
FORK_JOIN
    ├── approver_1
    └── approver_2
    │
    ▼
JOIN (wait for all branches)
fep_finalize

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
java -jar target/four-eyes-approval-1.0.0.jar

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
java -jar target/four-eyes-approval-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow four_eyes_approval_demo \
  --version 1 \
  --input '{"requestId": "REQ-2026-0042"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w four_eyes_approval_demo -s COMPLETED -c 5

```

### Completing the WAIT tasks (human approval)

When the workflow reaches the FORK/JOIN, two parallel WAIT tasks (`approver_1` and `approver_2`) are created. Both must be completed externally for the workflow to proceed. The workflow status will show as `RUNNING` with both WAIT tasks `IN_PROGRESS`.

**Step 1: Find the WAIT task IDs**

```bash
# Get the execution details: look for tasks named "approver_1" and "approver_2"
conductor workflow get-execution <workflow_id> -c

```

The task IDs are in the `taskId` field of the `approver_1_ref` and `approver_2_ref` tasks.

**Step 2: Approver 1 approves**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<approver_1_task_id>", "status": "COMPLETED", "outputData": {"approval": true, "approvedBy": "senior-analyst@example.com", "approvedAt": "2026-03-14T09:15:00Z"}}'

```

**Step 3: Approver 2 approves (independently)**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<approver_2_task_id>", "status": "COMPLETED", "outputData": {"approval": true, "approvedBy": "compliance-officer@example.com", "approvedAt": "2026-03-14T10:30:00Z"}}'

```

**Rejecting (either approver can reject):**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<approver_task_id>", "status": "COMPLETED", "outputData": {"approval": false, "rejectedBy": "compliance-officer@example.com", "reason": "Insufficient supporting documentation"}}'

```

After both WAIT tasks are completed, the JOIN gate opens and the `fep_finalize` worker receives both approvers' decisions.

## How to Extend

Each worker handles one step of the four-eyes flow. Connect the submit worker to your request management system and the finalize worker to your GRC or compliance platform, and the dual-approval workflow stays the same.

- **SubmitWorker** (`fep_submit`): pull request details from your business system and assign approvers based on org chart rules, ensuring the two approvers are from different teams or roles (e.g., one from Risk and one from Compliance). Integrate with your HRIS (Workday, BambooHR) to enforce separation-of-duties rules
- **FinalizeWorker** (`fep_finalize`): push the dual-approved decision to downstream systems with a compliance audit trail, recording both approvers' identities and timestamps. Post the finalized record to your GRC (Governance, Risk, Compliance) system and notify the requester via email or Slack
- **WAIT tasks**: trigger from your internal approval UI (e.g., a React dashboard that calls `POST /tasks/{taskId}` when the approver clicks Approve), or from a Slack bot with Approve/Reject buttons that forward the decision to Conductor
- **Add a pre-check step**: insert a worker before the FORK/JOIN that validates the request against automated policy rules, rejecting obviously non-compliant requests before consuming approver time

Connect your compliance system for real dual-approval enforcement and the four-eyes workflow operates unchanged.

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
four-eyes-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/foureyesapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FourEyesApprovalExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java      # Records dual-approval outcome
│       └── SubmitWorker.java        # Submits request for dual approval
└── src/test/java/foureyesapproval/workers/
    ├── FinalizeWorkerTest.java      # 5 tests. Completion, output shape, determinism
    └── SubmitWorkerTest.java        # 5 tests. Completion, output shape, determinism

```
