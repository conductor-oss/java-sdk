# Payroll Workflow in Java with Conductor

Payroll processing: collect hours, calculate gross, apply deductions, process, distribute stubs. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process payroll for a department. This means collecting timesheets and hours worked for the pay period, calculating gross pay (salary, overtime, bonuses), applying deductions (taxes, benefits, retirement contributions), processing the net payments, and distributing pay stubs to employees. Incorrect gross calculations result in wage violations; missed deductions create tax liability.

Without orchestration, you'd build a batch payroll job that queries the timesheet system, computes pay inline, applies tax tables, triggers ACH payments, and generates stubs. manually handling employees who clock in late, retrying failed payment submissions, and logging everything to satisfy Department of Labor audits.

## The Solution

**You just write the payroll workers. Timesheet collection, gross pay calculation, deduction application, payment processing, and stub distribution. Conductor handles sequential processing, automatic retries when the payment processor is unavailable, and detailed payroll run tracking for Department of Labor compliance.**

Each payroll concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect hours, calculate gross, apply deductions, process payments, distribute stubs), retrying if the payment processor is unavailable, tracking every payroll run with full calculation details, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the payroll cycle: CollectHoursWorker gathers timesheets, CalculateGrossWorker computes earnings, ApplyDeductionsWorker handles taxes and benefits, ProcessPayrollWorker submits the payment batch, and DistributeStubsWorker delivers pay statements.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyDeductionsWorker** | `prl_apply_deductions` | Apply Deductions. Computes and returns net payroll, total deductions, federal tax, state tax |
| **CalculateGrossWorker** | `prl_calculate_gross` | Calculate Gross. Computes and returns gross payroll, employee count |
| **CollectHoursWorker** | `prl_collect_hours` | Collecting hours for period |
| **DistributeStubsWorker** | `prl_distribute_stubs` | Distribute Stubs. Computes and returns distributed count, method |
| **ProcessPayrollWorker** | `prl_process_payroll` | Process Payroll. Computes and returns batch id, bank reference |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
prl_collect_hours
    │
    ▼
prl_calculate_gross
    │
    ▼
prl_apply_deductions
    │
    ▼
prl_process_payroll
    │
    ▼
prl_distribute_stubs

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
java -jar target/payroll-workflow-1.0.0.jar

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
java -jar target/payroll-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow payroll_workflow \
  --version 1 \
  --input '{"payPeriodId": "TEST-001", "departmentId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w payroll_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectHoursWorker to your timesheet system, ApplyDeductionsWorker to your tax table and benefits engine, and ProcessPayrollWorker to your bank's ACH payment interface. The workflow definition stays exactly the same.

- **Hours collector**: pull timesheet data from your time-tracking system (Kronos, ADP, BambooHR, Gusto)
- **Gross calculator**: compute pay using salary/hourly rates, overtime rules (FLSA), shift differentials, bonuses, and commissions
- **Deductions processor**: apply federal/state/local tax withholding, pre-tax benefits (401k, HSA, insurance premiums), and post-tax deductions (garnishments, union dues)
- **Payment processor**: submit ACH direct deposits via your payroll provider (ADP, Gusto, Paychex) or bank's NACHA file
- **Stub distributor**: generate and distribute digital pay stubs via your employee portal or email

Replace demo payroll calculations with real timesheet, tax, and payment integrations while preserving the same output structure, and the payroll workflow continues without modification.

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
payroll-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/payrollworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PayrollWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyDeductionsWorker.java
│       ├── CalculateGrossWorker.java
│       ├── CollectHoursWorker.java
│       ├── DistributeStubsWorker.java
│       └── ProcessPayrollWorker.java
└── src/test/java/payrollworkflow/workers/
    ├── ApplyDeductionsWorkerTest.java        # 2 tests
    └── CalculateGrossWorkerTest.java        # 2 tests

```
