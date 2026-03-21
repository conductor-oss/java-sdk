# EHR Integration in Java Using Conductor :  Patient Record Query, Cross-System Merge, Validation, and Master Record Update

A Java Conductor workflow example for EHR integration. querying patient records from a source system, merging data from multiple EHR instances, validating the merged record for completeness and consistency, and updating the master patient record. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to integrate patient records across multiple EHR systems. When a patient has data in more than one system (e.g., hospital Epic, clinic Cerner, lab system), the records must be queried by patient ID, merged into a unified view that reconciles demographics, medications, allergies, and diagnoses, validated against data quality rules (missing fields, conflicting entries, duplicate records), and then written back to the master patient index. Each step depends on the previous one. you cannot merge without querying, and you cannot update the master record with unvalidated data. A bad merge that promotes incorrect allergy data or drops a medication can directly harm the patient.

Without orchestration, you'd build a monolithic ETL service that queries each source system, runs the merge logic, validates the output, and writes to the master record. If one EHR system is down during the query phase, you'd need to retry without re-querying systems that already responded. If the process crashes after merging but before validation, you'd have an unvalidated record in an intermediate state. HIPAA and Meaningful Use require a complete audit trail of every data exchange for compliance.

## The Solution

**You just write the EHR integration workers. Patient record query, cross-system merge, validation, and master record update. Conductor handles pipeline ordering, automatic retries when an EHR endpoint is temporarily down, and a healthcare-pattern audit trail of every data exchange.**

Each stage of the integration pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of querying before merging, validating before updating, retrying if an EHR endpoint is temporarily unavailable, and maintaining a healthcare-pattern audit trail of every data exchange. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the integration pipeline: QueryPatientWorker pulls records from the source EHR, MergeRecordsWorker reconciles data from multiple systems, ValidateRecordWorker checks completeness and consistency, and UpdateRecordWorker writes to the master patient index.

| Worker | Task | What It Does |
|---|---|---|
| **QueryPatientWorker** | `ehr_query_patient` | Queries patient records from the specified source EHR system by patient ID |
| **MergeRecordsWorker** | `ehr_merge_records` | Merges records from multiple sources, reconciling demographics, medications, allergies, and problem lists |
| **ValidateRecordWorker** | `ehr_validate` | Validates the merged record for completeness, consistency, and data quality (missing fields, duplicates, conflicts) |
| **UpdateRecordWorker** | `ehr_update` | Writes the validated, merged record to the master patient index |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
ehr_query_patient
    │
    ▼
ehr_merge_records
    │
    ▼
ehr_validate
    │
    ▼
ehr_update

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
java -jar target/ehr-integration-1.0.0.jar

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
java -jar target/ehr-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ehr_integration_workflow \
  --version 1 \
  --input '{"patientId": "TEST-001", "sourceSystem": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ehr_integration_workflow -s COMPLETED -c 5

```

## How to Extend

Connect QueryPatientWorker to your FHIR R4 endpoints across EHR systems, MergeRecordsWorker to your EMPI merge engine, and UpdateRecordWorker to your master patient index. The workflow definition stays exactly the same.

- **QueryPatientWorker** → call real FHIR R4 endpoints (Epic, Cerner, athenahealth) or process HL7v2 ADT messages for patient data retrieval
- **MergeRecordsWorker** → integrate with your EMPI (Enterprise Master Patient Index) or MDM platform for probabilistic matching and record linking
- **ValidateRecordWorker** → apply your organization's data governance rules, including NPI validation, address standardization, and duplicate detection
- **UpdateRecordWorker** → write to your MPI via FHIR Patient resource updates or HL7v2 ADT^A08 messages
- Add a **ConsentCheckWorker** before querying to verify the patient has consented to data sharing under 42 CFR Part 2 or state privacy laws

Point each worker at your real FHIR endpoints and EMPI while keeping the same output structure, and the integration pipeline runs without any workflow modifications.

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
ehr-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ehrintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EhrIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MergeRecordsWorker.java
│       ├── QueryPatientWorker.java
│       ├── UpdateRecordWorker.java
│       └── ValidateRecordWorker.java
└── src/test/java/ehrintegration/workers/
    ├── MergeRecordsWorkerTest.java        # 2 tests
    ├── QueryPatientWorkerTest.java        # 2 tests
    ├── UpdateRecordWorkerTest.java        # 2 tests
    └── ValidateRecordWorkerTest.java        # 2 tests

```
