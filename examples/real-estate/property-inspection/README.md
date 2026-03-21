# Property Inspection in Java with Conductor :  Schedule, Inspect, Document, and Report

A Java Conductor workflow example for managing property inspections .  scheduling the inspection with an inspector, conducting the on-site evaluation (structural, plumbing, electrical, HVAC), documenting findings with photos and notes, and generating the final inspection report. Uses [Conductor](https://github.

## The Problem

You need to coordinate property inspections for real estate transactions. When a buyer makes an offer contingent on inspection, an inspector must be scheduled, the on-site inspection must cover all systems (roof, foundation, plumbing, electrical, HVAC), findings must be documented with photos and severity ratings, and a formal report must be generated for the buyer, seller, and their agents. Each step depends on the previous one .  you can't document findings before the inspection, and the report can't be generated without documentation.

Without orchestration, inspection coordination happens over phone calls and email. The agent schedules the inspector, the inspector emails findings in a Word doc, someone reformats it into a report, and it gets forwarded to the buyer. If the documentation step fails, the report is incomplete. If the inspector's notes are lost, the entire inspection must be repeated. Nobody can track which inspections are in progress, complete, or overdue.

## The Solution

**You just write the scheduling, on-site inspection, documentation, and report generation logic. Conductor handles scheduling retries, assessment sequencing, and inspection audit trails.**

Each inspection step is a simple, independent worker .  one schedules the inspector, one records the inspection findings, one organizes documentation (photos, notes, checklists), one generates the final report. Conductor takes care of executing them in order, retrying if the scheduling API is unavailable, and maintaining a permanent record of every inspection from scheduling through report delivery. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Scheduling, on-site assessment, deficiency documentation, and report generation workers each own one phase of property condition evaluation.

| Worker | Task | What It Does |
|---|---|---|
| **ScheduleWorker** | `pin_schedule` | Books the inspection appointment, coordinating inspector availability with property access |
| **InspectWorker** | `pin_inspect` | Conducts the on-site inspection, evaluating structural, plumbing, electrical, and HVAC systems |
| **DocumentWorker** | `pin_document` | Organizes inspection findings into structured documentation with photos, severity ratings, and notes |
| **ReportWorker** | `pin_report` | Generates the formal inspection report with overall condition assessment and recommended repairs |

Workers simulate property transaction steps .  listing, inspection, escrow, closing ,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
pin_schedule
    │
    ▼
pin_inspect
    │
    ▼
pin_document
    │
    ▼
pin_report

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
java -jar target/property-inspection-1.0.0.jar

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
java -jar target/property-inspection-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pin_property_inspection \
  --version 1 \
  --input '{"propertyId": "TEST-001", "inspectorId": "TEST-001", "inspectionType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pin_property_inspection -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real inspection tools .  your scheduling platform for inspector booking, a mobile inspection app for findings capture, your document system for report generation, and the workflow runs identically in production.

- **ScheduleWorker** (`pin_schedule`): integrate with Calendly or Google Calendar to book appointments, check inspector availability in your scheduling system
- **InspectWorker** (`pin_inspect`): accept inspection data from a mobile app (Spectora, HomeGauge) where the inspector records findings on-site
- **DocumentWorker** (`pin_document`): upload photos to S3 or Google Cloud Storage, attach them to the property record in your MLS/CRM
- **ReportWorker** (`pin_report`): generate PDF inspection reports using a template engine, distribute to buyer, seller, and agents via email or portal

Update inspection criteria or report formats and the inspection pipeline stays intact.

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
property-inspection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/propertyinspection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PropertyInspectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DocumentWorker.java
│       ├── InspectWorker.java
│       ├── ReportWorker.java
│       └── ScheduleWorker.java
└── src/test/java/propertyinspection/workers/
    ├── DocumentWorkerTest.java        # 2 tests
    ├── InspectWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    └── ScheduleWorkerTest.java        # 2 tests

```
