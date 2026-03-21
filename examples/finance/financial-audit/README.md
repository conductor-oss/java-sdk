# Financial Audit in Java with Conductor

Financial audit: define scope, collect evidence, test controls, generate report, remediate. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to conduct a financial audit for a business entity. This involves defining the audit scope and objectives, collecting evidence (financial statements, transaction records, supporting documents), testing internal controls for effectiveness, generating the audit report with findings, and tracking remediation of any deficiencies. An audit without proper evidence collection is incomplete; findings without remediation tracking are toothless.

Without orchestration, you'd manage the audit process through spreadsheets and email. manually tracking which evidence has been collected, scheduling control tests, drafting reports in documents, and following up on remediation through calendar reminders.

## The Solution

**You just write the audit workers. Scope definition, evidence collection, control testing, report generation, and remediation tracking. Conductor handles stage sequencing, automatic retries when a data source is unavailable, and timestamped evidence collection for SOX and regulatory compliance.**

Each audit concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (scope, collect, test, report, remediate), retrying if a data source is unavailable, tracking the entire audit lifecycle with timestamps and evidence, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers cover the audit lifecycle: DefineScopeWorker sets objectives, CollectEvidenceWorker gathers financial records, GenerateReportWorker produces findings and recommendations, and RemediateWorker tracks deficiency resolution.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEvidenceWorker** | `fau_collect_evidence` | Collecting evidence for scope areas |
| **DefineScopeWorker** | `fau_define_scope` | Defines the scope |
| **GenerateReportWorker** | `fau_generate_report` | Generates the audit report with findings, discrepancies, and recommendations |
| **RemediateWorker** | `fau_remediate` | Remediate. Computes and returns remediation plan id, actions created, target completion date |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
fau_define_scope
    │
    ▼
fau_collect_evidence
    │
    ▼
fau_test_controls
    │
    ▼
fau_generate_report
    │
    ▼
fau_remediate

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
java -jar target/financial-audit-1.0.0.jar

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
java -jar target/financial-audit-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow financial_audit_workflow \
  --version 1 \
  --input '{"auditId": "TEST-001", "entityName": "test", "auditType": "standard", "fiscalYear": "sample-fiscalYear"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w financial_audit_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectEvidenceWorker to your financial systems and document repository, GenerateReportWorker to your audit reporting platform, and RemediateWorker to your GRC tool for tracking corrective actions. The workflow definition stays exactly the same.

- **Scope definer**: pull audit requirements from regulatory frameworks (SOX, PCAOB) and map to your entity's control environment
- **Evidence collector**: extract financial data from your ERP (SAP, Oracle), bank statements via Plaid, and supporting documents from your DMS
- **Control tester**: execute automated control tests (reconciliation checks, segregation of duties validation, access control reviews)
- **Report generator**: generate audit reports in standard formats (PCAOB AS 2201, ISA 700) with findings, risk ratings, and management responses
- **Remediation tracker**: create and monitor remediation tasks with deadlines and status updates

Point each worker at your real audit management platform and evidence repository while preserving the same output contract, and the audit workflow continues without modification.

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
financial-audit/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/financialaudit/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FinancialAuditExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectEvidenceWorker.java
│       ├── DefineScopeWorker.java
│       ├── GenerateReportWorker.java
│       └── RemediateWorker.java
└── src/test/java/financialaudit/workers/
    ├── DefineScopeWorkerTest.java        # 2 tests
    └── TestControlsWorkerTest.java        # 2 tests

```
