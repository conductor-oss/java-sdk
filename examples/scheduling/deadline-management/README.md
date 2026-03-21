# Deadline Management in Java Using Conductor: Check Deadlines, Route by Urgency, and Handle Overdue Items

The SOC 2 compliance filing was due Friday. On Monday morning, the auditor emails asking where it is. You check Jira, the ticket was assigned to someone who went on vacation. The "deadline reminder" was a cron job that sent an email three days before the due date, but the assignee's inbox had 200 unread messages. Nobody escalated because the system doesn't distinguish between "due in two weeks" and "due in four hours." By the time the overdue item surfaces, you're already in breach, scrambling to file late, and explaining to leadership why a known deadline slipped through a process that was supposed to prevent exactly this.

## The Problem

You manage tasks with deadlines: support tickets, deliverables, compliance filings. Each task's urgency depends on how close it is to its due date: still on track (normal processing), approaching deadline (urgent, escalate priority), or past due (overdue, immediate escalation and notification). The routing must be automatic and consistent across all tasks.

Without orchestration, deadline tracking lives in spreadsheets or dashboards that require manual review. Someone checks the list daily, misses an overdue item, and it becomes a fire drill. Building deadline automation as a script means hardcoding urgency thresholds and manually routing to different handling paths.

## The Solution

**You just write the deadline evaluation and escalation rules. Conductor handles urgency-based routing via SWITCH tasks, retries on escalation service failures, and a full record of every deadline evaluation and routing decision.**

A deadline checker worker evaluates the task's due date against current time. Conductor's SWITCH task routes to the appropriate handler: normal, urgent, or overdue, based on the urgency classification. Each handler takes the right action for its urgency level. Every deadline evaluation is tracked, so you can audit which items were escalated and when. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Workers handle each urgency level: CheckDeadlinesWorker evaluates how close a task is to its due date, then Conductor routes to HandleNormalWorker for on-track items, HandleUrgentWorker for approaching deadlines, or HandleOverdueWorker for past-due escalation.

| Worker | Task | What It Does |
|---|---|---|
| **CheckDeadlinesWorker** | `ded_check_deadlines` | Evaluates a task's due date, returning an urgency classification (normal/urgent/overdue) and hours remaining or overdue |
| **HandleNormalWorker** | `ded_handle_normal` | Handles on-track tasks by setting them to monitor status with a 24-hour check-in interval |
| **HandleOverdueWorker** | `ded_handle_overdue` | Escalates overdue tasks to management with P0 priority, reporting hours past deadline |
| **HandleUrgentWorker** | `ded_handle_urgent` | Escalates urgent tasks to the senior team with P1 priority, reporting remaining hours before deadline |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic, the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
ded_check_deadlines
    │
    ▼
SWITCH (ded_switch_ref)
    ├── urgent: ded_handle_urgent
    ├── overdue: ded_handle_overdue
    └── default: ded_handle_normal

```

## Example Output

```
=== Example 410: Deadline Management ===

Step 1: Registering task definitions...
  Registered: ded_check_deadlines, ded_handle_urgent, ded_handle_normal, ded_handle_overdue

Step 2: Registering workflow 'deadline_management_410'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 4a76f54d-df78-f1b2-420e-11335b585688

  [check] Checking deadline for task TASK-2201 (due: 2026-03-08T18:00:00Z)
  [urgent] Escalating task TASK-2201 - 4h remaining


  Status: COMPLETED
  Output: {urgency=urgent, hoursRemaining=4, action=urgent}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/deadline-management-1.0.0.jar

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
java -jar target/deadline-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow deadline_management_410 \
  --version 1 \
  --input '{"projectId": "proj-alpha", "taskId": "TASK-2201", "dueDate": "2026-03-08T18:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w deadline_management_410 -s COMPLETED -c 5

```

## How to Extend

Each worker handles one deadline concern. Connect the deadline checker to Jira or your project management tool, the escalation handler to PagerDuty or Slack, and the check-route-handle workflow stays the same.

- **CheckDeadlinesWorker** (`ded_check_deadlines`): query your task management system (Jira, Asana) for real due dates and compute urgency based on business calendar rules
- **HandleNormalWorker** (`ded_handle_normal`): process normally. Update task status, no escalation needed
- **HandleOverdueWorker** (`ded_handle_overdue`): create escalation tickets, notify managers via Slack/email, trigger SLA breach workflows

Connect to your ticketing system and escalation channels, and the deadline routing workflow runs in production without changes.

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
deadline-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/deadlinemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DeadlineManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckDeadlinesWorker.java
│       ├── HandleNormalWorker.java
│       ├── HandleOverdueWorker.java
│       └── HandleUrgentWorker.java
└── src/test/java/deadlinemanagement/workers/
    └── CheckDeadlinesWorkerTest.java        # 2 tests

```
