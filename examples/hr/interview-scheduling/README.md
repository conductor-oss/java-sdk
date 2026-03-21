# Interview Scheduling in Java with Conductor :  Availability Check, Slot Selection, Candidate Invite, Confirmation, and Reminder

A Java Conductor workflow example for interview scheduling. checking interviewer panel calendar availability, selecting the best time slot, sending the candidate an invite with video link and details, confirming all participants, and sending day-of reminders. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to coordinate interview scheduling across multiple busy interviewers and a candidate. Given an interview panel (e.g., hiring manager, two engineers, a product lead), you must find time slots where all interviewers are free. The best available slot is selected, and the candidate receives an invite with the date, time, role description, interviewer names, and a video conference link. Each interviewer must confirm their participation. On the day of the interview, all parties receive reminders with the agenda and join details. If any interviewer's calendar changes after booking, the system needs to detect the conflict and reschedule. Coordinating this manually across four or five calendars through back-and-forth emails regularly takes days and creates a poor candidate experience.

Without orchestration, you'd build a scheduling tool that queries each interviewer's calendar API, finds overlapping free slots, sends emails, polls for confirmations, and fires off reminders. If Google Calendar's API rate-limits you while checking availability for a large panel, you'd need retry logic. If the system crashes after sending the candidate invite but before the interviewers are confirmed, you have a scheduled interview that interviewers don't know about. Recruiters need visibility into which interviews are confirmed versus pending so they can follow up on stragglers.

## The Solution

**You just write the availability checking, slot selection, candidate invitation, confirmation, and reminder logic. Conductor handles calendar booking retries, availability matching, and scheduling audit trails.**

Each stage of interview scheduling is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of checking availability before scheduling, scheduling before inviting, confirming all participants after the invite is sent, sending reminders only after confirmation, retrying if a calendar API is temporarily unavailable, and giving recruiters complete visibility into every interview's scheduling status. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Availability collection, slot matching, calendar booking, and reminder workers coordinate interviews without coupling to the hiring decision logic.

| Worker | Task | What It Does |
|---|---|---|
| **AvailabilityWorker** | `ivs_availability` | Queries each interviewer's calendar and returns overlapping available slots (e.g., "2024-03-25 10:00", "2024-03-25 14:00", "2024-03-26 11:00") |
| **ScheduleWorker** | `ivs_schedule` | Selects the optimal time slot from available options, books the room or video link, and assigns an interview ID |
| **InviteWorker** | `ivs_invite` | Sends the candidate an interview invitation with the scheduled time, interviewer names, role details, and video join link |
| **ConfirmWorker** | `ivs_confirm` | Confirms all interviewers have accepted the calendar hold and flags any who have declined or not responded |
| **RemindWorker** | `ivs_remind` | Sends day-of reminders to the candidate and all interviewers with the agenda, scorecard link, and join instructions |

Workers implement HR operations. onboarding tasks, approvals, provisioning,  with realistic outputs. Replace with real HRIS and identity provider integrations and the workflow stays the same.

### The Workflow

```
ivs_availability
    │
    ▼
ivs_schedule
    │
    ▼
ivs_invite
    │
    ▼
ivs_confirm
    │
    ▼
ivs_remind

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
java -jar target/interview-scheduling-1.0.0.jar

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
java -jar target/interview-scheduling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ivs_interview_scheduling \
  --version 1 \
  --input '{"candidateName": "test", "interviewers": "sample-interviewers", "role": "user"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ivs_interview_scheduling -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real scheduling tools. Google Calendar or Outlook for availability checks, your ATS for candidate communication, Zoom or Teams for video link generation, and the workflow runs identically in production.

- **AvailabilityWorker** → query real calendar APIs (Google Calendar, Microsoft Graph) for each interviewer's free/busy data and find overlapping availability windows
- **ScheduleWorker** → book conference rooms via your room management system, create Zoom/Teams meetings, and optimize slot selection based on candidate timezone and interviewer preferences
- **InviteWorker** → send branded candidate emails through your ATS (Greenhouse, Lever) with role-specific interview prep materials and interviewer bios
- **ConfirmWorker** → monitor calendar event RSVPs and automatically trigger rescheduling if an interviewer declines or has a conflict
- **RemindWorker** → send multi-channel reminders (email + Slack) with scorecard links, candidate resume, and the structured interview rubric for each interviewer
- Add a **RescheduleWorker** to handle cancellations by finding the next available slot and re-running the invite/confirm flow
- Add a **FeedbackWorker** after the interview to collect scorecards from each interviewer and aggregate ratings in your ATS

Switch calendar providers or add new scheduling constraints and the pipeline adjusts transparently.

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
interview-scheduling-interview-scheduling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/interviewscheduling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InterviewSchedulingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AvailabilityWorker.java
│       ├── ConfirmWorker.java
│       ├── InviteWorker.java
│       ├── RemindWorker.java
│       └── ScheduleWorker.java
└── src/test/java/interviewscheduling/workers/
    ├── AvailabilityWorkerTest.java        # 2 tests
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── InviteWorkerTest.java        # 2 tests
    ├── RemindWorkerTest.java        # 2 tests
    └── ScheduleWorkerTest.java        # 2 tests

```
