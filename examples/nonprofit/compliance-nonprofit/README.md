# Compliance Nonprofit in Java with Conductor

A Java Conductor workflow example demonstrating Compliance Nonprofit. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Your nonprofit's fiscal year just closed, and you need to run the annual compliance review before filing deadlines. The compliance team must audit the organization's financials and governance, verify that all required filings (Form 990, state registration, annual report) are current, check IRS, state, and donor compliance requirements, generate a compliance report with findings and risk scores, and submit the final compliance package to regulators. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the compliance checking, documentation gathering, review, and regulatory filing logic. Conductor handles documentation retries, assessment sequencing, and compliance audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Regulation identification, documentation review, gap assessment, and reporting workers each handle one aspect of nonprofit regulatory compliance.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `cnp_audit` | Audits the organization's financials, governance, and program expense ratio for the fiscal year |
| **CheckRequirementsWorker** | `cnp_check_requirements` | Validates IRS, state, and donor compliance requirements, returning an overall compliance status |
| **ReportWorker** | `cnp_report` | Generates the annual compliance report with findings count, overall status, and recommended actions |
| **SubmitWorker** | `cnp_submit` | Submits the compliance package for the organization's EIN and returns a confirmation ID |
| **VerifyFilingsWorker** | `cnp_verify_filings` | Verifies that Form 990, state registration, and annual report filings are current for the EIN |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cnp_audit
    │
    ▼
cnp_verify_filings
    │
    ▼
cnp_check_requirements
    │
    ▼
cnp_report
    │
    ▼
cnp_submit
```

## Example Output

```
=== Example 760: Nonprofit Compliance ===

Step 1: Registering task definitions...
  Registered: cnp_audit, cnp_verify_filings, cnp_check_requirements, cnp_report, cnp_submit

Step 2: Registering workflow 'compliance_nonprofit_760'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [audit] Auditing
  [requirements] Checking all compliance requirements
  [report] Generating compliance report for
  [submit] Submitting compliance package for EIN
  [filings] Verifying filings for EIN

  Status: COMPLETED

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
java -jar target/compliance-nonprofit-1.0.0.jar
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
java -jar target/compliance-nonprofit-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow compliance_nonprofit_760 \
  --version 1 \
  --input '{"organizationName": "sample-name", "HopeWorks Foundation": "sample-HopeWorks Foundation", "fiscalYear": "sample-fiscalYear", "ein": "sample-ein"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w compliance_nonprofit_760 -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real compliance systems .  your document management for policy review, your grant portal for reporting requirements, your filing platform for regulatory submissions, and the workflow runs identically in production.

- **AuditWorker** (`cnp_audit`): pull financial data from your accounting system (QuickBooks, Sage Intacct) and governance records from your board management platform to compute the program expense ratio
- **VerifyFilingsWorker** (`cnp_verify_filings`): query the IRS e-file status API and your state's charity registration portal to confirm all filings are up to date
- **CheckRequirementsWorker** (`cnp_check_requirements`): cross-reference requirements from your compliance checklist database against actual filings and donor restrictions stored in Salesforce NPSP or DonorPerfect
- **ReportWorker** (`cnp_report`): generate the compliance report as a PDF using a template engine and store it in your document management system for board review
- **SubmitWorker** (`cnp_submit`): submit the compliance package via the IRS e-file API or your state's electronic filing system, recording the confirmation number

Update regulatory requirements or documentation standards and the compliance pipeline adjusts seamlessly.

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
compliance-nonprofit/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/compliancenonprofit/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplianceNonprofitExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuditWorker.java
│       ├── CheckRequirementsWorker.java
│       ├── ReportWorker.java
│       ├── SubmitWorker.java
│       └── VerifyFilingsWorker.java
└── src/test/java/compliancenonprofit/workers/
    ├── AuditWorkerTest.java        # 1 tests
    └── SubmitWorkerTest.java        # 1 tests
```
