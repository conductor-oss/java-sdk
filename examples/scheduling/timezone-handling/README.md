# Timezone Handling in Java Using Conductor :  Zone Detection, Time Conversion, and Timezone-Aware Scheduling

A Java Conductor workflow example for timezone handling .  detecting a user's timezone, converting requested times to UTC, and scheduling jobs in the correct timezone context.

## The Problem

Your users are in different timezones. When a user in Tokyo schedules a job for "9:00 AM", that's 9:00 AM JST, not UTC. You need to detect the user's timezone, convert the requested time to UTC for storage and scheduling, and execute the job at the correct moment regardless of where your servers are. Getting timezone conversion wrong means jobs run at the wrong time for every user.

Without orchestration, timezone handling is scattered across the codebase .  one method converts to UTC, another assumes local time, and a third ignores timezones entirely. Daylight saving time transitions cause jobs to run an hour early or late. Nobody can tell what timezone a scheduled job is stored in.

## The Solution

**You just write the timezone detection and UTC conversion logic. Conductor handles the detect-convert-schedule-execute sequence, retries when timezone lookups or scheduling APIs are unavailable, and a record of every conversion showing original time, detected zone, and UTC result.**

Each timezone concern is an independent worker .  zone detection, time conversion, and job scheduling. Conductor runs them in sequence: detect the user's timezone, convert the requested time to UTC, then schedule the job. Every timezone conversion is tracked with the original time, detected zone, and UTC result. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

DetectZoneWorker identifies the user's IANA timezone, ConvertTimeWorker translates the requested time to UTC, ScheduleJobWorker queues the job at the correct UTC instant, and ExecuteJobWorker runs it when the moment arrives.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertTimeWorker** | `tz_convert_time` | Converts a requested time from the user's source timezone to the target timezone, returning the offset |
| **DetectZoneWorker** | `tz_detect_zone` | Detects a user's timezone and returns the IANA zone name, UTC offset, and DST status |
| **ExecuteJobWorker** | `tz_execute_job` | Executes the scheduled job at the correct UTC time, returning completion timestamp and duration |
| **ScheduleJobWorker** | `tz_schedule_job` | Schedules a job at the converted UTC time and returns a job ID for tracking |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tz_detect_zone
    │
    ▼
tz_convert_time
    │
    ▼
tz_schedule_job
    │
    ▼
tz_execute_job
```

## Example Output

```
=== Example 407: Timezone Handling ===

Step 1: Registering task definitions...
  Registered: tz_detect_zone, tz_convert_time, tz_schedule_job, tz_execute_job

Step 2: Registering workflow 'timezone_handling_407'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [convert] Converting
  [detect] Detecting timezone for user
  [execute] Executing \"" + task.getInputData().get("jobName") + "\" at
  [schedule] Scheduling \"" + task.getInputData().get("jobName") + "\" at

  Status: COMPLETED
  Output: {convertedTime=..., originalTime=..., offset=..., timezone=...}

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
java -jar target/timezone-handling-1.0.0.jar
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
java -jar target/timezone-handling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow timezone_handling_407 \
  --version 1 \
  --input '{"userId": "user-jp-442", "user-jp-442": "requestedTime", "requestedTime": "2026-03-09T02:00:00+09:00", "2026-03-09T02:00:00+09:00": "jobName", "jobName": "daily-backup", "daily-backup": "sample-daily-backup"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w timezone_handling_407 -s COMPLETED -c 5
```

## How to Extend

Each worker manages one timezone concern .  connect the zone detector to your user profile service, the scheduler to your job queue, and the detect-convert-schedule workflow stays the same.

- **ConvertTimeWorker** (`tz_convert_time`): use java.time.ZonedDateTime for real timezone conversions with DST handling, or call a timezone API for complex rules
- **DetectZoneWorker** (`tz_detect_zone`): determine the user's timezone from their profile, browser geolocation, or IP-based geo-lookup (MaxMind)
- **ExecuteJobWorker** (`tz_execute_job`): run the actual job at the scheduled time with timezone context available in the execution metadata

Wire in your real geo-lookup service and job scheduler, and the timezone-aware pipeline adapts without any orchestration changes.

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
timezone-handling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/timezonehandling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TimezoneHandlingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConvertTimeWorker.java
│       ├── DetectZoneWorker.java
│       ├── ExecuteJobWorker.java
│       └── ScheduleJobWorker.java
└── src/test/java/timezonehandling/workers/
    ├── DetectZoneWorkerTest.java        # 2 tests
    └── ExecuteJobWorkerTest.java        # 2 tests
```
