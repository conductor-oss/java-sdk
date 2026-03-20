# Tutoring Match in Java with Conductor :  Request Intake, Tutor Matching, Session Scheduling, and Confirmation

A Java Conductor workflow example for matching students with tutors .  receiving a tutoring request with subject and time preferences, finding an available tutor qualified in that subject, scheduling a session at the preferred time, and confirming the booking with both student and tutor. Uses [Conductor](https://github.## The Problem

You need to connect students who need help with qualified tutors. A student requests tutoring in a specific subject at a preferred time, the system must find a tutor who is both qualified in that subject and available during that time slot, a session is scheduled, and both parties need confirmation with the session details. Matching a student with a tutor who is unavailable wastes everyone's time; scheduling without confirming availability leads to no-shows.

Without orchestration, you'd build a single matching service that queries the tutor database, checks availability calendars, creates calendar events, and sends confirmation emails .  manually handling the case where a tutor's availability changes between matching and scheduling, retrying failed calendar API calls, and logging every step to investigate complaints about missed or double-booked sessions.

## The Solution

**You just write the request intake, tutor matching, session scheduling, and booking confirmation logic. Conductor handles availability retries, match scoring, and session scheduling audit trails.**

Each tutoring-match concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (request, match, schedule, confirm), retrying if the calendar service is temporarily unavailable, tracking every tutoring request from intake to confirmed session, and resuming from the last successful step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Student profiling, tutor availability checking, compatibility scoring, and session scheduling workers each contribute to finding the right tutor match.

| Worker | Task | What It Does |
|---|---|---|
| **StudentRequestWorker** | `tut_student_request` | Records the student's tutoring request with subject and time preference |
| **MatchTutorWorker** | `tut_match_tutor` | Finds an available tutor qualified in the requested subject at the preferred time |
| **ScheduleWorker** | `tut_schedule` | Creates a tutoring session at the agreed time for the matched student-tutor pair |
| **ConfirmWorker** | `tut_confirm` | Sends session confirmation to both the student and the tutor |

Workers simulate educational operations .  enrollment, grading, notifications ,  with realistic outputs. Replace with real LMS and SIS integrations and the workflow stays the same.

### The Workflow

```
tut_student_request
    │
    ▼
tut_match_tutor
    │
    ▼
tut_schedule
    │
    ▼
tut_confirm
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
java -jar target/tutoring-match-1.0.0.jar
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
java -jar target/tutoring-match-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tut_tutoring_match \
  --version 1 \
  --input '{"studentId": "TEST-001", "subject": "test-value", "preferredTime": "2026-01-01T00:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tut_tutoring_match -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real tutoring systems .  your tutor roster database for matching, Google Calendar or Calendly for availability and scheduling, Twilio or SendGrid for confirmation notifications, and the workflow runs identically in production.

- **StudentRequestWorker** (`tut_student_request`): persist the request to your tutoring center database and validate the student's eligibility for tutoring services
- **MatchTutorWorker** (`tut_match_tutor`): query your tutor roster database for subject-qualified tutors, check their availability via Google Calendar API or Calendly, and apply matching preferences (rating, language, past sessions)
- **ScheduleWorker** (`tut_schedule`): create a calendar event via Google Calendar or Microsoft Graph API, book a room via your facility scheduling system, and set up a video link (Zoom API) for virtual sessions
- **ConfirmWorker** (`tut_confirm`): send confirmation emails/SMS (SendGrid, Twilio) to both student and tutor with session details, location, and preparation materials

Update your matching algorithm or availability source and the matching pipeline adapts without modification.

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
tutoring-match/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tutoringmatch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TutoringMatchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmWorker.java
│       ├── MatchTutorWorker.java
│       ├── ScheduleWorker.java
│       └── StudentRequestWorker.java
└── src/test/java/tutoringmatch/workers/
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── MatchTutorWorkerTest.java        # 2 tests
    ├── ScheduleWorkerTest.java        # 2 tests
    └── StudentRequestWorkerTest.java        # 2 tests
```
