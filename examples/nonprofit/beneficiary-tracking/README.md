# Beneficiary Tracking in Java with Conductor

A Java Conductor workflow example demonstrating Beneficiary Tracking. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A community health nonprofit enrolls a new beneficiary into a housing-assistance program. The intake team needs to register the person's demographics and location, assess their needs across housing, food security, education, and health, match them with appropriate services like food assistance, health screenings, and tutoring, monitor their progress over time, and produce a case report summarizing outcomes. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the beneficiary enrollment, service delivery, progress tracking, and outcome reporting logic. Conductor handles service delivery retries, progress monitoring, and beneficiary audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Registration, needs assessment, service provision, and monitoring workers each track one dimension of a beneficiary's journey through the program.

| Worker | Task | What It Does |
|---|---|---|
| **AssessNeedsWorker** | `btr_assess_needs` | Evaluates the beneficiary across housing, food security, education, and health dimensions, returning a needs map (e.g., housing: stable, food: insecure) |
| **MonitorWorker** | `btr_monitor` | Tracks ongoing progress across food security, health status, and academic outcomes for the enrolled beneficiary |
| **ProvideServicesWorker** | `btr_provide_services` | Enrolls the beneficiary in matched services (food assistance, health screening, tutoring) and records completed sessions |
| **RegisterWorker** | `btr_register` | Registers the beneficiary with their name and location, assigning a unique beneficiary ID |
| **ReportWorker** | `btr_report` | Generates a case report summarizing the beneficiary's services received and outcome status |

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
btr_register
    │
    ▼
btr_assess_needs
    │
    ▼
btr_provide_services
    │
    ▼
btr_monitor
    │
    ▼
btr_report
```

## Example Output

```
=== Example 758: Beneficiary Tracking ===

Step 1: Registering task definitions...
  Registered: btr_register, btr_assess_needs, btr_provide_services, btr_monitor, btr_report

Step 2: Registering workflow 'beneficiary_tracking_758'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [assess] Assessing needs for
  [monitor] Monitoring progress for
  [services] Providing services for
  [register] Registering beneficiary:
  [report] Generating report for

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
java -jar target/beneficiary-tracking-1.0.0.jar
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
java -jar target/beneficiary-tracking-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow beneficiary_tracking_758 \
  --version 1 \
  --input '{"beneficiaryName": "Aisha Johnson", "Aisha Johnson": "programId", "programId": "PGM-Education", "PGM-Education": "location", "location": "East District", "East District": "sample-East District"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w beneficiary_tracking_758 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real program systems .  your case management platform for intake, your outcomes database for progress tracking, your reporting tools for impact metrics, and the workflow runs identically in production.

- **RegisterWorker** (`btr_register`): create the beneficiary record in Salesforce NPSP or Bloomerang via their REST API, returning the CRM contact ID
- **AssessNeedsWorker** (`btr_assess_needs`): pull intake assessment data from your case management system (e.g., Apricot, Penelope) and map responses to standardized need categories
- **ProvideServicesWorker** (`btr_provide_services`): log service delivery in your program database and sync attendance records with Salesforce NPSP program engagement objects
- **MonitorWorker** (`btr_monitor`): query outcome tracking tables or pull progress data from your case management platform to evaluate beneficiary well-being over time
- **ReportWorker** (`btr_report`): generate the case report as a PDF using a reporting engine and upload it to your document management system or Salesforce Files

Swap your case management system or reporting tools and the tracking pipeline operates without structural changes.

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
beneficiary-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/beneficiarytracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BeneficiaryTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessNeedsWorker.java
│       ├── MonitorWorker.java
│       ├── ProvideServicesWorker.java
│       ├── RegisterWorker.java
│       └── ReportWorker.java
└── src/test/java/beneficiarytracking/workers/
    ├── RegisterWorkerTest.java        # 1 tests
    └── ReportWorkerTest.java        # 1 tests
```
