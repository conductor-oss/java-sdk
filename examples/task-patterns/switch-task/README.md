# Switch Task in Java with Conductor

SWITCH task demo. routes support tickets to different handlers based on priority level (LOW/MEDIUM/HIGH) with a default catch-all for unrecognized values. Uses Conductor's `value-param` evaluator for declarative conditional branching without JavaScript expressions. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to route support tickets to different handlers based on priority. LOW priority tickets should be auto-resolved with a canned response. MEDIUM priority tickets should be assigned to the support team for review. HIGH priority tickets should be immediately escalated to a manager. Unknown or missing priorities should be flagged for triage. After the ticket is handled: regardless of which priority path was taken, the action must be logged for audit and SLA tracking.

Without orchestration, you'd write a switch statement or if/else chain, calling different handler functions based on the priority string. When a new priority level is added (e.g., CRITICAL), you modify the routing code and hope the else clause still catches edge cases. If the escalation handler fails for a HIGH ticket, there is no automatic retry, and the ticket sits unhandled. There is no record of which handler processed a given ticket without searching through application logs.

## The Solution

**You just write the priority-specific ticket handlers and the audit logger. Conductor handles the SWITCH routing, default-case fallback, and execution tracking.**

This example demonstrates Conductor's SWITCH task with `value-param` evaluator for ticket priority routing. The SWITCH matches on the `priority` input: `LOW` routes to AutoHandleWorker (auto-resolution), `MEDIUM` routes to TeamReviewWorker (team assignment), `HIGH` routes to EscalateWorker (manager escalation), and unrecognized values fall to the default case handled by UnknownPriorityWorker (triage flagging). After the SWITCH resolves, LogActionWorker records the action taken on the ticket regardless of which branch executed. Conductor tracks which priority branch was taken for every ticket, giving you a complete audit trail of ticket routing decisions.

### What You Write: Workers

Five workers cover the priority-based ticket routing: AutoHandleWorker auto-resolves LOW tickets, TeamReviewWorker assigns MEDIUM tickets, EscalateWorker pages managers for HIGH tickets, UnknownPriorityWorker flags unrecognized values, and LogActionWorker records the audit trail after every branch.

| Worker | Task | What It Does |
|---|---|---|
| **AutoHandleWorker** | `sw_auto_handle` | Handles LOW priority tickets: returns handler="auto" and resolved=true, indicating the ticket was auto-resolved without human intervention. |
| **TeamReviewWorker** | `sw_team_review` | Handles MEDIUM priority tickets: returns handler="team" and assignedTo="support-team-1", indicating the ticket was routed to a support team for manual review. |
| **EscalateWorker** | `sw_escalate` | Handles HIGH priority tickets: returns handler="manager" and escalatedTo="manager@example.com", indicating immediate manager escalation. |
| **UnknownPriorityWorker** | `sw_unknown_priority` | Handles unrecognized priority values (default case): returns handler="default" and needsClassification=true, flagging the ticket for triage. |
| **LogActionWorker** | `sw_log_action` | Runs after every SWITCH branch. Logs the ticketId and priority, returns logged=true to confirm the audit trail entry was recorded. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_ref)
    ├── LOW: sw_auto_handle
    ├── MEDIUM: sw_team_review
    ├── HIGH: sw_escalate
    └── default: sw_unknown_priority
    │
    ▼
sw_log_action

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
java -jar target/switch-task-1.0.0.jar

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
java -jar target/switch-task-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# Route a LOW priority ticket (auto-handled)
conductor workflow start \
  --workflow switch_demo \
  --version 1 \
  --input '{"ticketId": "TKT-101", "priority": "LOW", "description": "Button color is slightly off"}'

# Route a HIGH priority ticket (escalated)
conductor workflow start \
  --workflow switch_demo \
  --version 1 \
  --input '{"ticketId": "TKT-102", "priority": "HIGH", "description": "Production database unreachable"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w switch_demo -s COMPLETED -c 5

```

## How to Extend

Connect the ticket handlers to Zendesk, PagerDuty, and your audit logging system, and the SWITCH-based priority routing works unchanged.

- **AutoHandleWorker** (`sw_auto_handle`): auto-resolve LOW tickets using your knowledge base or chatbot API, send a canned response via Zendesk/Freshdesk, and close the ticket
- **TeamReviewWorker** (`sw_team_review`): assign MEDIUM tickets to the appropriate support team in your ticketing system based on category, send a Slack notification to the on-call agent
- **EscalateWorker** (`sw_escalate`): page a manager via PagerDuty or Opsgenie, set the ticket SLA to urgent, and notify the customer that their issue has been escalated
- **LogActionWorker** (`sw_log_action`): write the ticket action to your audit log, update SLA tracking metrics, and publish the event to your analytics pipeline
- **Add a new branch**: add a `CRITICAL` case to the SWITCH in workflow.json and create a CriticalHandleWorker; no existing code changes required

Connecting the handlers to real ticketing systems or adding new priority branches does not affect the SWITCH routing structure, since each worker just returns the handler result and the log entry.

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
switch-task/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/switchtask/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SwitchTaskExample.java       # Main entry point (runs all 4 scenarios)
│   └── workers/
│       ├── AutoHandleWorker.java
│       ├── EscalateWorker.java
│       ├── LogActionWorker.java
│       ├── TeamReviewWorker.java
│       └── UnknownPriorityWorker.java
└── src/test/java/switchtask/workers/
    ├── AutoHandleWorkerTest.java    # 7 tests
    ├── EscalateWorkerTest.java      # 7 tests
    ├── LogActionWorkerTest.java     # 7 tests
    ├── TeamReviewWorkerTest.java    # 7 tests
    └── UnknownPriorityWorkerTest.java # 7 tests

```
