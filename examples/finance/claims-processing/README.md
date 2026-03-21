# Claims Processing in Java with Conductor

A policyholder rear-ends someone at a stoplight and files a claim that afternoon. Eight days pass before an adjuster is assigned. the intake system routed it to a queue that nobody monitors on weekends. The adjuster schedules an inspection, but the body shop estimate doesn't come back for another three weeks because the request sat in the shop's email. Six weeks after a fender bender, the customer still doesn't have a repair estimate, and they switch carriers. The claim itself was straightforward, the process just had no urgency, no tracking, and no escalation when steps stalled. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full claims lifecycle, submission, verification, damage assessment, settlement, and closure, as independent workers.

## The Problem

You need to process insurance claims from submission to settlement. A policyholder submits a claim with description and estimated amount, the claims team verifies policy coverage and claim details, a damage assessment determines the actual loss, the settlement amount is calculated based on coverage limits and deductibles, and the claim is closed. Settling without proper verification exposes the insurer to fraudulent claims; inadequate damage assessment leads to underpayment disputes.

Without orchestration, you'd build a monolithic claims handler that validates policies, contacts adjusters, calculates settlements, and updates the claims database. Manually coordinating between adjusters in the field, retrying failed policy lookups, and maintaining claim state across what can be a weeks-long process.

## The Solution

**You just write the claims workers. Submission intake, policy verification, damage assessment, settlement calculation, and claim closure. Conductor handles sequential step execution, automatic retries when the policy system is unavailable, and full claims lifecycle tracking for regulatory compliance.**

Each claims concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (submit, verify, assess, settle, close), retrying if the policy system is unavailable, tracking every claim's full lifecycle for regulatory compliance, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle the claims lifecycle: SubmitClaimWorker intakes the claim, VerifyDetailsWorker checks policy coverage, AssessDamageWorker determines actual loss, SettleAmountWorker calculates the payout, and CloseClaimWorker finalizes the record.

| Worker | Task | What It Does |
|---|---|---|
| **AssessDamageWorker** | `clp_assess_damage` | Assesses damage for a claim and computes assessed amount (85% of requested). |
| **CloseClaimWorker** | `clp_close_claim` | Closes a claim after settlement. |
| **SettleAmountWorker** | `clp_settle_amount` | Settles claim amount (assessed minus $500 deductible). |
| **SubmitClaimWorker** | `clp_submit_claim` | Submits an insurance claim and returns policy metadata. |
| **VerifyDetailsWorker** | `clp_verify_details` | Verifies policy details for a claim. |

Workers implement financial operations: risk assessment, compliance checks, settlement, with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
clp_submit_claim
    │
    ▼
clp_verify_details
    │
    ▼
clp_assess_damage
    │
    ▼
clp_settle_amount
    │
    ▼
clp_close_claim

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
java -jar target/claims-processing-1.0.0.jar

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
java -jar target/claims-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow claims_processing_workflow \
  --version 1 \
  --input '{"claimId": "CLM-8810", "policyId": "POL-3322", "claimType": "auto_collision", "description": "Rear-end collision at intersection", "amount": 12000}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w claims_processing_workflow -s COMPLETED -c 5

```

## How to Extend

Connect SubmitClaimWorker to your claims intake portal, VerifyDetailsWorker to your policy administration system, and AssessDamageWorker to your adjuster tools and estimation platform. The workflow definition stays exactly the same.

- **Claim submitter**: persist claims to your claims management system (Guidewire, Duck Creek, Majesco) with document attachments
- **Verifier**: validate policy coverage and status against your policy administration system; check for prior claims and fraud indicators
- **Damage assessor**: integrate with adjuster dispatch systems, receive field reports, or use AI-based damage estimation from photos
- **Settlement calculator**: compute settlement based on coverage limits, deductibles, depreciation, and applicable state regulations
- **Claim closer**: issue payment via ACH/check, update the claims database, and generate closure documents

Point each worker at your real policy administration and adjuster management systems while keeping the same output structure, and the claims pipeline requires zero workflow changes.

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
claims-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/claimsprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ClaimsProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessDamageWorker.java
│       ├── CloseClaimWorker.java
│       ├── SettleAmountWorker.java
│       ├── SubmitClaimWorker.java
│       └── VerifyDetailsWorker.java
└── src/test/java/claimsprocessing/workers/
    ├── AssessDamageWorkerTest.java        # 8 tests
    ├── CloseClaimWorkerTest.java        # 8 tests
    ├── SettleAmountWorkerTest.java        # 8 tests
    ├── SubmitClaimWorkerTest.java        # 8 tests
    └── VerifyDetailsWorkerTest.java        # 8 tests

```
