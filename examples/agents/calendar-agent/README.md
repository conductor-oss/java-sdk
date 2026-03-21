# Calendar Agent in Java Using Conductor :  Parse Requests, Check Availability, Find Slots, Book Meetings

Calendar Agent. parse meeting request, check attendee calendars, find available slots, and book the meeting through a sequential pipeline. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Meeting Scheduling Is a Constraint Satisfaction Problem

Finding a time that works for three people across different time zones with existing commitments is tedious. The agent needs to parse the request ("1-hour meeting with Alice and Bob, preferably next Tuesday afternoon"), check each attendee's calendar for existing events, find overlapping free windows that satisfy the duration and preference constraints, and book the meeting with proper invitations.

Each step depends on the previous one. you can't find available slots without knowing existing commitments, and you can't book without finding a valid slot. If the calendar API is temporarily unavailable, you need to retry the availability check without re-parsing the request. And if the booking fails because someone accepted another meeting in the meantime, you need to find the next available slot.

## The Solution

**You write the request parsing, calendar queries, slot-finding, and booking logic. Conductor handles the scheduling pipeline, retries on calendar API failures, and booking state.**

`ParseRequestWorker` extracts structured meeting parameters from the natural language request. attendees, duration, preferred date, and any constraints (morning only, avoid Fridays). `CheckCalendarWorker` queries each attendee's calendar and returns their existing commitments with busy/free blocks. `FindSlotsWorker` computes overlapping availability windows that satisfy the duration and preference constraints, ranking them by preference score. `BookMeetingWorker` creates the calendar event in the best available slot and sends invitations to all attendees. Conductor sequences these four steps and records each one's output for scheduling analytics.

### What You Write: Workers

Four workers handle scheduling. Parsing the meeting request, checking attendee calendars, finding available slots, and booking the meeting.

| Worker | Task | What It Does |
|---|---|---|
| **BookMeetingWorker** | `cl_book_meeting` | Books the selected meeting slot. creates the calendar event, sends invitations, and returns the confirmed meeting de.. |
| **CheckCalendarWorker** | `cl_check_calendar` | Checks calendar availability for all attendees across the specified date range. Returns per-date time slots with avai... |
| **FindSlotsWorker** | `cl_find_slots` | Finds the best meeting slots from the availability data, applying the caller's preferences (morning preference, avoid... |
| **ParseRequestWorker** | `cl_parse_request` | Parses a natural-language meeting request into structured data: attendees with timezones, duration, date range, prefe... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
cl_parse_request
    │
    ▼
cl_check_calendar
    │
    ▼
cl_find_slots
    │
    ▼
cl_book_meeting

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
java -jar target/calendar-agent-1.0.0.jar

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
java -jar target/calendar-agent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow calendar_agent \
  --version 1 \
  --input '{"action": "schedule", "title": "Team standup", "date": "2026-03-25", "time": "09:00", "attendees": ["alice@example.com"]}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w calendar_agent -s COMPLETED -c 5

```

## How to Extend

Each worker handles one scheduling step. Integrate Google Calendar or Microsoft Graph for availability checks, timezone-aware slot computation, and real event creation with Zoom/Teams links, and the parse-check-find-book workflow runs unchanged.

- **CheckCalendarWorker** (`cl_check_calendar`): integrate with Google Calendar API's FreeBusy endpoint, Microsoft Graph Calendar API, or CalDAV servers to check real availability across providers
- **FindSlotsWorker** (`cl_find_slots`): implement timezone-aware slot finding using Java's ZonedDateTime, with support for recurring availability patterns and working hours preferences per attendee
- **BookMeetingWorker** (`cl_book_meeting`): create real calendar events via Google Calendar API `events.insert()`, send Zoom/Teams meeting links, and post confirmation to Slack channels

Connect to Google Calendar or Microsoft Graph; the scheduling pipeline preserves the same parse-check-find-book interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
calendar-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/calendaragent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CalendarAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookMeetingWorker.java
│       ├── CheckCalendarWorker.java
│       ├── FindSlotsWorker.java
│       └── ParseRequestWorker.java
└── src/test/java/calendaragent/workers/
    ├── BookMeetingWorkerTest.java        # 9 tests
    ├── CheckCalendarWorkerTest.java        # 9 tests
    ├── FindSlotsWorkerTest.java        # 9 tests
    └── ParseRequestWorkerTest.java        # 9 tests

```
