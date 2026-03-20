# Regulatory Reporting in Java with Conductor

Regulatory reporting workflow: collect data, validate, format, submit, and confirm. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to submit a regulatory report to a governing body. This involves collecting the required data from source systems, validating it against regulatory rules and completeness checks, formatting it according to the regulator's specifications, submitting the report electronically, and confirming acceptance. Late or inaccurate filings result in fines, enforcement actions, or loss of operating licenses.

Without orchestration, you'd build a reporting pipeline that queries multiple databases, applies validation rules, formats XML/XBRL/CSV output, uploads to the regulator's portal, and checks for acceptance .  manually handling data quality issues, retrying failed submissions, and meeting tight filing deadlines.

## The Solution

**You just write the reporting workers. Data collection, validation, regulatory formatting, submission, and acceptance confirmation. Conductor handles pipeline ordering, automatic retries when the regulatory portal is down, and complete filing lifecycle tracking for audit evidence.**

Each reporting concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (collect, validate, format, submit, confirm), retrying if the regulatory portal is down, tracking every filing with complete audit trail, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the reporting pipeline: CollectDataWorker aggregates data from source systems, ValidateWorker applies regulatory rules, FormatWorker generates the required output format, SubmitWorker uploads to the regulatory portal, and ConfirmWorker checks for acceptance.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `reg_collect_data` | Collects data |
| **ConfirmWorker** | `reg_confirm` | Confirm. Computes and returns accepted, receipt number, confirmed at |
| **FormatWorker** | `reg_format` | Formatting report as |
| **SubmitWorker** | `reg_submit` | Submit. Computes and returns submission id, submitted at, deadline, submitted before |
| **ValidateWorker** | `reg_validate` | Validate. Computes and returns validated data, error count, passed |

Workers simulate financial operations .  risk assessment, compliance checks, settlement ,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
reg_collect_data
    │
    ▼
reg_validate
    │
    ▼
reg_format
    │
    ▼
reg_submit
    │
    ▼
reg_confirm
```

## Example Output

```
=== Example 497: Regulatory Reporting ===

Step 1: Registering task definitions...
  Registered: reg_collect_data, reg_validate, reg_format, reg_submit, reg_confirm

Step 2: Registering workflow 'regulatory_reporting_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Gathering
  [confirm] Submission
  [format] Formatting report as
  [submit] Submitting
  [validate] Validation:

  Status: COMPLETED
  Output: {data=..., recordCount=..., accepted=..., receiptNumber=...}

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
java -jar target/regulatory-reporting-1.0.0.jar
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
java -jar target/regulatory-reporting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow regulatory_reporting_workflow \
  --version 1 \
  --input '{"reportId": "REG-2024-Q1-CALL", "REG-2024-Q1-CALL": "reportType", "reportType": "CALL", "CALL": "reportingPeriod", "reportingPeriod": "2024-Q1", "2024-Q1": "entity", "entity": "First National Bank", "First National Bank": "sample-First National Bank"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w regulatory_reporting_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CollectDataWorker to your source systems, FormatWorker to your regulatory template engine (XBRL, XML), and SubmitWorker to the regulator's electronic filing portal. The workflow definition stays exactly the same.

- **Data collector**: aggregate data from your GL, trading systems, and risk platforms; handle data from multiple legal entities
- **Validator**: apply regulatory validation rules (Fed Y-9C, SEC Form PF, CCAR, Basel III) with configurable rule sets per report type
- **Formatter**: generate output in required formats (XBRL for SEC, XML for CFTC, CSV for state regulators, SDMX for central banks)
- **Submitter**: upload to regulatory portals (EDGAR, CFTC Portal, Fed Reserve) via their APIs or SFTP
- **Confirmation handler**: monitor for acceptance/rejection, handle resubmission if rejected, and archive the filing for retention

Connect each worker to your real data warehouse, regulatory template engine, and filing portal while preserving the same output contract, and the reporting pipeline runs without changes.

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
regulatory-reporting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/regulatoryreporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RegulatoryReportingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectDataWorker.java
│       ├── ConfirmWorker.java
│       ├── FormatWorker.java
│       ├── SubmitWorker.java
│       └── ValidateWorker.java
└── src/test/java/regulatoryreporting/workers/
    ├── CollectDataWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 3 tests
```
