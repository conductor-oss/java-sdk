# Health Insurance Claims in Java Using Conductor :  Submission, Verification, Adjudication, Payment, and Closure

A Java Conductor workflow example for health insurance claims processing .  submitting claims with procedure codes and amounts, verifying member eligibility and provider credentials, adjudicating the claim against policy rules, issuing payment to the provider, and closing the claim with a final status. Uses [Conductor](https://github.

## The Problem

You need to process health insurance claims from submission through payment. A provider submits a claim with the patient ID, procedure code, and billed amount. The claim must be verified .  confirming the member's active coverage, the provider's network status, and that the procedure is a covered benefit. The verified claim is then adjudicated against the policy's benefit rules, applying deductibles, copays, coinsurance, and out-of-pocket maximums to determine the allowed amount. Payment is issued to the provider for the approved amount. Finally, the claim is closed with an explanation of benefits (EOB). Each step depends on the previous one ,  you cannot adjudicate without verifying eligibility, and you cannot pay without adjudication.

Without orchestration, you'd build a monolithic claims engine that receives the 837 claim, checks eligibility, runs the adjudication rules, triggers the payment, and generates the EOB. If the eligibility check system is temporarily unavailable, you'd need retry logic. If the process crashes after adjudication but before payment, the provider is not paid and the claim is stuck. State insurance regulators require a complete audit trail of every claim decision for compliance reviews and prompt-pay law enforcement.

## The Solution

**You just write the claims workers. Submission intake, eligibility verification, adjudication, payment, and EOB closure. Conductor handles strict step ordering, automatic retries when the eligibility system is temporarily unavailable, and a complete regulatory audit trail from submission to closure.**

Each stage of claims processing is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of verifying before adjudicating, paying only after adjudication approves the claim, retrying if the eligibility system is temporarily unavailable, and maintaining a complete regulatory audit trail from submission to closure. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the claims lifecycle: SubmitClaimWorker intakes the claim, VerifyClaimWorker checks eligibility, AdjudicateClaimWorker applies benefit rules, PayClaimWorker issues provider payment, and CloseClaimWorker generates the EOB.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitClaimWorker** | `clm_submit` | Receives and registers the incoming claim with procedure code, patient, provider, and billed amount |
| **VerifyClaimWorker** | `clm_verify` | Verifies member eligibility, provider network status, and benefit coverage for the submitted procedure |
| **AdjudicateClaimWorker** | `clm_adjudicate` | Applies benefit rules .  deductibles, copays, coinsurance, policy limits ,  to determine the allowed payment amount |
| **PayClaimWorker** | `clm_pay` | Issues payment to the provider via EFT/check and generates the remittance advice (835) |
| **CloseClaimWorker** | `clm_close` | Closes the claim, generates the Explanation of Benefits (EOB), and archives the record |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### The Workflow

```
clm_submit
    │
    ▼
clm_verify
    │
    ▼
clm_adjudicate
    │
    ▼
clm_pay
    │
    ▼
clm_close

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
java -jar target/insurance-claims-1.0.0.jar

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
java -jar target/insurance-claims-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow insurance_claims_workflow \
  --version 1 \
  --input '{"claimId": "TEST-001", "patientId": "TEST-001", "providerId": "TEST-001", "amount": 100, "procedureCode": "sample-procedureCode"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w insurance_claims_workflow -s COMPLETED -c 5

```

## How to Extend

Connect SubmitClaimWorker to your 837 transaction processor, VerifyClaimWorker to your eligibility system, and AdjudicateClaimWorker to your claims adjudication rules engine. The workflow definition stays exactly the same.

- **SubmitClaimWorker** → parse real 837P/837I EDI transactions or integrate with your claims intake portal
- **VerifyClaimWorker** → call real 270/271 eligibility checks and query your provider credentialing database
- **AdjudicateClaimWorker** → integrate with your claims adjudication engine with full benefit plan rules, COB, and medical policy
- **PayClaimWorker** → trigger real EFT payments through your treasury system and generate 835 remittance advice
- **CloseClaimWorker** → generate member EOBs and archive claims to your data warehouse for actuarial analysis
- Add a **FraudDetectionWorker** before adjudication to score claims against fraud models and flag suspicious patterns

Connect each worker to your real claims adjudication engine, eligibility system, and payment platform while preserving the same output fields, and the claims pipeline requires zero workflow changes.

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
insurance-claims/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/insuranceclaims/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InsuranceClaimsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AdjudicateClaimWorker.java
│       ├── CloseClaimWorker.java
│       ├── PayClaimWorker.java
│       ├── SubmitClaimWorker.java
│       └── VerifyClaimWorker.java
└── src/test/java/insuranceclaims/workers/
    ├── AdjudicateClaimWorkerTest.java        # 2 tests
    └── CloseClaimWorkerTest.java        # 2 tests

```
