# Legal Case Management in Java with Conductor

A Java Conductor workflow example demonstrating Legal Case Management. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

A new client matter arrives at the firm. You need to intake the case and assign it a tracking number, assess its complexity to determine staffing needs, assign the right attorney, track the case through phases like discovery and resolution, and eventually close it with a recorded outcome (e.g., settled). Without a structured workflow, cases fall through the cracks, deadlines get missed, and attorneys are assigned unevenly.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the case intake, party management, deadline tracking, and case resolution logic. Conductor handles deadline tracking, party management retries, and case lifecycle audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Case intake, party management, deadline tracking, and disposition workers handle case lifecycle through independent operational steps.

| Worker | Task | What It Does |
|---|---|---|
| **IntakeWorker** | `lcm_intake` | Registers a new case in the system and generates a case ID (e.g., CASE-691) for downstream tracking |
| **AssessWorker** | `lcm_assess` | Evaluates the case and assigns a complexity level (low/medium/high) to guide attorney assignment and resource allocation |
| **AssignWorker** | `lcm_assign` | Assigns a qualified attorney to the case, returning an attorney ID (e.g., ATT-25) based on complexity and availability |
| **TrackWorker** | `lcm_track` | Monitors the case through its lifecycle phases (e.g., discovery, trial, resolution) and reports the current phase |
| **CloseWorker** | `lcm_close` | Closes the case with a recorded outcome (e.g., "settled", "dismissed", "verdict") and finalizes all case records |

Workers simulate legal operations .  document review, compliance checks, approval routing ,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
lcm_intake
    │
    ▼
lcm_assess
    │
    ▼
lcm_assign
    │
    ▼
lcm_track
    │
    ▼
lcm_close
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
java -jar target/legal-case-management-1.0.0.jar
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
java -jar target/legal-case-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lcm_legal_case_management \
  --version 1 \
  --input '{"clientId": "TEST-001", "caseType": "test-value", "description": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lcm_legal_case_management -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real case systems .  your case management platform for matter intake, your calendar system for deadline tracking, your document store for filing management, and the workflow runs identically in production.

- **IntakeWorker** (`lcm_intake`): integrate with a legal case management system like Clio, Litify, or PracticePanther to create new matters and run conflict checks
- **AssessWorker** (`lcm_assess`): connect to your firm's matter scoring rules or an AI triage model to automatically evaluate case complexity and estimated value
- **AssignWorker** (`lcm_assign`): query attorney availability, expertise, and caseload from your HR/staffing system to make data-driven assignments
- **TrackWorker** (`lcm_track`): sync case phase updates to Clio, MyCase, or a custom dashboard, and trigger automated deadline calculations for each phase
- **CloseWorker** (`lcm_close`): finalize billing, archive documents to a DMS like NetDocuments or iManage, and update outcome records for reporting

Integrate a different docket system or notification service and the case pipeline persists unchanged.

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
legal-case-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/legalcasemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LegalCaseManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── AssignWorker.java
│       ├── CloseWorker.java
│       ├── IntakeWorker.java
│       └── TrackWorker.java
└── src/test/java/legalcasemanagement/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── AssignWorkerTest.java        # 2 tests
    ├── CloseWorkerTest.java        # 2 tests
    ├── IntakeWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 2 tests
```
