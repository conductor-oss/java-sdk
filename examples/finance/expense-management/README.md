# Expense Management in Java with Conductor

Expense management: submit, validate receipts, categorize, approve, reimburse. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process employee expense reports from submission to reimbursement. An employee submits an expense with receipt, the receipt is validated for authenticity and policy compliance (amount limits, eligible categories), the expense is categorized for accounting, a manager approves it, and the employee is reimbursed. Processing expenses without receipt validation invites fraud; reimbursing without approval violates spending controls.

Without orchestration, you'd build a single expense service that uploads receipts, validates amounts, categorizes line items, emails managers for approval, and triggers payroll reimbursement. manually tracking approval status through email threads, retrying failed receipt OCR, and logging everything for tax audit.

## The Solution

**You just write the expense workers. Report submission, receipt validation, GL categorization, manager approval, and reimbursement. Conductor handles sequential processing, automatic retries when the receipt validation service times out, and complete expense tracking from submission to reimbursement.**

Each expense concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, validate, categorize, approve, reimburse), retrying if the receipt validation service times out, tracking every expense from submission to reimbursement, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the expense lifecycle: SubmitExpenseWorker captures the report, ValidateReceiptsWorker checks receipt authenticity, CategorizeWorker assigns GL codes, ApproveExpenseWorker routes for manager sign-off, and ReimburseWorker triggers payment.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveExpenseWorker** | `exp_approve_expense` | Approves the expense |
| **CategorizeWorker** | `exp_categorize` | Categorizes the input and computes final category, gl code, tax deductible |
| **ReimburseWorker** | `exp_reimburse` | Reimburse the data and computes reimbursement status, payment id, expected date |
| **SubmitExpenseWorker** | `exp_submit_expense` | Handles submit expense |
| **ValidateReceiptsWorker** | `exp_validate_receipts` | Checking receipt for expense |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
exp_submit_expense
    │
    ▼
exp_validate_receipts
    │
    ▼
exp_categorize
    │
    ▼
exp_approve_expense
    │
    ▼
exp_reimburse

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
java -jar target/expense-management-1.0.0.jar

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
java -jar target/expense-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow expense_management_workflow \
  --version 1 \
  --input '{"expenseId": "TEST-001", "employeeId": "TEST-001", "amount": 100, "category": "general", "receiptUrl": "https://example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w expense_management_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ValidateReceiptsWorker to your OCR service (ABBYY, Google Document AI), CategorizeWorker to your chart of accounts, and ReimburseWorker to your payroll or AP system. The workflow definition stays exactly the same.

- **Receipt validator**: use OCR (AWS Textract, Google Vision) to extract receipt details; cross-check amounts, dates, and merchant against policy rules
- **Categorizer**: map expenses to GL account codes using your chart of accounts; apply per-diem and mileage calculations
- **Approver**: route to the appropriate manager based on amount thresholds and reporting hierarchy; use a WAIT task for human approval
- **Reimbursement processor**: trigger reimbursement via your payroll system (ADP, Gusto, Workday) or corporate card system

Connect each worker to your real OCR service, GL system, and payroll reimbursement platform while keeping the same output fields, and the expense workflow needs no modifications.

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
expense-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/expensemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExpenseManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveExpenseWorker.java
│       ├── CategorizeWorker.java
│       ├── ReimburseWorker.java
│       ├── SubmitExpenseWorker.java
│       └── ValidateReceiptsWorker.java
└── src/test/java/expensemanagement/workers/
    ├── ApproveExpenseWorkerTest.java        # 2 tests
    └── ReimburseWorkerTest.java        # 2 tests

```
