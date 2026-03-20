# Next.js Approval Dashboard in Java Using Conductor :  Request Processing and Human Approval via WAIT Task

A Java Conductor workflow example paired with a Next.js full-stack dashboard .  a SIMPLE task validates and processes an incoming approval request (type, title, amount, requester), then a WAIT task pauses the workflow until a human approver clicks approve or reject in the Next.js dashboard. The dashboard queries Conductor's API to list pending approvals and completes the WAIT task when the approver acts. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need a web-based approval dashboard where managers can see all pending requests and approve or reject them with a single click. Requests come in with a type (expense, purchase, time-off), title, dollar amount, and requester name. The system must validate and process each request, then pause and wait .  potentially for hours or days ,  until a human approver makes a decision. The dashboard needs to show all pending, approved, and rejected requests in real time. Without a built-in WAIT mechanism, you'd poll a database for the approver's decision, building your own state management, timeout handling, and dashboard query logic.

Without orchestration, you'd build a full-stack app where the backend writes requests to a database, the frontend polls for pending items, and a separate cron job checks for decisions and advances the workflow. If the server restarts while waiting for approval, you'd need to rebuild state from the database. There is no built-in way to track how long each request has been pending, which approver acted, or when the approval happened.

## The Solution

**You just write the request-processing worker. Conductor handles the pending-approval queue and the dashboard API.**

The WAIT task is the key pattern here. After processing the request, the workflow pauses at the WAIT task. Conductor holds the state durably until the Next.js dashboard calls the Conductor API to complete it with the approver's decision. Conductor takes care of holding the pending request for hours or days, accepting the approval decision (approved/rejected and approver identity) via a simple API call from the dashboard, tracking the complete lifecycle from submission through approval, and providing the API that the Next.js dashboard queries to list all pending, approved, and rejected requests. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

NxtProcessWorker validates incoming requests with type, title, and amount. It has no awareness of the Next.js dashboard or the WAIT task that powers the approval queue.

| Worker | Task | What It Does |
|---|---|---|
| **NxtProcessWorker** | `nxt_process` | Validates and processes the incoming approval request .  checks the type, title, amount, and requester, and marks it as ready for human review |
| *WAIT task* | `nxt_approval` | Pauses the workflow until the Next.js dashboard sends an approval decision via `POST /tasks/{taskId}` with the approver's identity and approved/rejected status | Built-in Conductor WAIT .  no worker needed |

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
nxt_process
    │
    ▼
nxt_approval [WAIT]
```

## Example Output

```
=== Next.js Full-Stack Approval Dashboard Demo ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'approval_dashboard_nextjs_demo'...
  Workflow registered.

Step 3: Starting workers...
  1 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [nxt_process] Processing approval request:

  Status: COMPLETED
  Output: {processed=..., type=..., title=..., amount=...}

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
java -jar target/approval-dashboard-nextjs-1.0.0.jar
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
java -jar target/approval-dashboard-nextjs-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow approval_dashboard_nextjs_demo \
  --version 1 \
  --input '{"type": "standard", "expense": "sample-expense", "title": "sample-title", "Q4 Marketing Budget": "sample-Q4 Marketing Budget", "amount": 250.0, "requester": "sample-requester"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w approval_dashboard_nextjs_demo -s COMPLETED -c 5
```

## How to Extend

The process worker validates incoming requests .  swap in real budget checks, policy rules, or duplicate detection, and the Next.js dashboard workflow remains unchanged.

- **NxtProcessWorker** → add real validation logic .  check the requester's budget authority, verify the amount is within approval limits, detect duplicate requests, and enrich with department and cost center data
- **WAIT task** → the Next.js dashboard completes it by calling `POST /tasks/{taskId}` with `{ "approved": true, "approver": "manager@company.com" }` .  add fields like rejection reason, conditional approval notes, or delegation to another approver
- Add a **NotifyWorker** after the WAIT task to email the requester with the approval decision and any comments from the approver
- Add a SWITCH after approval to route approved requests to a fulfillment workflow and rejected requests to an archive
- Add an escalation timer using Conductor's timeout on the WAIT task to auto-escalate requests that have been pending beyond the SLA

Add real budget checks or policy validation and the Next.js dashboard approval flow continues to operate without changes.

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
approval-dashboard-nextjs/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/approvaldashboardnextjs/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApprovalDashboardNextjsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── NxtProcessWorker.java
└── src/test/java/approvaldashboardnextjs/workers/
    └── NxtProcessWorkerTest.java        # 8 tests
```
