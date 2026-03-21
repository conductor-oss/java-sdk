# Grant Management in Java with Conductor

A Java Conductor workflow example demonstrating Grant Management. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Your nonprofit is applying for a foundation grant to fund a community program. The grants team needs to submit the application with the organization name and requested amount, have the grant committee review and score the application, approve the grant based on the review score, disburse the funds to the organization, and file a grant usage report documenting expenditures and outcomes. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the grant application, budget tracking, milestone reporting, and compliance documentation logic. Conductor handles submission retries, compliance tracking, and grant lifecycle audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Application preparation, submission, compliance tracking, and reporting workers each manage one stage of the grant lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `gmt_apply` | Submits the grant application from the organization for the specified program, returning an application ID and submission date |
| **ApproveWorker** | `gmt_approve` | Approves the grant based on the review score, confirming the approved funding amount |
| **FundWorker** | `gmt_fund` | Disburses the approved amount to the organization, assigning a grant ID and recording the disbursement date |
| **ReportWorker** | `gmt_report` | Files the grant usage report linking the grant ID, organization, and funding status |
| **ReviewWorker** | `gmt_review` | Reviews the application for the requested amount, assigning a score and recommendation from the grant committee |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### The Workflow

```
gmt_apply
    │
    ▼
gmt_review
    │
    ▼
gmt_approve
    │
    ▼
gmt_fund
    │
    ▼
gmt_report

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
java -jar target/grant-management-1.0.0.jar

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
java -jar target/grant-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow grant_management_752 \
  --version 1 \
  --input '{"organizationName": "test", "grantProgram": "sample-grantProgram", "requestedAmount": 100}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w grant_management_752 -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real grant systems .  your grants database for application tracking, your finance platform for budget management, your funder portals for progress reporting, and the workflow runs identically in production.

- **ApplyWorker** (`gmt_apply`): submit the application through your grants portal or create the record in Salesforce NPSP Grants Management, attaching required documents from your document store
- **ReviewWorker** (`gmt_review`): route the application to the grant committee via your workflow system, collect reviewer scores, and aggregate the recommendation
- **ApproveWorker** (`gmt_approve`): update the grant status in your CRM (Salesforce NPSP, DonorPerfect) and trigger approval notifications to stakeholders
- **FundWorker** (`gmt_fund`): initiate the fund disbursement through your accounting system (Sage Intacct, QuickBooks) or banking API, recording the transaction
- **ReportWorker** (`gmt_report`): pull expenditure data from your accounting system, compile outcomes from your program database, and submit the report to the funder's portal

Change application portals or reporting requirements and the grant pipeline handles them without modification.

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
grant-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/grantmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GrantManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── ApproveWorker.java
│       ├── FundWorker.java
│       ├── ReportWorker.java
│       └── ReviewWorker.java
└── src/test/java/grantmanagement/workers/
    ├── ApplyWorkerTest.java        # 1 tests
    └── ReportWorkerTest.java        # 1 tests

```
