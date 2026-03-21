# KYC AML in Java with Conductor

KYC/AML workflow that verifies customer identity, screens against watchlists, assesses risk, and makes a compliance decision. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to verify a customer's identity and screen them against anti-money-laundering watchlists before onboarding. The workflow verifies the customer's identity documents, screens their name against sanctions lists (OFAC, EU, UN), PEP lists, and adverse media, assesses the overall risk level, and makes a compliance decision (approve, enhanced due diligence, or reject). Onboarding a sanctioned individual exposes the institution to massive fines and criminal liability.

Without orchestration, you'd build a single compliance service that calls identity verification APIs, queries watchlist databases, runs risk scoring, and records decisions. manually handling conflicting results from different watchlist providers, retrying failed API calls, and maintaining an audit trail that regulators can inspect.

## The Solution

**You just write the compliance workers. Identity verification, watchlist screening, risk assessment, and approval/rejection decision. Conductor handles step sequencing, automatic retries when a watchlist provider is unavailable, and a tamper-evident compliance audit trail.**

Each KYC/AML concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (verify identity, screen watchlists, assess risk, make decision), retrying if a watchlist provider is unavailable, maintaining a complete compliance audit trail, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the compliance pipeline: VerifyIdentityWorker checks identity documents, ScreenWatchlistsWorker queries OFAC, PEP, and adverse media lists, AssessRiskWorker computes the overall risk level, and DecideWorker makes the approve, EDD, or reject determination.

| Worker | Task | What It Does |
|---|---|---|
| **AssessRiskWorker** | `kyc_assess_risk` | Assesses overall KYC risk based on identity verification and watchlist results. |
| **DecideWorker** | `kyc_decide` | Makes a compliance decision based on the assessed risk level. |
| **ScreenWatchlistsWorker** | `kyc_screen_watchlists` | Screens customer against OFAC, PEP, and adverse media watchlists. |
| **VerifyIdentityWorker** | `kyc_verify_identity` | Verifies customer identity using the provided document type. |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
kyc_verify_identity
    │
    ▼
kyc_screen_watchlists
    │
    ▼
kyc_assess_risk
    │
    ▼
kyc_decide

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
java -jar target/kyc-aml-1.0.0.jar

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
java -jar target/kyc-aml-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow kyc_aml_workflow \
  --version 1 \
  --input '{"customerId": "TEST-001", "name": "test", "nationality": "sample-nationality", "documentType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w kyc_aml_workflow -s COMPLETED -c 5

```

## How to Extend

Connect VerifyIdentityWorker to your KYC document verification provider, ScreenWatchlistsWorker to OFAC and PEP screening services, and AssessRiskWorker to your AML risk scoring model. The workflow definition stays exactly the same.

- **Identity verifier**: call KYC providers (Jumio, Onfido, Socure) for document verification and liveness detection
- **Watchlist screener**: screen against OFAC SDN, EU sanctions, UN consolidated list, PEP databases, and adverse media using providers like Dow Jones, World-Check, or ComplyAdvantage
- **Risk assessor**: compute a composite risk score based on country risk, PEP status, transaction patterns, and customer profile using your risk model
- **Decision maker**: apply regulatory thresholds to approve, require enhanced due diligence (EDD), or reject; generate SAR filings for suspicious cases

Point each worker at your real identity verification provider and watchlist screening service while maintaining the same output contract, and the KYC/AML workflow runs without modification.

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
kyc-aml/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/kycaml/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── KycAmlExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessRiskWorker.java
│       ├── DecideWorker.java
│       ├── ScreenWatchlistsWorker.java
│       └── VerifyIdentityWorker.java
└── src/test/java/kycaml/workers/
    ├── AssessRiskWorkerTest.java        # 7 tests
    ├── DecideWorkerTest.java        # 6 tests
    ├── ScreenWatchlistsWorkerTest.java        # 4 tests
    └── VerifyIdentityWorkerTest.java        # 4 tests

```
