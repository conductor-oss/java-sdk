# On Call Rotation in Java with Conductor

Automates on-call rotation handoffs using [Conductor](https://github.com/conductor-oss/conductor). This workflow checks the rotation schedule, hands off active incidents between engineers, updates PagerDuty/alerting routing rules, and confirms the new on-call has acknowledged.

## Seamless On-Call Handoffs

It is Monday morning and on-call shifts from Alice to Bob. Two incidents are still active. The handoff needs to happen cleanly: Alice briefs Bob on open issues, PagerDuty routing updates so new alerts go to Bob, and Bob confirms he is ready. If any step is missed, alerts fall into a black hole or wake up the wrong person at 3 AM.

Without orchestration, you'd rely on a shared Google Calendar and a Slack message saying "you're on-call now." PagerDuty routing stays pointed at Alice until someone remembers to update it. Active incidents don't get handed off. Bob discovers them when a customer escalates. If the routing update fails silently, alerts go to the wrong person for hours. There's no record of whether the handoff actually completed, who acknowledged, or which incidents were transferred.

## The Solution

**You write the schedule lookup and routing update logic. Conductor handles handoff sequencing, confirmation gates, and rotation audit trails.**

Each stage of the on-call handoff is a simple, independent worker. The schedule checker looks up the rotation to determine who is currently on-call and who is next. Supporting weekly, daily, or custom rotation types per team. The handoff worker transfers active incidents from the outgoing to the incoming engineer, summarizing open issues, severity levels, and any context needed to pick up where the previous on-call left off. The routing updater changes PagerDuty escalation policies and alerting rules so new alerts reach the right person immediately. The confirmation worker verifies the incoming on-call has acknowledged the handoff and is ready to respond. Conductor executes them in strict sequence, ensures routing only updates after incidents are transferred, retries if PagerDuty's API is temporarily unavailable, and tracks every handoff so you can audit who was on-call when. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage the handoff. Checking the schedule, transferring incident context, updating alerting routes, and confirming acknowledgment.

| Worker | Task | What It Does |
|---|---|---|
| **CheckScheduleWorker** | `oc_check_schedule` | Checks the rotation schedule to determine who is going off-call and who is coming on (e.g., Alice to Bob) |
| **ConfirmWorker** | `oc_confirm` | Verifies the new on-call engineer has acknowledged and is ready to receive alerts |
| **HandoffWorker** | `oc_handoff` | Transfers active incident context from the outgoing to the incoming on-call engineer |
| **UpdateRoutingWorker** | `oc_update_routing` | Updates PagerDuty/Opsgenie routing rules to direct new alerts to the incoming on-call |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
oc_check_schedule
    │
    ▼
oc_handoff
    │
    ▼
oc_update_routing
    │
    ▼
oc_confirm

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
java -jar target/on-call-rotation-1.0.0.jar

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
java -jar target/on-call-rotation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow on_call_rotation_workflow \
  --version 1 \
  --input '{"team": "sample-team", "rotationType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w on_call_rotation_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one handoff step. replace the simulated calls with PagerDuty Schedules API, Opsgenie routing rules, or Slack interactive messages, and the rotation workflow runs unchanged.

- **CheckScheduleWorker** (`oc_check_schedule`): query PagerDuty Schedules API or Opsgenie On-Call API to determine current and next on-call engineers, supporting weekly, daily, or follow-the-sun rotation types
- **HandoffWorker** (`oc_handoff`): pull active incidents from PagerDuty Incidents API, compile severity and context summaries, and post the handoff brief to the incoming on-call via Slack DM
- **UpdateRoutingWorker** (`oc_update_routing`): update PagerDuty escalation policies, Opsgenie routing rules, or Slack on-call group membership so new alerts reach the incoming engineer immediately
- **ConfirmWorker** (`oc_confirm`): send a Slack interactive message or PagerDuty acknowledgment request, waiting for the incoming on-call to explicitly confirm they are ready to respond

Integrate with PagerDuty and your calendar system; the rotation workflow uses the same handoff interface.

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
on-call-rotation-on-call-rotation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/oncallrotation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckScheduleWorker.java
│       ├── ConfirmWorker.java
│       ├── HandoffWorker.java
│       └── UpdateRoutingWorker.java
└── src/test/java/oncallrotation/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
