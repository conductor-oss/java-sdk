# Medical Records Review in Java Using Conductor :  HIPAA Compliance Validation, Physician Review via WAIT, and Audit-Trailed Storage

A Java Conductor workflow example for medical records review .  automatically validating HIPAA compliance (PHI encryption, audit logging, access controls), pausing at a WAIT task for a physician to review the records and provide their clinical assessment, and storing the result with a complete audit trail. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Medical Records Must Pass HIPAA Compliance Before Physician Review

Before a physician can review medical records, the system must validate HIPAA compliance. Checking PHI encryption, audit logging, access controls, and data retention policies. The workflow runs automated HIPAA checks, pauses at a WAIT task for physician review, then stores the result with an audit trail. If storing the result fails, you need to retry it without asking the physician to re-review.

## The Solution

**You just write the HIPAA validation and audit-trailed storage workers. Conductor handles the durable pause for physician review and the compliance pipeline.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

MrValidateHipaaWorker checks PHI encryption and access controls, and MrStoreResultWorker records the physician's assessment with an audit trail, the clinical review pause between them is HIPAA-compliant and durable.

| Worker | Task | What It Does |
|---|---|---|
| **MrValidateHipaaWorker** | `mr_validate_hipaa` | Runs automated HIPAA compliance checks .  verifies PHI encryption, audit logging configuration, access controls, and data retention policies |
| *WAIT task* | `mr_physician_review` | Pauses until a physician reviews the records and submits their clinical assessment via `POST /tasks/{taskId}` | Built-in Conductor WAIT .  no worker needed |
| **MrStoreResultWorker** | `mr_store_result` | Stores the physician review result with a complete HIPAA-compliant audit trail including reviewer identity, timestamp, and access log |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
mr_validate_hipaa
    │
    ▼
physician_review [WAIT]
    │
    ▼
mr_store_result
```

## Example Output

```
=== Medical Records Review Demo: HIPAA Validation + Physician Review ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'medical_records_review_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [mr_store_result] Storing physician review result...
  [mr_validate_hipaa] Validating HIPAA compliance...

  Status: COMPLETED
  Output: {stored=..., auditTrailId=..., compliant=..., checks=...}

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
java -jar target/medical-records-review-1.0.0.jar
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
java -jar target/medical-records-review-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow medical_records_review_demo \
  --version 1 \
  --input '{"recordId": "REC-12345", "REC-12345": "sample-REC-12345"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w medical_records_review_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one stage of the compliance flow .  connect your HIPAA compliance engine for validation and your EHR system (Epic, Cerner) for audit-trailed storage, and the medical review workflow stays the same.

- **MrStoreResultWorker** (`mr_store_result`): write the review result to a HIPAA-compliant data store like AWS HealthLake, update the patient record in your EHR system, and generate a compliance audit report
- **MrValidateHipaaWorker** (`mr_validate_hipaa`): integrate with a compliance platform like Vanta or Drata to run real HIPAA control checks, or query your security infrastructure for encryption and access log status

Integrate your HIPAA compliance platform and EHR system and the physician review pipeline with audit trail remains intact.

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
medical-records-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/medicalrecordsreview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MedicalRecordsReviewExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MrStoreResultWorker.java
│       └── MrValidateHipaaWorker.java
└── src/test/java/medicalrecordsreview/workers/
    ├── MrStoreResultWorkerTest.java        # 6 tests
    └── MrValidateHipaaWorkerTest.java        # 6 tests
```
