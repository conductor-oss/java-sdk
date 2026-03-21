# Calendar Integration in Java Using Conductor :  Event Sync, Schedule Comparison, Change Propagation, and Notification

A Java Conductor workflow example for calendar integration .  fetching events from a calendar, comparing against the canonical schedule, syncing changes bidirectionally, and notifying stakeholders of schedule updates.

## The Problem

You need to keep calendars in sync .  when a meeting is added to Google Calendar, it needs to appear in your internal scheduling system, and vice versa. Events must be fetched from the source, compared against the target to find additions/deletions/changes, synced with conflict resolution, and stakeholders notified of any changes. If the sync step fails, changes are lost. If notifications fail, people miss schedule updates.

Without orchestration, calendar sync is a fragile cron job that overwrites one calendar with another. Conflict detection is minimal, notification is an afterthought, and a failure in the sync step leaves calendars permanently out of sync with no record of what went wrong.

## The Solution

**You just write the calendar API calls and conflict resolution rules. Conductor handles the fetch-compare-sync-notify sequence, retries when calendar APIs are temporarily unavailable, and a complete record of every sync operation and conflict resolved.**

Each sync concern is an independent worker .  event fetching, schedule comparison, change sync, and notification. Conductor runs them in sequence with retry logic, ensuring a temporary API failure doesn't cause permanent sync drift. Every sync operation is tracked ,  you can see what changed, what was synced, and who was notified. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle bidirectional sync: FetchEventsWorker pulls events from the source calendar, CompareSchedulesWorker diffs additions/deletions/conflicts, SyncChangesWorker applies the reconciled changes, and NotifyStakeholdersWorker alerts affected participants.

| Worker | Task | What It Does |
|---|---|---|
| **CompareSchedulesWorker** | `cal_compare_schedules` | Compares fetched external events against the internal schedule, counting additions, updates, deletions, and conflicts |
| **FetchEventsWorker** | `cal_fetch_events` | Fetches calendar events from a specified calendar source, returning event details and total count |
| **NotifyStakeholdersWorker** | `cal_notify_stakeholders` | Notifies stakeholders about synced schedule changes, returning the recipient count |
| **SyncChangesWorker** | `cal_sync_changes` | Applies additions, updates, and deletions to synchronize the target calendar with the source |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
cal_fetch_events
    │
    ▼
cal_compare_schedules
    │
    ▼
cal_sync_changes
    │
    ▼
cal_notify_stakeholders

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
java -jar target/calendar-integration-1.0.0.jar

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
java -jar target/calendar-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow calendar_integration_406 \
  --version 1 \
  --input '{"calendarId": "TEST-001", "syncWindow": "sample-syncWindow", "direction": "sample-direction"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w calendar_integration_406 -s COMPLETED -c 5

```

## How to Extend

Each worker manages one sync phase .  connect the event fetcher to Google Calendar or Outlook API, the notifier to Slack or email, and the fetch-compare-sync-notify workflow stays the same.

- **CompareSchedulesWorker** (`cal_compare_schedules`): diff events by time, title, and attendees to find additions, deletions, and modifications
- **FetchEventsWorker** (`cal_fetch_events`): pull events from Google Calendar, Microsoft Outlook, or Apple Calendar via their APIs
- **NotifyStakeholdersWorker** (`cal_notify_stakeholders`): send schedule change summaries via Slack, email, or push notifications

Connect to the Google Calendar or Outlook API, and the sync pipeline transfers directly to production with no workflow edits.

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
calendar-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/calendarintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CalendarIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompareSchedulesWorker.java
│       ├── FetchEventsWorker.java
│       ├── NotifyStakeholdersWorker.java
│       └── SyncChangesWorker.java
└── src/test/java/calendarintegration/workers/
    ├── FetchEventsWorkerTest.java        # 2 tests
    └── SyncChangesWorkerTest.java        # 2 tests

```
