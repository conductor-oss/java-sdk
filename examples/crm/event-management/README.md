# Event Management in Java with Conductor :  Plan, Register, Execute, and Follow Up on Events

A Java Conductor workflow that manages an event lifecycle .  planning the event with venue and schedule details, processing attendee registrations, executing the event, and sending post-event follow-ups. Given an `eventName`, `date`, and `capacity`, the pipeline produces a venue assignment, registration count, execution status, and follow-up metrics. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-phase event lifecycle.

## Running Events Without Dropping the Ball

Events have a strict lifecycle: plan the logistics, open and manage registrations, execute the event itself, then follow up with attendees. Each phase must complete before the next begins .  you cannot register attendees for an unplanned event, and you cannot follow up before the event happens. Missing a phase or running them out of order results in confused attendees, overbooked venues, or missed follow-ups that waste the event's value.

This workflow enforces the event lifecycle. The planner sets up the event with venue, schedule, and capacity. The registration worker processes attendee sign-ups. The execution worker manages the live event. The follow-up worker sends thank-you emails, surveys, and next-step communications to attendees. Each step's output feeds the next .  the event plan feeds registration, registration counts feed execution, and attendee lists feed follow-up.

## The Solution

**You just write the event-planning, registration, execution, and follow-up workers. Conductor handles the phase sequencing and attendee data flow.**

Four workers handle the event lifecycle .  planning, registration, execution, and follow-up. The planner creates the event with venue and capacity. The registration worker processes attendees. The executor runs the event. The follow-up worker sends post-event communications. Conductor sequences the four phases and passes event details, attendee lists, and execution data between them automatically.

### What You Write: Workers

PlanWorker sets up venue and capacity, RegisterAttendeesWorker processes sign-ups, ExecuteEventWorker manages the live event, and FollowupWorker sends post-event surveys and communications.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteEventWorker** | `evt_execute` | Manages the live event and records actual attendance numbers. |
| **FollowupWorker** | `evt_followup` | Sends post-event surveys, thank-you emails, and next-step communications to attendees. |
| **PlanWorker** | `evt_plan` | Sets up the event with venue, schedule, and capacity details. |
| **RegisterAttendeesWorker** | `evt_register` | Processes attendee registrations up to the event's capacity limit. |

Workers simulate CRM operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
evt_plan
    │
    ▼
evt_register
    │
    ▼
evt_execute
    │
    ▼
evt_followup
```

## Example Output

```
=== Example 627: Event Management ===

Step 1: Registering task definitions...
  Registered: evt_plan, evt_register, evt_execute, evt_followup

Step 2: Registering workflow 'evt_event_management'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [execute] Event executed:
  [followup] Post-event surveys sent to
  [plan] Event \"" + task.getInputData().get("eventName") + "\" planned for
  [register]

  Status: COMPLETED
  Output: {actualAttendees=..., sessions=..., speakersPresent=..., surveySent=...}

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
java -jar target/event-management-1.0.0.jar
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
java -jar target/event-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow evt_event_management \
  --version 1 \
  --input '{"eventName": "sample-name", "DevOps Summit 2024": "sample-DevOps Summit 2024", "date": "2025-01-15T10:00:00Z", "2024-11-15": "sample-2024-11-15", "capacity": "sample-capacity"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w evt_event_management -s COMPLETED -c 5
```

## How to Extend

Each worker handles one event phase .  connect your event platform (Eventbrite, Splash, Hopin) for registration and your email service (SendGrid, Mailchimp) for follow-ups, and the event-lifecycle workflow stays the same.

- **ExecuteEventWorker** (`evt_execute`): integrate with webinar platforms (Zoom, Teams) or event platforms (Eventbrite) for live event management
- **FollowupWorker** (`evt_followup`): connect to email platforms (SendGrid, Mailchimp) and survey tools (Typeform) for post-event outreach
- **PlanWorker** (`evt_plan`): integrate with venue booking APIs and calendar systems for real event planning

Connect your event platform and email service and the plan-register-execute-followup lifecycle runs without any workflow definition changes.

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
event-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteEventWorker.java
│       ├── FollowupWorker.java
│       ├── PlanWorker.java
│       └── RegisterAttendeesWorker.java
└── src/test/java/eventmanagement/workers/
    ├── ExecuteEventWorkerTest.java        # 2 tests
    ├── FollowupWorkerTest.java        # 2 tests
    ├── PlanWorkerTest.java        # 2 tests
    └── RegisterAttendeesWorkerTest.java        # 2 tests
```
