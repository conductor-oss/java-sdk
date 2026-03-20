# Loan Origination in Java with Conductor

Loan origination: application intake, credit check, underwriting, approval, and funding. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to originate a loan from application to funding. An applicant submits a loan application, a credit check evaluates their creditworthiness, underwriting assesses the loan's risk and terms, an approval decision is made, and funds are disbursed. Each step depends on the previous .  you cannot underwrite without a credit report, and you cannot fund without approval. Funding a loan without proper credit assessment creates bad debt exposure.

Without orchestration, you'd build a monolithic loan pipeline that collects applications, pulls credit reports, runs underwriting models, updates loan status, and triggers funding .  manually handling the weeks-long lifecycle, retrying failed credit bureau calls, and tracking every step for TILA/RESPA compliance.

## The Solution

**You just write the loan workers. Application intake, credit check, underwriting, approval, and fund disbursement. Conductor handles pipeline ordering, automatic retries when the credit bureau API times out, and full application lifecycle tracking for TILA/RESPA compliance.**

Each origination concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (intake, credit check, underwriting, approval, funding), retrying if the credit bureau API times out, tracking every application's full journey for regulatory compliance, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the origination pipeline: ApplicationWorker intakes the application, CreditCheckWorker pulls the credit report, UnderwriteWorker assesses risk and terms, ApproveWorker records the decision, and FundWorker disburses the loan.

| Worker | Task | What It Does |
|---|---|---|
| **ApplicationWorker** | `lnr_application` | Receives a loan application and records intake details. |
| **ApproveWorker** | `lnr_approve` | Approves the loan after underwriting. |
| **CreditCheckWorker** | `lnr_credit_check` | Runs a credit check for the loan applicant. |
| **FundWorker** | `lnr_fund` | Disburses the loan funds to the applicant. |
| **UnderwriteWorker** | `lnr_underwrite` | Underwrites the loan based on credit score and DTI ratio. |

Workers simulate financial operations .  risk assessment, compliance checks, settlement ,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
lnr_application
    │
    ▼
lnr_credit_check
    │
    ▼
lnr_underwrite
    │
    ▼
lnr_approve
    │
    ▼
lnr_fund
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
java -jar target/loan-origination-1.0.0.jar
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
java -jar target/loan-origination-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow loan_origination_workflow \
  --version 1 \
  --input '{"applicationId": "TEST-001", "applicantId": "TEST-001", "loanAmount": 100, "loanType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w loan_origination_workflow -s COMPLETED -c 5
```

## How to Extend

Connect ApplicationWorker to your loan application portal, CreditCheckWorker to a credit bureau API, and UnderwriteWorker to your automated underwriting system for risk evaluation. The workflow definition stays exactly the same.

- **Application intake**: persist applications to your loan origination system (Encompass, Blend, custom LOS) and trigger document collection
- **Credit checker**: pull credit reports and scores from the three bureaus via Equifax/Experian/TransUnion APIs
- **Underwriter**: run automated underwriting (DU, LP) or custom risk models; apply lending policy rules for LTV, DTI, and credit score thresholds
- **Approval handler**: generate loan estimates and closing disclosures per TILA/RESPA; route edge cases to human underwriters via WAIT tasks
- **Funding processor**: disburse funds via ACH/wire, record the lien, and set up the loan servicing record

Swap in real credit bureau, underwriting model, and core banking integrations while keeping the same output fields, and the origination pipeline runs without any workflow changes.

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
loan-origination/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/loanorigination/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LoanOriginationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplicationWorker.java
│       ├── ApproveWorker.java
│       ├── CreditCheckWorker.java
│       ├── FundWorker.java
│       └── UnderwriteWorker.java
└── src/test/java/loanorigination/workers/
    ├── ApplicationWorkerTest.java        # 8 tests
    ├── ApproveWorkerTest.java        # 8 tests
    ├── CreditCheckWorkerTest.java        # 8 tests
    ├── FundWorkerTest.java        # 8 tests
    └── UnderwriteWorkerTest.java        # 11 tests
```
