# Implementing GDPR Compliance in Java with Conductor: Identity Verification, Data Location, Request Processing, and Completion Confirmation

A customer emailed asking you to delete all their data. Legal forwarded it to engineering, who found records in the CRM. The billing team deleted their payment history. Analytics still had their event stream but didn't hear about the request until week three. The marketing team's third-party enrichment vendor? Nobody even thought to check. Day 29: the customer filed a complaint with the Data Protection Authority. You have no audit trail proving what was deleted, what was missed, or when each system was actually processed, and now you're explaining to regulators why three of seven data stores were never touched. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate GDPR data subject requests end-to-end: verify identity, locate data across every system, process the request, and confirm completion, with a regulator-ready audit trail proving every step.

## The Problem

Under GDPR, data subjects can request access to their data, erasure ("right to be forgotten"), or portability. You have 30 days to comply. Each request requires verifying the requester's identity (prevent unauthorized data access), locating their data across all systems (databases, backups, SaaS tools, logs), processing the specific request type, and confirming completion back to the requester.

Without orchestration, GDPR requests are tracked in a spreadsheet. Someone manually searches each database, emails each SaaS vendor, and hopes they found everything before the 30-day deadline. If they miss a system, the organization faces regulatory fines. There's no audit trail proving the request was fully processed.

## The Solution

**You just write the data location queries and erasure operations. Conductor handles the mandated sequence from identity verification through completion, retries when data systems are unavailable, and a regulators-ready audit trail proving every step of the request was fulfilled.**

Each GDPR step is an independent worker: identity verification, data location, request processing, and completion confirmation. Conductor runs them in sequence: verify identity, locate all personal data, process the request, then confirm. Every request is tracked with full audit trail, when it was received, what data was found, what action was taken, and when it was completed, proving compliance to regulators. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle GDPR requests end-to-end: VerifyIdentityWorker confirms the requester is who they claim, LocateDataWorker finds their personal data across all systems, ProcessRequestWorker executes the erasure or export, and ConfirmCompletionWorker notifies the subject within the 30-day deadline.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmCompletionWorker** | `gdpr_confirm_completion` | Sends a completion confirmation to the data subject within the 30-day deadline |
| **LocateDataWorker** | `gdpr_locate_data` | Locates the subject's personal data across all systems (e.g., CRM, billing, analytics, logs) |
| **ProcessRequestWorker** | `gdpr_process_request` | Executes the requested right (erasure, export, rectification) across all identified systems |
| **VerifyIdentityWorker** | `gdpr_verify_identity` | Verifies the identity of the data subject making the GDPR request |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations, the workflow logic stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
gdpr_verify_identity
    │
    ▼
gdpr_locate_data
    │
    ▼
gdpr_process_request
    │
    ▼
gdpr_confirm_completion
```

## Example Output

```
=== Example 357: GDPR Compliance ===

Step 1: Registering task definitions...
  Registered: gdpr_verify_identity, gdpr_locate_data, gdpr_process_request, gdpr_confirm_completion

Step 2: Registering workflow 'gdpr_compliance_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: bd3bdc2b-9822-ffa6-7be8-21a4f8b067c5

  [confirm] Confirmation sent to data subject within 30-day deadline
  [locate] Data found in 4 systems: CRM, billing, analytics, logs
  [process] right-to-erasure: data exported/deleted from 4 systems
  [identity] Subject EU-USER-12345: identity verified

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
java -jar target/gdpr-compliance-1.0.0.jar
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

```bash
conductor workflow start \
  --workflow gdpr_compliance_workflow \
  --version 1 \
  --input '{"subjectId": "EU-USER-12345", "requestType": "right-to-erasure"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gdpr_compliance_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one GDPR step. Connect LocateDataWorker to query your CRM, billing, and analytics databases, ProcessRequestWorker to execute erasure or export operations, and the verify-locate-process-confirm workflow stays the same.

- **ConfirmCompletionWorker** (`gdpr_confirm_completion`): send completion notice to the data subject, record the processing in your GDPR compliance log
- **LocateDataWorker** (`gdpr_locate_data`): search for personal data across databases, SaaS applications (via SCIM/API), backups, and log stores
- **ProcessRequestWorker** (`gdpr_process_request`): execute the request. Export data for access/portability requests, delete/anonymize for erasure requests

Point the data location and processing workers at your real databases and SaaS apps, and the GDPR compliance flow operates without workflow changes.

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
gdpr-compliance-gdpr-compliance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gdprcompliance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point
│   └── workers/
│       ├── ConfirmCompletionWorker.java
│       ├── LocateDataWorker.java
│       ├── ProcessRequestWorker.java
│       └── VerifyIdentityWorker.java
└── src/test/java/gdprcompliance/
    └── MainExampleTest.java        # 2 tests. Workflow resource loading, worker instantiation
```
