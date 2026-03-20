# Insurance Underwriting in Java with Conductor

Insurance underwriting with SWITCH decision routing for accept/decline/refer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to underwrite an insurance application. The workflow collects applicant information and coverage requirements, assesses the risk based on applicant profile and coverage type, makes an underwriting decision (accept at standard rates, accept with modified terms, decline, or refer for manual review), and communicates the decision. Accepting high-risk applicants at standard rates leads to adverse selection; declining without proper assessment loses good business.

Without orchestration, you'd build a single underwriting engine that collects data, runs risk models, makes decisions, and sends letters .  manually handling referrals that require human underwriter review, retrying failed risk-model API calls, and logging every decision to satisfy state insurance commissioner audits.

## The Solution

**You just write the underwriting workers. Application collection, risk assessment, accept/decline/refer routing, quote generation, and policy binding. Conductor handles conditional SWITCH routing for accept, decline, and refer decisions, automatic retries when the risk model is unavailable, and full application tracking for insurance commissioner audits.**

Each underwriting concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of collecting information, assessing risk, routing via a SWITCH task to the correct decision (accept, decline, refer), and communicating the outcome ,  retrying if the risk model service is unavailable, tracking every application's underwriting journey, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Seven workers manage the underwriting process: CollectAppWorker gathers applicant data, AssessRiskWorker evaluates the risk profile, SWITCH routes to AcceptWorker, DeclineWorker, or ReferWorker based on the assessment, QuoteWorker calculates the premium, and BindWorker issues the policy.

| Worker | Task | What It Does |
|---|---|---|
| **AcceptWorker** | `uw_accept` | Accept. Computes and returns accepted, accepted at |
| **AssessRiskWorker** | `uw_assess_risk` | Assess Risk. Computes and returns risk class, decision, risk score, decline reason |
| **BindWorker** | `uw_bind` | Binds the policy if the underwriting decision is 'accept'. Generates a policy number, records the effective date and premium. If the decision is not 'accept', marks the policy as unbound |
| **CollectAppWorker** | `uw_collect_app` | Collect App. Computes and returns applicant data |
| **DeclineWorker** | `uw_decline` | Records the application decline with the reason and flags the application as eligible for appeal |
| **QuoteWorker** | `uw_quote` | Quote. Computes and returns premium, premium frequency, quote valid days |
| **ReferWorker** | `uw_refer` | Routes the application for senior underwriter review with the referral reason and assigns a reviewer |

Workers simulate financial operations .  risk assessment, compliance checks, settlement ,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
uw_collect_app
    │
    ▼
uw_assess_risk
    │
    ▼
uw_quote
    │
    ▼
SWITCH (uw_switch_ref)
    ├── accept: uw_accept
    ├── decline: uw_decline
    ├── refer: uw_refer
    └── default: uw_refer
    │
    ▼
uw_bind
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
java -jar target/insurance-underwriting-1.0.0.jar
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
java -jar target/insurance-underwriting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow insurance_underwriting_workflow \
  --version 1 \
  --input '{"applicationId": "TEST-001", "applicantName": "test", "coverageType": "test-value", "coverageAmount": 100}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w insurance_underwriting_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CollectAppWorker to your application portal, AssessRiskWorker to your actuarial risk model, and BindWorker to your policy administration system for policy issuance. The workflow definition stays exactly the same.

- **Data collector**: pull applicant data from your policy admin system, MIB (Medical Information Bureau), motor vehicle records, or prescription databases
- **Risk assessor**: run actuarial risk models, ML-based risk scoring, or rule-based underwriting guidelines specific to your coverage type
- **Decision maker**: apply underwriting authority limits, generate quotes with premium calculations, and route referrals to human underwriters via WAIT tasks
- **Decision communicator**: send acceptance letters, decline notices, or modification offers via email and update your policy administration system

Swap in your real risk models, rating engine, and policy administration system while returning the same fields, and the underwriting workflow: including SWITCH routing, runs without changes.

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
insurance-underwriting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/insuranceunderwriting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InsuranceUnderwritingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcceptWorker.java
│       ├── AssessRiskWorker.java
│       ├── BindWorker.java
│       ├── CollectAppWorker.java
│       ├── DeclineWorker.java
│       ├── QuoteWorker.java
│       └── ReferWorker.java
└── src/test/java/insuranceunderwriting/workers/
    ├── BindWorkerTest.java        # 3 tests
    └── QuoteWorkerTest.java        # 3 tests
```
