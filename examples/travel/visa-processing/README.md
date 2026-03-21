# Visa Processing in Java with Conductor

Visa processing: collect docs, validate, submit, track, receive. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process a visa application for an employee traveling internationally. collecting required documents (passport, photos, invitation letter, financial statements), validating that all documents meet the destination country's requirements, submitting the application to the consulate or visa service, tracking the application's progress through processing phases, and receiving the approved visa and returning the passport to the employee. Each step depends on the previous one's completion.

If validation fails because a document is missing or expired, you need to notify the employee before submission to avoid consulate rejection. If the application is submitted but tracking stops polling, the employee misses their visa pickup window and the trip is cancelled. Without orchestration, you'd build a monolithic handler that mixes document management, consulate API integration, status polling, and employee notifications. making it impossible to support different visa types, test document validation rules independently, or audit which documents were submitted for which application.

## The Solution

**You just write the document collection, validation, application submission, status tracking, and visa receipt logic. Conductor handles document verification retries, submission sequencing, and visa application audit trails.**

CollectWorker gathers the required documents for the visa type and destination country. Passport scans, photos, invitation letters, and financial statements. ValidateWorker checks each document against the consulate's requirements (passport validity, photo dimensions, letter formatting). SubmitWorker sends the completed application package to the consulate or visa processing service. TrackWorker monitors the application status through processing phases (received, in review, decision pending). ReceiveWorker handles the approved visa receipt and passport return to the employee. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Document collection, eligibility verification, application submission, and status tracking workers each manage one phase of the visa application lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWorker** | `vsp_collect` | Collect. Computes and returns application id, documents |
| **ReceiveWorker** | `vsp_receive` | Visa approved and passport returned |
| **SubmitWorker** | `vsp_submit` | Submit. Computes and returns submitted, reference number |
| **TrackWorker** | `vsp_track` | Track. Computes and returns phase, estimated date |
| **ValidateWorker** | `vsp_validate` | All documents verified |

Workers implement travel operations. booking, approval, itinerary generation,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### The Workflow

```
vsp_collect
    │
    ▼
vsp_validate
    │
    ▼
vsp_submit
    │
    ▼
vsp_track
    │
    ▼
vsp_receive

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
java -jar target/visa-processing-1.0.0.jar

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
java -jar target/visa-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow vsp_visa_processing \
  --version 1 \
  --input '{"applicantId": "TEST-001", "country": 10, "visaType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w vsp_visa_processing -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real visa services. your document management system for collection, a visa service API like CIBT for submission, consulate tracking portals for status updates, and the workflow runs identically in production.

- **CollectWorker** (`vsp_collect`): pull document records from your document management system or prompt the employee to upload via your travel portal
- **ValidateWorker** (`vsp_validate`): check passport expiry (must be 6+ months), photo specs against ICAO standards, and completeness of supporting documents for the specific visa type
- **SubmitWorker** (`vsp_submit`): submit the application via a visa processing service API (CIBT, VFS Global) or directly to the consulate's electronic visa system
- **TrackWorker** (`vsp_track`): poll the visa service's status API for processing updates and estimated decision dates
- **ReceiveWorker** (`vsp_receive`): confirm visa issuance, update the employee's travel record, and coordinate passport return via courier or office pickup

Update document requirements or tracking systems and the visa pipeline handles them without modification.

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
visa-processing-visa-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/visaprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VisaProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWorker.java
│       ├── ReceiveWorker.java
│       ├── SubmitWorker.java
│       ├── TrackWorker.java
│       └── ValidateWorker.java
└── src/test/java/visaprocessing/workers/
    ├── CollectWorkerTest.java        # 2 tests
    ├── ReceiveWorkerTest.java        # 2 tests
    ├── SubmitWorkerTest.java        # 2 tests
    ├── TrackWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests

```
