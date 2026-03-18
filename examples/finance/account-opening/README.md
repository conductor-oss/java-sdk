# Account Opening in Java with Conductor

A customer spent eight minutes filling out your online account application on their phone during their lunch break. They uploaded a photo of their driver's license, entered their SSN, picked a checking account with a $1,000 initial deposit -- then hit a screen that said "Please visit your nearest branch to complete identity verification." They didn't. They opened an account with a competitor that afternoon. Your conversion report shows 62% of applications abandoned at the identity verification step, but nobody connects that to the fact that the verification API, the credit check, and the core banking provisioning are three separate manual workflows stitched together by a case management queue that a human has to advance. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate account opening end-to-end -- collect documents, verify identity, run credit checks, provision the account, and send the welcome package -- as a single automated pipeline that completes in minutes, not days.

## The Problem

You need to open a new bank account for a customer. This requires collecting applicant information, verifying their identity against government databases, running a credit check to determine eligibility, provisioning the account in your core banking system with the initial deposit, and sending a welcome package with account details. Opening an account without identity verification exposes the bank to fraud; skipping the credit check violates regulatory requirements.

Without orchestration, you'd chain identity verification API calls, credit bureau queries, and core banking system writes in a single service -- manually handling timeouts from slow identity providers, retrying failed credit checks, and ensuring a partially opened account is never left in an inconsistent state.

## The Solution

**You just write the account opening workers -- document collection, identity verification, credit check, account provisioning, and welcome package. Conductor handles step ordering, automatic retries when the credit bureau API times out, and complete application lifecycle tracking.**

Each account-opening concern is a simple, independent worker -- a plain Java class that does one thing. Conductor takes care of executing them in order (collect info, verify identity, credit check, open account, send welcome), retrying if the credit bureau API times out, tracking every application's full lifecycle, and resuming from the last successful step if the process crashes mid-verification. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the onboarding pipeline: CollectInfoWorker gathers identity documents, VerifyIdentityWorker runs KYC checks, CreditCheckWorker queries ChexSystems, OpenAccountWorker provisions the account, and WelcomeWorker sends the welcome package.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **CollectInfoWorker** | `acc_collect_info` | Collects applicant documents (driver's license, SSN, proof of address) and records that all required identity documents are present | Simulated |
| **CreditCheckWorker** | `acc_credit_check` | Runs a credit and banking history check via ChexSystems | Simulated |
| **OpenAccountWorker** | `acc_open_account` | Provisions the new account in the core banking system -- generates an account number, assigns a routing number, and records the account type and initial deposit | Simulated |
| **VerifyIdentityWorker** | `acc_verify_identity` | Verifies the applicant's identity using KYC document checks | Simulated |
| **WelcomeWorker** | `acc_welcome` | Sends the welcome package to the new account holder -- includes debit card, checks, online banking enrollment, and mobile app setup | Simulated |

Workers simulate financial operations -- risk assessment, compliance checks, settlement -- with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
acc_collect_info
    │
    ▼
acc_verify_identity
    │
    ▼
acc_credit_check
    │
    ▼
acc_open_account
    │
    ▼
acc_welcome
```

## Example Output

```
=== Example 499: Account Opening ===

Step 1: Registering task definitions...
  Registered: acc_collect_info, acc_verify_identity, acc_credit_check, acc_open_account, acc_welcome

Step 2: Registering workflow 'account_opening_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: c9f0b76f-1a93-8809-7b70-ca1c2a2aa392

  [info] Collecting info for
  [credit] Running ChexSystems check for
  [open] Account 3 opened -- type: checking, deposit: $1000
  [identity] Verifying identity for
  [welcome] Welcome package sent to

  Status: COMPLETED
  Output: {applicationId=ACCTAPP-2024-001, accountNumber=3, accountType=checking, welcomeSent=true}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/account-opening-1.0.0.jar
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
java -jar target/account-opening-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow account_opening_workflow \
  --version 1 \
  --input '{"applicationId": "ACCTAPP-2024-001", "applicantName": "Emily Davis", "accountType": "checking", "initialDeposit": 1000}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w account_opening_workflow -s COMPLETED -c 5
```

## How to Extend

Connect VerifyIdentityWorker to your KYC provider, CreditCheckWorker to ChexSystems or a credit bureau, and OpenAccountWorker to your core banking system for account provisioning. The workflow definition stays exactly the same.

- **Identity verifier** -- call KYC providers (Jumio, Onfido, Plaid Identity) to verify government-issued IDs and perform liveness checks
- **Credit checker** -- pull credit reports from bureaus (Experian, Equifax, TransUnion) via their APIs
- **Account opener** -- provision the account in your core banking system (FIS, Fiserv, Temenos) with proper account type and initial deposit
- **Welcome sender** -- send welcome emails with account details via SendGrid/SES, mail physical cards, and set up online banking access

Swap in real identity verification, credit bureau, and core banking APIs while keeping the same output fields, and the account opening workflow continues without modification.

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
account-opening/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/accountopening/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AccountOpeningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectInfoWorker.java
│       ├── CreditCheckWorker.java
│       ├── OpenAccountWorker.java
│       ├── VerifyIdentityWorker.java
│       └── WelcomeWorker.java
└── src/test/java/accountopening/workers/
    ├── OpenAccountWorkerTest.java        # 2 tests
    └── WelcomeWorkerTest.java        # 2 tests
```
