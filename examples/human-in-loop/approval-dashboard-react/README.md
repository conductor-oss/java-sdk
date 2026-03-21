# React Approval Dashboard in Java Using Conductor :  Task Processing and Priority-Based Pending Approval via WAIT Task

A Java Conductor workflow example paired with a React dashboard. a SIMPLE task processes an incoming request with title, priority, and assignee, then a WAIT task pauses the workflow until the assigned approver acts through the React UI. The React dashboard queries Conductor's task search API to list all pending approvals, filter by priority and assignee, and complete them with an approval status. Uses [Conductor](https://github.

## The Problem

You need a React-based approval dashboard where approvers can see their pending tasks, filter by priority (high, medium, low), and act on them. Each request comes in with a title describing what needs approval, a priority level, and the assigned approver. The system must process the request and then wait for the human approver to make a decision. which could take minutes or days depending on the priority. The dashboard needs to query for all pending tasks assigned to a specific person and display them sorted by priority, with real-time status updates as approvals are completed.

Without orchestration, you'd build a React frontend backed by a custom API server that manages a pending-tasks table, polls for status changes, and tracks approval timing. If the backend restarts while tasks are pending, you'd need to rebuild the approval queue from the database. There is no built-in way to see which tasks are pending for which assignee, how long each has been waiting, or to enforce SLA timeouts on high-priority items.

## The Solution

**You just write the task-processing worker. Conductor handles the priority-based approval queue and the searchable task API.**

The WAIT task is the key pattern here. After processing the request, the workflow pauses at the WAIT task. Conductor holds the state with the title, priority, and assignee metadata until the React dashboard completes the task via the API. Conductor takes care of holding pending approvals durably, providing a searchable task API that the React dashboard queries by assignee and status, accepting the approval decision when the assignee acts, and tracking the complete timeline from request to approval. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

DashTaskWorker processes requests with title, priority, and assignee metadata. It has no knowledge of the React dashboard or how pending approvals are queried and filtered.

| Worker | Task | What It Does |
|---|---|---|
| **DashTaskWorker** | `dash_task` | Processes the incoming approval request. validates the title, priority, and assignee, and marks it as ready for human review in the dashboard |
| *WAIT task* | `pending_approval` | Pauses the workflow with title, priority, and assignee metadata until the React dashboard calls `POST /tasks/{taskId}` with the approver's decision | Built-in Conductor WAIT. no worker needed |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
dash_task
    │
    ▼
pending_approval [WAIT]

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
java -jar target/approval-dashboard-react-1.0.0.jar

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
java -jar target/approval-dashboard-react-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow approval_dashboard_react_demo \
  --version 1 \
  --input '{"title": "sample-title", "priority": "high", "assignee": "sample-assignee"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w approval_dashboard_react_demo -s COMPLETED -c 5

```

## How to Extend

The task worker validates incoming requests with priority and assignee data. swap in your user directory and SLA rules for real validation, and the React dashboard workflow remains unchanged.

- **DashTaskWorker** → add real validation logic. verify the assignee exists in your user directory, check the priority against SLA rules, and enrich the request with department and cost center data
- **WAIT task** → the React dashboard completes it by calling `POST /tasks/{taskId}` with `{ "approvalStatus": "approved" }`. add fields like rejection reason, comments, or conditional approval notes
- Add a **NotifyWorker** after the WAIT task to send the requester an email or Slack notification with the approval decision
- Add priority-based timeout on the WAIT task. auto-escalate high-priority items after 1 hour, medium after 24 hours
- Add a SWITCH after approval to route approved items to fulfillment and rejected items to the requester for revision

Plug in your real request enrichment logic and the React dashboard's priority-based approval queue works exactly as before.

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
approval-dashboard-react/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/approvaldashboardreact/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApprovalDashboardReactExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       └── DashTaskWorker.java
└── src/test/java/approvaldashboardreact/workers/
    └── DashTaskWorkerTest.java        # 8 tests

```
