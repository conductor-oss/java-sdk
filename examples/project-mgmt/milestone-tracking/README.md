# Milestone Tracking in Java with Conductor :  Progress Checking, Health Evaluation, Conditional Routing (On-Track / At-Risk / Delayed), and Action Execution

A Java Conductor workflow example that automates milestone tracking. checking progress by counting completed vs: total deliverables, evaluating milestone health to classify as on-track, at-risk, or delayed, routing to different response handlers based on the health status using a SWITCH task, and executing the appropriate action (continue as planned, escalate to the project lead, or trigger recovery). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Milestone Tracking Needs Orchestration

Tracking milestones requires more than checking a percentage. you need to evaluate progress against the deadline and take different actions depending on how healthy the milestone looks. You check progress by counting completed deliverables against the total (e.g., 7 of 10 done, 70% complete). You evaluate that progress against the timeline,  70% complete at 80% of the timeline elapsed means the milestone is at risk, not on track. Based on the health status, you take fundamentally different actions: on-track milestones continue as planned, at-risk milestones get escalated to the project lead for attention, and delayed milestones trigger recovery plans with scope re-negotiation or deadline extension.

The conditional routing is the key design decision. A milestone at 70% completion needs escalation to the lead, while a milestone at 90% completion just needs a status update. Without orchestration, you'd write nested if/else blocks mixing progress queries, evaluation logic, Slack notifications for escalation, and Jira updates for status changes. making it impossible to add a new health category (e.g., "blocked"), test your escalation logic independently from progress checking, or audit which milestones were escalated and what action was taken.

## How This Workflow Solves It

**You just write the progress checking, health evaluation, and on-track/at-risk/delayed response logic. Conductor handles progress collection retries, risk alerting, and milestone audit trails.**

Each tracking stage is an independent worker. check progress, evaluate, handle on-track/at-risk/delayed, act. Conductor sequences the progress check and evaluation, then uses a SWITCH task to route to the correct handler based on the health status. On-track milestones flow to the on-track handler (action: continue), at-risk milestones flow to the at-risk handler (action: escalate to lead), and delayed milestones flow to the delayed handler. After the SWITCH branch completes, all paths converge at the act worker which executes the recommended action. Conductor retries if your PM tool's API is unavailable during the progress check, and records the full evaluation trail for every milestone.

### What You Write: Workers

Progress collection, milestone evaluation, risk flagging, and status reporting workers each monitor one aspect of project timeline adherence.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `mst_act` | Taking action for milestone |
| **AtRiskWorker** | `mst_at_risk` | At Risk. Computes and returns action |
| **CheckProgressWorker** | `mst_check_progress` | Checking milestone |
| **DelayedWorker** | `mst_delayed` | Delayed. Computes and returns action |
| **EvaluateWorker** | `mst_evaluate` | Evaluates and computes pct done |
| **OnTrackWorker** | `mst_on_track` | On Track. Computes and returns action |

Workers implement project management operations. task creation, status updates, notifications,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
mst_check_progress
    │
    ▼
mst_evaluate
    │
    ▼
SWITCH (switch_ref)
    ├── on_track: mst_on_track
    ├── at_risk: mst_at_risk
    └── default: mst_delayed
    │
    ▼
mst_act

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
java -jar target/milestone-tracking-1.0.0.jar

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
java -jar target/milestone-tracking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow milestone_tracking_milestone-tracking \
  --version 1 \
  --input '{"milestoneId": "MS-Q1-2026", "MS-Q1-2026": "projectName", "projectName": "Project Alpha", "Project Alpha": "sample-Project Alpha"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w milestone_tracking_milestone-tracking -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real project tools. Jira or Asana for progress data, your escalation platform for at-risk alerts, your PM dashboard for status updates, and the workflow runs identically in production.

- **CheckProgressWorker** (`mst_check_progress`): query your PM tool (Jira, Asana, Linear) for the milestone's deliverables, count completed vs: total, and compute percent done from real task statuses
- **EvaluateWorker** (`mst_evaluate`): compare progress percentage against timeline percentage (how far into the milestone window you are), apply your health thresholds to classify as on_track, at_risk, or delayed
- **OnTrackWorker** (`mst_on_track`): log the healthy status to your project dashboard, update the milestone's status page, and optionally send a "green" status update to stakeholders
- **AtRiskWorker** (`mst_at_risk`): escalate to the project lead via Slack or PagerDuty, create an at-risk flag in your PM tool, and suggest mitigation actions (add resources, reduce scope)
- **DelayedWorker** (`mst_delayed`): trigger the recovery process: notify the project sponsor, create a recovery plan ticket, schedule a scope re-negotiation meeting, and update the milestone deadline
- **ActWorker** (`mst_act`): execute the recommended action from the SWITCH branch: update the milestone status in your PM tool, record the action taken for audit, and schedule the next tracking check

Update your progress data sources or risk criteria and the tracking pipeline continues unchanged.

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
milestone-tracking-milestone-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/milestonetracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MilestoneTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActWorker.java
│       ├── AtRiskWorker.java
│       ├── CheckProgressWorker.java
│       ├── DelayedWorker.java
│       ├── EvaluateWorker.java
│       └── OnTrackWorker.java
└── src/test/java/milestonetracking/workers/
    ├── ActWorkerTest.java        # 2 tests
    ├── AtRiskWorkerTest.java        # 2 tests
    ├── CheckProgressWorkerTest.java        # 2 tests
    ├── DelayedWorkerTest.java        # 2 tests
    ├── EvaluateWorkerTest.java        # 2 tests
    └── OnTrackWorkerTest.java        # 2 tests

```
