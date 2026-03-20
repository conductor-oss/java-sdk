# Post Mortem Automation in Java with Conductor

Automates post-incident post-mortem generation using [Conductor](https://github.com/conductor-oss/conductor). This workflow gathers the incident timeline from alerts and response events, collects impact metrics (affected users, availability), drafts a structured post-mortem document with action items, and schedules a blameless review meeting. You write the post-mortem logic, Conductor handles retries, failure routing, durability, and observability for free.

## Learning From Incidents

Incident INC-2024-042 is resolved. Now the real work starts: piecing together what happened, when, and why. You need to reconstruct a timeline from 24 scattered events across PagerDuty, Slack, and deploy logs. You need to know the blast radius. 1,200 affected users, availability dropped to 99.2%. You need a structured document with root cause, contributing factors, and action items. And you need a review meeting on the calendar before the details fade from memory.

Without orchestration, someone opens a blank Google Doc, spends two hours scrubbing through PagerDuty alerts and Slack threads to reconstruct what happened, guesses at the impact numbers, writes action items that nobody tracks, and forgets to schedule the review meeting. By the time the post-mortem lands, it is two weeks late, missing key details, and the action items never get completed. There's no consistent format across incidents, no systematic impact measurement, and no guarantee the review actually happens.

## The Solution

**You write the timeline reconstruction and impact analysis logic. Conductor handles data gathering sequencing, document assembly, and follow-through tracking.**

Each stage of the post-mortem pipeline is a simple, independent worker. The timeline gatherer reconstructs the incident chronology from PagerDuty alerts, Slack messages, and deploy events. Building an ordered sequence of the 24 events that made up INC-2024-042. The metrics collector measures the blast radius: affected user count, availability drop, error rate spike, and duration of impact. The document drafter assembles a structured post-mortem from a template, populating the timeline, impact data, root cause section, and placeholder action items for the team to refine. The review scheduler creates a calendar invite for the blameless review meeting with all responders. Conductor executes them in strict sequence, ensures the document is only drafted after timeline and metrics are collected, retries if PagerDuty or Slack APIs are temporarily unavailable, and tracks every post-mortem so you can audit incident follow-through. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers assemble the post-mortem. Gathering the incident timeline, collecting impact metrics, drafting the document, and scheduling the review meeting.

| Worker | Task | What It Does |
|---|---|---|
| **CollectMetricsWorker** | `pm_collect_metrics` | Pulls impact metrics from the incident window (affected users, availability percentage, error rates) |
| **DraftDocumentWorker** | `pm_draft_document` | Creates a structured post-mortem document with timeline, impact summary, root cause, and action items |
| **GatherTimelineWorker** | `pm_gather_timeline` | Builds the incident timeline by collecting alert triggers, response actions, and resolution events |
| **ScheduleReviewWorker** | `pm_schedule_review` | Schedules a blameless post-mortem review meeting with the involved team |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
pm_gather_timeline
    │
    ▼
pm_collect_metrics
    │
    ▼
pm_draft_document
    │
    ▼
pm_schedule_review
```

## Example Output

```
=== Example 337: Post-Mortem Automatio ===

Step 1: Registering task definitions...
  Registered: pm_gather_timeline, pm_collect_metrics, pm_draft_document, pm_schedule_review

Step 2: Registering workflow 'post_mortem_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [metrics] Impact: 1,200 affected users, 99.2% availability during incident
  [draft] Post-mortem document generated with timeline and action items
  [timeline] Gathered 24 events for incident INC-2024-042
  [review] Review meeting scheduled for next Tuesday

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
java -jar target/post-mortem-automation-1.0.0.jar
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
java -jar target/post-mortem-automation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow post_mortem_workflow \
  --version 1 \
  --input '{"incidentId": "INC-2024-042", "INC-2024-042": "severity", "severity": "P1", "P1": "sample-P1"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w post_mortem_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one post-mortem stage .  replace the simulated calls with PagerDuty timeline APIs, Datadog incident metrics, or Google Calendar scheduling, and the post-mortem workflow runs unchanged.

- **GatherTimelineWorker** (`pm_gather_timeline`): query PagerDuty Incidents API for alert and acknowledgment timestamps, pull Slack message history for the incident channel, and correlate with deploy events from CI/CD to build a chronological event sequence
- **CollectMetricsWorker** (`pm_collect_metrics`): query Datadog or Prometheus for error rates, latency, and availability during the incident window, and pull affected user counts from application logs or analytics APIs
- **DraftDocumentWorker** (`pm_draft_document`): generate a Confluence or Google Docs post-mortem from a template, populating timeline, impact data, root cause analysis, and placeholder action items for the team to refine during review
- **ScheduleReviewWorker** (`pm_schedule_review`): create a Google Calendar or Outlook invite for the blameless review meeting, including all incident responders, with the draft post-mortem document linked in the invite description

Plug in PagerDuty and Google Calendar APIs; the post-mortem pipeline keeps the same document-assembly interface.

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
post-mortem-automation-post-mortem-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/postmortemautomation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectMetricsWorker.java
│       ├── DraftDocumentWorker.java
│       ├── GatherTimelineWorker.java
│       └── ScheduleReviewWorker.java
└── src/test/java/postmortemautomation/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
