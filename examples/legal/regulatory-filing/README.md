# Regulatory Filing in Java with Conductor

A Java Conductor workflow example demonstrating Regulatory Filing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

A regulatory filing deadline is approaching. You need to prepare the filing package with required disclosures and attachments for the specific entity and jurisdiction, validate that the package meets all regulatory requirements (no missing fields or documents), submit it to the regulatory body, track the submission through processing (typically 15 days), and confirm receipt. A missed or defective filing can trigger penalties, enforcement actions, or loss of business licenses.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the filing preparation, data validation, regulatory submission, and confirmation tracking logic. Conductor handles validation retries, submission sequencing, and filing audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Data compilation, form population, validation, and submission workers each handle one step of preparing and submitting regulatory documents.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `rgf_prepare` | Assembles the filing package for the specified entity and filing type, generating a filing ID (FIL-{timestamp}) and bundling 5 required documents |
| **ValidateWorker** | `rgf_validate` | Checks the filing package for completeness and correctness, returning a validation status and an empty error list when all requirements are met |
| **SubmitWorker** | `rgf_submit` | Submits the validated filing to the regulatory body, generating a submission ID (SUB-{timestamp}) and recording the submission timestamp |
| **TrackWorker** | `rgf_track` | Monitors the submission status (e.g., "received") and reports the estimated processing time (15 days) |
| **ConfirmWorker** | `rgf_confirm` | Confirms the filing was accepted by the regulatory body, generating a confirmation number (CNF-{timestamp}) and a confirmed flag |

Workers simulate legal operations .  document review, compliance checks, approval routing ,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
rgf_prepare
    │
    ▼
rgf_validate
    │
    ▼
rgf_submit
    │
    ▼
rgf_track
    │
    ▼
rgf_confirm
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
java -jar target/regulatory-filing-1.0.0.jar
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
java -jar target/regulatory-filing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rgf_regulatory_filing \
  --version 1 \
  --input '{"filingType": "test-value", "entityName": "test", "jurisdiction": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rgf_regulatory_filing -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real regulatory systems .  your compliance platform for requirement tracking, your document generator for filing preparation, the regulator's submission portal for filing, and the workflow runs identically in production.

- **PrepareWorker** (`rgf_prepare`): pull entity data and required disclosures from your ERP or legal entity management system (e.g., Diligent Entities, CT Corporation) and auto-populate filing templates
- **ValidateWorker** (`rgf_validate`): integrate with regulatory body validation APIs (e.g., SEC EDGAR validation, state SOS filing rules) to catch errors before submission
- **SubmitWorker** (`rgf_submit`): submit electronically via EDGAR, XBRL filing APIs, state Secretary of State portals, or regulatory platforms like Lex Mundi or CSC Global
- **TrackWorker** (`rgf_track`): poll regulatory body status APIs or use entity management platforms to monitor filing progress and expected processing timelines
- **ConfirmWorker** (`rgf_confirm`): verify acceptance via confirmation APIs or email parsing, and update your compliance calendar and entity management records

Update form templates or validation rules and the filing pipeline handles them with no restructuring.

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
regulatory-filing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/regulatoryfiling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RegulatoryFilingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmWorker.java
│       ├── PrepareWorker.java
│       ├── SubmitWorker.java
│       ├── TrackWorker.java
│       └── ValidateWorker.java
└── src/test/java/regulatoryfiling/workers/
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── PrepareWorkerTest.java        # 2 tests
    ├── SubmitWorkerTest.java        # 2 tests
    ├── TrackWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests
```
