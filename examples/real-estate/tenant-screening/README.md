# Tenant Screening in Java with Conductor :  Application, Background Check, Credit Check, and Decision

A Java Conductor workflow example for screening prospective tenants. accepting the rental application, running a criminal background check, pulling a credit report against monthly rent, and making an approve/deny decision based on combined results. Uses [Conductor](https://github.

## The Problem

You need to screen rental applicants before signing a lease. Each applicant submits their information, and you must run a background check (criminal history, eviction records, identity verification) and a credit check (credit score, debt-to-income ratio relative to monthly rent). Both checks must complete before the decision step can weigh the results and issue an approve, conditional approve, or deny decision. Screening must be consistent across applicants and comply with Fair Housing regulations. every applicant must go through the same steps with the same criteria.

Without orchestration, screening is inconsistent and slow. Property managers run credit checks on some applicants but forget background checks on others. When the credit bureau API times out, the application sits in limbo. Nobody can prove that all applicants were evaluated with the same criteria, which creates Fair Housing liability. Building this as a monolithic script means a failure in the background check silently produces an incomplete decision.

## The Solution

**You just write the application intake, background check, credit report, and screening decision logic. Conductor handles credit check retries, verification sequencing, and screening audit trails.**

Each screening step is a simple, independent worker. one logs the application, one runs the background check, one pulls the credit report, one makes the approval decision. Conductor takes care of executing them in order, retrying if the credit bureau is temporarily unavailable, ensuring every applicant goes through the exact same pipeline, and maintaining an audit trail that proves consistent evaluation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Application intake, credit check, background verification, and decision notification workers each evaluate one dimension of tenant qualification.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `tsc_apply` | Logs the rental application with applicant name, requested property, and contact details |
| **BackgroundCheckWorker** | `tsc_background` | Runs criminal history, eviction records, and identity verification checks |
| **CreditCheckWorker** | `tsc_credit` | Pulls credit score and evaluates debt-to-income ratio against the monthly rent amount |
| **DecisionWorker** | `tsc_decision` | Decisions the input and returns decision, score |

Workers implement property transaction steps. listing, inspection, escrow, closing,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
tsc_apply
    │
    ▼
tsc_background
    │
    ▼
tsc_credit
    │
    ▼
tsc_decision

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
java -jar target/tenant-screening-1.0.0.jar

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
java -jar target/tenant-screening-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tsc_tenant_screening \
  --version 1 \
  --input '{"applicantName": "test", "propertyId": "TEST-001", "monthlyRent": "sample-monthlyRent"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tsc_tenant_screening -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real screening services. TransUnion SmartMove for background checks, Experian for credit reports, your property management platform for decision tracking, and the workflow runs identically in production.

- **ApplyWorker** (`tsc_apply`): accept applications from your tenant portal or integrate with RentSpree/Avail for standardized application intake
- **BackgroundCheckWorker** (`tsc_background`): integrate with TransUnion SmartMove, Checkr, or Sterling for real criminal/eviction/identity checks
- **CreditCheckWorker** (`tsc_credit`): pull credit reports from Experian RentBureau or TransUnion, calculate rent-to-income ratios
- **DecisionWorker** (`tsc_decision`): implement your screening criteria as a scoring model, send adverse action notices for denials per FCRA requirements

Add new screening criteria or verification providers and the pipeline structure remains the same.

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
tenant-screening/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tenantscreening/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TenantScreeningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── BackgroundCheckWorker.java
│       ├── CreditCheckWorker.java
│       └── DecisionWorker.java
└── src/test/java/tenantscreening/workers/
    ├── ApplyWorkerTest.java        # 2 tests
    ├── BackgroundCheckWorkerTest.java        # 2 tests
    ├── CreditCheckWorkerTest.java        # 2 tests
    └── DecisionWorkerTest.java        # 2 tests

```
