# Approval Delegation in Java Using Conductor :  Request Preparation, WAIT for Approver, SWITCH to Delegate, and Finalization

Approval delegation. prepares a request, pauses at a WAIT task for the initial approver, then uses a SWITCH to handle delegation: if the approver responds with "delegate" and a delegateTo target, a second WAIT task pauses for the delegate's decision. Whether approved directly or via delegation, the workflow finalizes the result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need approvals where the assigned approver can delegate to someone else. A manager receives an approval request but is on vacation, overloaded, or not the right person. they need to reassign it to a colleague or their backup. The original WAIT task must accept three possible actions: approve (finalize immediately), reject (finalize immediately), or delegate (specify who should handle it, then wait for that person). The delegation must be tracked,  who delegated to whom, when, and what the final decision was. Without a SWITCH after the WAIT, you cannot route delegation to a second WAIT task for the delegate's input.

Without orchestration, you'd build custom delegation logic. the approver clicks "delegate" in your UI, your backend updates a database record with the new assignee, and a polling loop watches for the delegate's response. If the system crashes after the delegation but before the delegate is notified, the request is stuck in limbo. There is no audit trail showing the delegation chain, and the original requester has no visibility into whether their request has been delegated or is still pending with the original approver.

## The Solution

**You just write the request-preparation and finalization workers. Conductor handles the approval hold, delegation routing, and the second wait for the delegate.**

The WAIT + SWITCH + WAIT pattern is the key here. After preparing the request, the workflow pauses at the first WAIT for the initial approver. The approver's response flows into a SWITCH. if the action is "delegate", the workflow enters a second WAIT task addressed to the delegate specified in the delegateTo field. Whether the approval comes from the original approver or the delegate, the finalize worker processes the outcome. Conductor takes care of holding state at each WAIT, routing delegation via SWITCH, tracking the complete delegation chain with timestamps, and finalizing regardless of which path was taken. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

PrepareWorker sets up the approval request, and FinalizeWorker records who decided. Whether the original approver or their delegate, neither worker handles the delegation routing.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `ad_prepare` | Prepares the approval request data. validates the request, identifies the initial approver, and signals readiness for the first WAIT task |
| *WAIT task* | `initial_approval_wait` | Pauses for the initial approver's response. they can approve (action: "approve"), reject, or delegate (action: "delegate", delegateTo: "person") | Built-in Conductor WAIT,  no worker needed |
| *SWITCH task* | `delegation_switch` | Routes based on the approver's action. "delegate" sends to a second WAIT task, other actions proceed to finalization | Built-in Conductor SWITCH,  no worker needed |
| *WAIT task* | `delegated_approval_wait` | Pauses for the delegate's decision, with the delegateTo identity passed from the initial approver's response | Built-in Conductor WAIT. no worker needed |
| **FinalizeWorker** | `ad_finalize` | Finalizes the approval. records the decision, who made it (original approver or delegate), and completes the request |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
ad_prepare
    │
    ▼
initial_approval_wait [WAIT]
    │
    ▼
SWITCH (delegation_switch_ref)
    ├── delegate: delegated_approval_wait
    │
    ▼
ad_finalize

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
java -jar target/approval-delegation-1.0.0.jar

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
java -jar target/approval-delegation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow approval_delegation_demo \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w approval_delegation_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one end of the delegation flow. connect your org chart or HRIS (Workday, BambooHR) for approver lookup and your notification service for delegation alerts, and the approval-delegation workflow stays the same.

- **PrepareWorker** → look up the initial approver from your org chart or delegation rules (out-of-office auto-delegation, reporting chain), and enrich the request with context for the approver
- **Initial WAIT task** → complete it with `{ "action": "delegate", "delegateTo": "backup@company.com" }` to trigger delegation, or `{ "action": "approve" }` to approve directly
- **FinalizeWorker** → record the complete delegation chain (who delegated to whom and when), update the request status, and notify the original requester of the final decision
- Add a **NotifyDelegateWorker** between the SWITCH and the delegated WAIT to email or Slack the delegate that a request has been delegated to them with context
- Add a timeout on the delegated WAIT task to auto-escalate if the delegate does not respond within the SLA
- Add a "re-delegate" SWITCH case to allow multi-hop delegation chains (delegate -> delegate -> approve)

Wire up a real approval system and the delegation chain: original approver to delegate to finalization, keeps working without touching the workflow definition.

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
approval-delegation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/approvaldelegation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApprovalDelegationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java
│       └── PrepareWorker.java
└── src/test/java/approvaldelegation/workers/
    ├── FinalizeWorkerTest.java        # 4 tests
    └── PrepareWorkerTest.java        # 4 tests

```
