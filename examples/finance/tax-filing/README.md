# Tax Filing in Java with Conductor

Tax filing: collect data, calculate tax, validate, file return, confirm. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to prepare and file a tax return. The workflow collects income, deduction, and credit data from source systems, calculates the tax liability using current tax tables and rules, validates the return for accuracy and completeness, files it electronically with the tax authority, and confirms acceptance. Incorrect calculations result in penalties; late filing incurs interest charges and potential audits.

Without orchestration, you'd build a tax preparation service that aggregates data from W-2s, 1099s, and other sources, applies tax logic inline, validates against IRS rules, transmits via MeF, and checks for acknowledgment .  manually handling data discrepancies, retrying failed transmissions, and meeting the April 15 deadline.

## The Solution

**You just write the tax workers. Income data collection, liability calculation, return validation, electronic filing, and acceptance confirmation. Conductor handles step sequencing, automatic retries when the e-filing system is unavailable, and complete return preparation tracking for audit defense.**

Each tax-filing concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (collect data, calculate tax, validate, file, confirm), retrying if the e-filing system is unavailable, tracking every return's preparation lifecycle, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the filing lifecycle: CollectDataWorker aggregates income and deduction data, CalculateTaxWorker computes liability, ValidateFilingWorker checks accuracy, FileReturnWorker transmits to the tax authority, and ConfirmSubmissionWorker verifies acceptance.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateTaxWorker** | `txf_calculate_tax` | Calculate Tax. Computes and returns tax liability, taxable income, effective rate |
| **CollectDataWorker** | `txf_collect_data` | Collect Data. Computes and returns gross income, deductions, credits, w2 count |
| **ConfirmSubmissionWorker** | `txf_confirm_submission` | Confirms the submission |
| **FileReturnWorker** | `txf_file_return` | File Return. Computes and returns filing id, confirmation number, filed at |
| **ValidateFilingWorker** | `txf_validate_filing` | Validate Filing. Computes and returns validated, validation checks, warnings |

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
txf_collect_data
    │
    ▼
txf_calculate_tax
    │
    ▼
txf_validate_filing
    │
    ▼
txf_file_return
    │
    ▼
txf_confirm_submission
```

## Example Output

```
=== Example 503: Tax Filing ===

Step 1: Registering task definitions...
  Registered: txf_collect_data, txf_calculate_tax, txf_validate_filing, txf_file_return, txf_confirm_submission

Step 2: Registering workflow 'tax_filing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [calculate] Taxable: $
  [collect] Gathering tax data for
  [confirm] Filing
  [file] Filing return for
  [validate] Validating filing .  liability: $

  Status: COMPLETED
  Output: {taxLiability=..., taxableIncome=..., effectiveRate=..., grossIncome=...}

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
java -jar target/tax-filing-1.0.0.jar
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
java -jar target/tax-filing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tax_filing_workflow \
  --version 1 \
  --input '{"taxpayerId": "TP-882244", "TP-882244": "taxYear", "taxYear": 2025, "individual": "INDIVUAL-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tax_filing_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CollectDataWorker to your W-2, 1099, and financial data sources, CalculateTaxWorker to your tax calculation engine with current tax tables, and FileReturnWorker to the IRS MeF e-filing system. The workflow definition stays exactly the same.

- **Data collector**: aggregate tax data from payroll (W-2), investment accounts (1099-B/DIV/INT), and deduction sources (1098, charitable receipts) via APIs
- **Tax calculator**: apply current tax tables, brackets, credits, and deductions using a tax engine (Drake, CCH Axcess, custom rules)
- **Return validator**: run IRS/state validation rules (MeF business rules, math error checks, consistency validations)
- **E-filer**: transmit returns via IRS MeF (Modernized e-File) system or state e-filing portals
- **Confirmation handler**: monitor for IRS acknowledgment, handle rejections with error resolution, and archive accepted returns

Swap in real tax data aggregation, IRS e-file (MeF), and acknowledgment processing while keeping the same output fields, and the filing workflow runs without modification.

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
tax-filing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taxfiling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaxFilingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateTaxWorker.java
│       ├── CollectDataWorker.java
│       ├── ConfirmSubmissionWorker.java
│       ├── FileReturnWorker.java
│       └── ValidateFilingWorker.java
└── src/test/java/taxfiling/workers/
    └── CalculateTaxWorkerTest.java        # 2 tests
```
