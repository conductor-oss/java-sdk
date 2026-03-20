# Litigation Hold in Java with Conductor

A Java Conductor workflow example demonstrating Litigation Hold. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A potential lawsuit triggers a legal hold. You need to identify custodians who may possess relevant evidence, notify them of their preservation obligations, collect and preserve electronic data from email, Slack, and cloud storage, and track acknowledgments until the hold is released. Missing a custodian or losing data can result in sanctions and adverse inferences at trial.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the hold initiation, custodian notification, data preservation, and compliance verification logic. Conductor handles notification retries, acknowledgment tracking, and hold compliance audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Custodian identification, hold notification, acknowledgment tracking, and release workers each handle one phase of preserving relevant evidence.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `lth_identify` | Takes a case ID and custodian list, generates a hold ID (e.g., HOLD-{caseId}-001), and maps data sources (email, Slack, Drive) for each custodian |
| **NotifyWorker** | `lth_notify` | Sends legal hold notices to all identified custodians and returns the count of notifications sent (e.g., 5) |
| **CollectWorker** | `lth_collect` | Gathers electronically stored information. Documents (1,250), emails (8,400), and messages (3,200), and reports the total item count (12,850) |
| **PreserveWorker** | `lth_preserve` | Places collected data under preservation with a unique preservation ID (PRSV-{timestamp}) and confirms preservation status |
| **TrackWorker** | `lth_track` | Creates a tracking record (TRK-{timestamp}) for the hold and reports its current status (active) |

Workers simulate legal operations .  document review, compliance checks, approval routing ,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
lth_identify
    │
    ▼
lth_notify
    │
    ▼
lth_collect
    │
    ▼
lth_preserve
    │
    ▼
lth_track
```

## Example Output

```
=== Litigation Hold Demo ===

Step 1: Registering task definitions...
  Registered: lth_collect, lth_identify, lth_notify, lth_preserve, lth_track

Step 2: Registering workflow 'litigation_hold'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Collecting data for case
  [identify] Identifying custodians for case
  [notify] Sending hold notices for case
  [preserve] Preserving collected data
  [track] Tracking hold for case

  Status: COMPLETED
  Output: {collectedData=..., totalItems=..., holdId=..., identifiedCustodians=...}

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
java -jar target/litigation-hold-1.0.0.jar
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
java -jar target/litigation-hold-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lth_litigation_hold \
  --version 1 \
  --input '{"caseId": "CASE-2024-001", "CASE-2024-001": "custodians", "custodians": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lth_litigation_hold -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real legal hold tools .  your litigation hold platform for custodian notifications, your email and file systems for preservation, your compliance tracker for acknowledgment collection, and the workflow runs identically in production.

- **IdentifyWorker** (`lth_identify`): query your HR system (e.g., Workday, BambooHR) or Active Directory to resolve custodian identities and map their data sources automatically
- **NotifyWorker** (`lth_notify`): send hold notices via email (SendGrid, Microsoft Graph API) or a legal hold platform like Exterro or Zapproved, and track acknowledgment receipts
- **CollectWorker** (`lth_collect`): integrate with Google Vault, Microsoft Purview, or Slack eDiscovery APIs to collect ESI from actual custodian accounts
- **PreserveWorker** (`lth_preserve`): write preservation records to a legal hold database or a platform like Relativity, and apply in-place holds via Mobservability-pipeline compliance APIs
- **TrackWorker** (`lth_track`): push hold status to a case management system like Clio or Litify, and set up automated reminders for unacknowledged custodians

Change notification templates or acknowledgment tracking and the hold pipeline adjusts seamlessly.

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
litigation-hold/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/litigationhold/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LitigationHoldExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWorker.java
│       ├── IdentifyWorker.java
│       ├── NotifyWorker.java
│       ├── PreserveWorker.java
│       └── TrackWorker.java
└── src/test/java/litigationhold/workers/
    ├── CollectWorkerTest.java        # 2 tests
    ├── IdentifyWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    ├── PreserveWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests
```
