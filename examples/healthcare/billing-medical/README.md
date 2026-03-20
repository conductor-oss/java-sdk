# Medical Billing in Java Using Conductor :  CPT/ICD Coding, Coverage Verification, Claim Submission, and Payment Tracking

A Java Conductor workflow example for medical billing .  coding clinical encounters with CPT and ICD codes, verifying patient insurance coverage, submitting claims to payers, and tracking reimbursement payments. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to bill for a clinical encounter. After a patient visit, the encounter must be coded with CPT procedure codes (e.g., 99213 for an office visit, 36415 for venipuncture) and ICD diagnosis codes (e.g., E11.9 for type 2 diabetes, I10 for hypertension). The patient's insurance coverage must be verified against the coded procedures. A claim is then submitted to the payer with the coded line items and total charge. Finally, the payment must be tracked through adjudication to reconcile what was billed versus what was reimbursed.

Without orchestration, you'd build a monolithic billing service that queries the encounter record, looks up codes, calls the eligibility API, formats and submits the 837 claim, and polls for the 835 remittance. If the payer's eligibility endpoint is down, you'd need retry logic. If the system crashes after coding but before claim submission, the encounter sits unbilled and revenue is delayed. Billing managers need a clear audit trail of every claim from coding through payment for denial management and compliance.

## The Solution

**You just write the billing workers. CPT/ICD coding, coverage verification, claim submission, and payment tracking. Conductor handles task ordering, automatic retries when the payer system is temporarily down, and a complete audit trail from encounter to payment.**

Each stage of the billing cycle is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of running coding before coverage verification, submitting claims only after coverage is confirmed, retrying if the payer's system is temporarily unavailable, and maintaining a complete audit trail from encounter to payment. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers cover the billing cycle: CodeProceduresWorker assigns CPT and ICD codes, VerifyCoverageWorker checks insurance eligibility, SubmitClaimWorker sends the 837 to the clearinghouse, and TrackPaymentWorker monitors adjudication.

| Worker | Task | What It Does |
|---|---|---|
| **CodeProceduresWorker** | `mbl_code_procedures` | Assigns CPT procedure codes and ICD diagnosis codes to the encounter, calculates total charge |
| **VerifyCoverageWorker** | `mbl_verify_coverage` | Checks the patient's insurance eligibility and benefit coverage for the coded procedures |
| **SubmitClaimWorker** | `mbl_submit_claim` | Formats and submits the claim (837P/837I) to the payer clearinghouse |
| **TrackPaymentWorker** | `mbl_track_payment` | Monitors the claim through adjudication and records the 835 remittance/payment |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
mbl_code_procedures
    │
    ▼
mbl_verify_coverage
    │
    ▼
mbl_submit_claim
    │
    ▼
mbl_track_payment
```

## Example Output

```
=== Medical Billing Workflow ===

Step 1: Registering task definitions...
  Registered: mbl_code_procedures, mbl_verify_coverage, mbl_submit_claim, mbl_track_payment

Step 2: Registering workflow 'medical_billing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [code] Coding procedures for encounter
  [submit] Submitting claim: $
  [track] Tracking payment for claim
  [coverage] Verifying coverage for

  Status: COMPLETED
  Output: {cptCodes=..., icdCodes=..., totalCharge=..., claimId=...}

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
java -jar target/billing-medical-1.0.0.jar
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
java -jar target/billing-medical-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow medical_billing_workflow \
  --version 1 \
  --input '{"encounterId": "ENC-2024-0301", "ENC-2024-0301": "patientId", "patientId": "PAT-10234", "PAT-10234": "providerId", "providerId": "PROV-5501", "PROV-5501": "procedures", "procedures": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w medical_billing_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CodeProceduresWorker to your coding engine, VerifyCoverageWorker to your 270/271 eligibility API, and SubmitClaimWorker to your clearinghouse (Availity, Change Healthcare). The workflow definition stays exactly the same.

- **CodeProceduresWorker** → integrate with an NLP-assisted coding engine or your EHR's charge capture module for automated CPT/ICD assignment
- **VerifyCoverageWorker** → call real 270/271 eligibility transactions through your clearinghouse or directly to payers
- **SubmitClaimWorker** → submit 837P/837I claims through Availity, Change Healthcare, or your clearinghouse API
- **TrackPaymentWorker** → process real 835 ERAs and auto-post payments to your practice management system
- Add a **DenialManagementWorker** with a SWITCH on claim status to automatically appeal denied claims with corrected codes
- Add a **PatientStatementWorker** to generate and mail patient responsibility statements for remaining balances

Replace the simulated outputs with real clearinghouse and payer connections while keeping the same field structure, and the billing pipeline runs without any workflow changes.

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
billing-medical/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/billingmedical/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BillingMedicalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CodeProceduresWorker.java
│       ├── SubmitClaimWorker.java
│       ├── TrackPaymentWorker.java
│       └── VerifyCoverageWorker.java
└── src/test/java/billingmedical/workers/
    ├── CodeProceduresWorkerTest.java        # 2 tests
    ├── SubmitClaimWorkerTest.java        # 2 tests
    ├── TrackPaymentWorkerTest.java        # 2 tests
    └── VerifyCoverageWorkerTest.java        # 2 tests
```
