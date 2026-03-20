# Volunteer Coordination in Java with Conductor

A Java Conductor workflow example demonstrating Volunteer Coordination. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A new volunteer signs up to help at your nonprofit. The volunteer coordination team needs to register the volunteer with their skills and availability, match them to an appropriate opportunity (e.g., food bank sorting), schedule their shift at a specific location and time, track their hours worked and events attended, and send a personalized thank-you acknowledging their contribution. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the volunteer registration, skill matching, shift scheduling, and engagement tracking logic. Conductor handles screening retries, assignment routing, and volunteer engagement audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Recruitment, screening, assignment, and hour tracking workers each manage one aspect of the volunteer engagement lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **MatchWorker** | `vol_match` | Matches the volunteer to an opportunity based on their skills, returning the opportunity name and location |
| **RegisterWorker** | `vol_register` | Registers the volunteer by name, assigning a unique volunteer ID |
| **ScheduleWorker** | `vol_schedule` | Schedules the volunteer for a specific date and shift at the matched opportunity |
| **ThankWorker** | `vol_thank` | Sends a personalized thank-you to the volunteer acknowledging their hours contributed |
| **TrackWorker** | `vol_track` | Logs the volunteer's hours for the session and updates their cumulative total hours and events attended |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
vol_register
    │
    ▼
vol_match
    │
    ▼
vol_schedule
    │
    ▼
vol_track
    │
    ▼
vol_thank
```

## Example Output

```
=== Example 753: Volunteer Coordinatio ===

Step 1: Registering task definitions...
  Registered: vol_register, vol_match, vol_schedule, vol_track, vol_thank

Step 2: Registering workflow 'volunteer_coordination_753'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [match] Matching volunteer
  [register] Registering volunteer:
  [schedule] Scheduling
  [thank] Thanking
  [track] Tracking hours for volunteer

  Status: COMPLETED

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
java -jar target/volunteer-coordination-1.0.0.jar
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
java -jar target/volunteer-coordination-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow volunteer_coordination_753 \
  --version 1 \
  --input '{"volunteerName": "sample-name", "Maria Garcia": "sample-Maria Garcia", "skills": "sample-skills", "organization": "sample-organization", "availability": "sample-availability"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w volunteer_coordination_753 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real volunteer systems .  your volunteer management platform for registration, your scheduling tool for shift assignment, your communications service for notifications, and the workflow runs identically in production.

- **RegisterWorker** (`vol_register`): create the volunteer record in your volunteer management platform (VolunteerHub, Galaxy Digital) or Salesforce Volunteers, capturing skills and availability
- **MatchWorker** (`vol_match`): query open opportunities from your volunteer management system and match based on skills, location, and availability using your matching algorithm
- **ScheduleWorker** (`vol_schedule`): book the volunteer's shift via your scheduling platform and send a calendar invite through Google Calendar API or Outlook
- **TrackWorker** (`vol_track`): log hours from your check-in system (SignUpGenius, VolunteerHub) and update the volunteer's cumulative record in your CRM
- **ThankWorker** (`vol_thank`): send a personalized thank-you email via SendGrid or your CRM's email integration, optionally including a volunteer impact summary

Change your scheduling tool or screening process and the coordination pipeline operates identically.

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
volunteer-coordination/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/volunteercoordination/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VolunteerCoordinationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MatchWorker.java
│       ├── RegisterWorker.java
│       ├── ScheduleWorker.java
│       ├── ThankWorker.java
│       └── TrackWorker.java
└── src/test/java/volunteercoordination/workers/
    ├── RegisterWorkerTest.java        # 1 tests
    └── ThankWorkerTest.java        # 1 tests
```
