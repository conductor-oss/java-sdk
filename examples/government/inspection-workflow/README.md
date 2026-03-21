# Inspection Workflow in Java with Conductor

Conducts a government property inspection: scheduling the visit, performing the on-site assessment, documenting findings, and recording pass or fail via a SWITCH task with code violations cited. Uses [Conductor](https://github.

## The Problem

You need to conduct a government property inspection (building code, fire safety, health, environmental). The inspection is scheduled, the inspector visits the property and conducts the assessment, findings are documented with photos and notes, and the result is recorded as pass or fail with specific code violations cited. Passing a property without thorough inspection creates safety risks; failing without documented evidence invites legal challenges.

Without orchestration, you'd manage inspections with paper checklists, phone-scheduled appointments, handwritten notes, and manual data entry back at the office .  losing inspector notes between field and office, missing scheduled inspections, and struggling to produce evidence when a building owner contests a violation.

## The Solution

**You just write the scheduling, on-site assessment, findings documentation, and pass/fail determination logic. Conductor handles scheduling retries, findings routing, and inspection audit trails.**

Each inspection concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (schedule, inspect, document, record pass/fail), routing via a SWITCH task based on the inspection result, tracking every inspection with timestamped evidence, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Scheduling, on-site inspection, findings recording, and compliance determination workers each own one step of the regulatory inspection process.

| Worker | Task | What It Does |
|---|---|---|
| **DocumentWorker** | `inw_document` | Documents inspection findings (structural, electrical, plumbing) with pass/fail determinations |
| **InspectWorker** | `inw_inspect` | Performs the on-site property inspection and records findings for structural, electrical, and plumbing systems |
| **RecordFailWorker** | `inw_record_fail` | Records a failed inspection result with specific code violations and re-inspection deadline |
| **RecordPassWorker** | `inw_record_pass` | Records a passed inspection result and issues a compliance certificate for the property |
| **ScheduleWorker** | `inw_schedule` | Schedules the inspection for the property, assigning a date and inspector |

Workers simulate government operations .  application processing, compliance checks, notifications ,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
inw_schedule
    │
    ▼
inw_inspect
    │
    ▼
inw_document
    │
    ▼
SWITCH (inw_switch_ref)
    ├── pass: inw_record_pass
    ├── fail: inw_record_fail

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
java -jar target/inspection-workflow-1.0.0.jar

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
java -jar target/inspection-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow inw_inspection_workflow \
  --version 1 \
  --input '{"propertyId": "TEST-001", "inspectionType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w inw_inspection_workflow -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real inspection systems .  your scheduling platform for inspector dispatch, your mobile inspection app for on-site assessment, your code enforcement database for violation tracking, and the workflow runs identically in production.

- **Scheduler**: integrate with your inspection management system to schedule visits based on inspector availability and geographic routing
- **Inspector**: provide mobile inspection checklists (iAuditor, GoCanvas) with offline capability for field use
- **Documenter**: capture photos, GPS coordinates, and inspector notes with timestamp; attach to the inspection record
- **Pass recorder**: issue compliance certificates and update the property's inspection history in your permitting system
- **Fail recorder**: generate violation notices with specific code citations, required corrections, and re-inspection deadlines

Modify inspection checklists or compliance criteria and the pipeline structure stays the same.

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
inspection-workflow-inspection-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/inspectionworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InspectionWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DocumentWorker.java
│       ├── InspectWorker.java
│       ├── RecordFailWorker.java
│       ├── RecordPassWorker.java
│       └── ScheduleWorker.java
└── src/test/java/inspectionworkflow/workers/
    ├── DocumentWorkerTest.java
    ├── InspectWorkerTest.java
    ├── RecordFailWorkerTest.java
    ├── RecordPassWorkerTest.java
    └── ScheduleWorkerTest.java

```
