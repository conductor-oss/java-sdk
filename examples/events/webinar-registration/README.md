# Webinar Registration in Java with Conductor

A Java Conductor workflow that manages the end-to-end webinar registration experience. registering the attendee, sending a confirmation email, delivering reminder notifications (24h and 1h before the event), and sending a post-webinar follow-up with the recording link. Given an attendee name, email, and webinar ID, the pipeline handles the full attendee lifecycle. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the register-confirm-remind-followup pipeline.

## Getting Attendees Registered, Reminded, and Followed Up

A webinar registration workflow is more than just recording a name. the attendee needs to be registered, sent a confirmation email, reminded before the event (24 hours and 1 hour out), and followed up afterward with a recording link and next steps. Missing any step means lower attendance rates and lost engagement opportunities. Each step depends on the previous one,  you cannot send a confirmation until registration succeeds, and follow-up only makes sense after the event.

This workflow manages a single attendee's webinar journey. The registrar records the attendee and generates a registration ID. The confirmer sends a confirmation email with event details and a calendar link. The reminder sends timed notifications before the webinar starts. The follow-up worker sends a post-event email with the recording link and related resources.

## The Solution

**You just write the registration, confirmation, reminder, and follow-up workers. Conductor handles the attendee lifecycle and timed notification sequencing.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

RegisterWorker records the attendee, ConfirmWorker sends event details with a calendar invite, RemindWorker delivers timed notifications at 24h and 1h before, and FollowupWorker sends the recording link afterward.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmWorker** | `wbr_confirm` | Sends a confirmation email with event details and a calendar invite link. |
| **FollowupWorker** | `wbr_followup` | Sends a post-webinar email with the recording link and related resources. |
| **RegisterWorker** | `wbr_register` | Registers the attendee for the webinar and generates a unique registration ID. |
| **RemindWorker** | `wbr_remind` | Sends reminder notifications at 24 hours and 1 hour before the event. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
wbr_register
    │
    ▼
wbr_confirm
    │
    ▼
wbr_remind
    │
    ▼
wbr_followup

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
java -jar target/webinar-registration-1.0.0.jar

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
java -jar target/webinar-registration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wbr_webinar_registration \
  --version 1 \
  --input '{"attendeeName": "test", "email": "user@example.com", "webinarId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wbr_webinar_registration -s COMPLETED -c 5

```

## How to Extend

Each worker handles one attendee step. connect your webinar platform (Zoom, GoTo, Webex) for registration and your email service (SendGrid, Mailchimp) for confirmations and follow-ups, and the webinar workflow stays the same.

- **ConfirmWorker** (`wbr_confirm`): integrate with an email service (SendGrid, SES) for real confirmation emails with calendar attachments
- **RegisterWorker** (`wbr_register`): connect to your webinar platform API (Zoom, GoToWebinar, Webex) for real attendee registration
- **RemindWorker** (`wbr_remind`): use a scheduling service or delayed task execution for timed reminder notifications

Replace the simulated email service with SendGrid and the register-confirm-remind-followup attendee lifecycle operates unchanged.

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
webinar-registration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webinarregistration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebinarRegistrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmWorker.java
│       ├── FollowupWorker.java
│       ├── RegisterWorker.java
│       └── RemindWorker.java
└── src/test/java/webinarregistration/workers/
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── FollowupWorkerTest.java        # 2 tests
    ├── RegisterWorkerTest.java        # 2 tests
    └── RemindWorkerTest.java        # 2 tests

```
