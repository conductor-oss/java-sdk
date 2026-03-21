# Group Assignment with Claim Pattern in Java Using Conductor :  Ticket Intake, WAIT for Group Claim, and Resolution

A Java Conductor workflow example for group-based task assignment. processing a ticket intake and queuing it to an assigned group (like a support queue), pausing at a WAIT task until any available team member claims and works the ticket, then resolving and closing it. Demonstrates the claim pattern where tasks are published to a pool and pulled by the first available person. Uses [Conductor](https://github.

## Tasks Can Be Assigned to a Group and Claimed by Any Member

Some tasks do not have a specific assignee. They are published to a group (like a support queue), and any available team member can claim and work on it. The workflow processes intake, pauses at a WAIT task visible to the group, and after someone claims and completes it, the resolution step closes the ticket. If resolution fails, you need to retry it without asking someone to re-claim the task.

## The Solution

**You just write the ticket-intake and resolution workers. Conductor handles the group queue and the durable wait for a team member to claim the task.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

IntakeWorker queues the ticket to a support group, and ResolveWorker closes it after a team member claims it. Neither manages the group queue or tracks who picked up the task.

| Worker | Task | What It Does |
|---|---|---|
| **IntakeWorker** | `hgc_intake` | Processes the ticket intake. validates the ticket ID, assigns it to the specified group queue, and marks it as queued and ready for claim |
| *WAIT task* | `group_assigned_wait` | Publishes the ticket to the assigned group with instructions; pauses until a team member claims it by completing the task with `{ "claimedBy": "agent@company.com" }` via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **ResolveWorker** | `hgc_resolve` | Resolves and closes the ticket, recording who claimed and completed it |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
hgc_intake
    │
    ▼
group_assigned_wait [WAIT]
    │
    ▼
hgc_resolve

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
java -jar target/human-group-claim-1.0.0.jar

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
java -jar target/human-group-claim-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow human_group_claim \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w human_group_claim -s COMPLETED -c 5

```

## How to Extend

Each worker handles one end of the claim flow. connect your ticketing system (Zendesk, Freshdesk, ServiceNow) for intake and your resolution tracking backend for closure, and the group-claim workflow stays the same.

- **IntakeWorker** (`hgc_intake`): pull ticket data from a helpdesk system like Zendesk or Freshdesk, enrich with customer history, and assign to the appropriate group queue
- **ResolveWorker** (`hgc_resolve`): update the ticket status in your helpdesk system, send resolution confirmation to the customer, and update SLA metrics

Connect Jira or ServiceNow for real ticket management and the group claim pattern: queue, claim, resolve, functions unchanged.

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
human-group-claim/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/humangroupclaim/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HumanGroupClaimExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── IntakeWorker.java
│       └── ResolveWorker.java
└── src/test/java/humangroupclaim/workers/
    ├── IntakeWorkerTest.java        # 5 tests
    └── ResolveWorkerTest.java        # 5 tests

```
