# Expense Reporting in Java with Conductor

Expense reporting: collect, categorize, submit, approve, reimburse. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to process a travel expense report from receipt collection through reimbursement. An employee returns from a trip with receipts for flights, hotels, meals, and transportation. You collect all receipts and line items. You categorize each expense (airfare, lodging, meals, ground transport, miscellaneous). You submit the categorized report for approval. The manager reviews and approves or rejects it. Finally, approved expenses are reimbursed to the employee's payroll or bank account.

If categorization misclassifies a meal as lodging, the report totals are wrong and the employee gets audited. If approval succeeds but the reimbursement payment fails, the employee is out of pocket with no visibility into when they'll be paid. Without orchestration, you'd build a monolithic expense handler that mixes OCR receipt parsing, category rules, approval workflows, and payment processing .  making it impossible to update categorization rules, test approval logic independently, or audit which receipts drove which reimbursement amounts.

## The Solution

**You just write the receipt collection, expense categorization, report submission, approval, and reimbursement logic. Conductor handles receipt processing retries, validation sequencing, and expense audit trails.**

CollectWorker gathers all receipts and line items for the trip. CategorizeWorker classifies each expense into the correct category (airfare, lodging, meals, ground transport) based on vendor type and amount. SubmitWorker assembles the categorized report with totals per category and submits it for review. ApproveWorker routes the report to the employee's manager for approval. ReimburseWorker processes the approved amount through payroll or direct deposit. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Receipt capture, categorization, policy validation, and reimbursement workers each manage one step of turning travel receipts into approved expenses.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `exr_approve` | Processing expense reporting step |
| **CategorizeWorker** | `exr_categorize` | Processing expense reporting step |
| **CollectWorker** | `exr_collect` | Processing expense reporting step |
| **ReimburseWorker** | `exr_reimburse` | Processing expense reporting step |
| **SubmitWorker** | `exr_submit` | Processing expense reporting step |

Workers simulate travel operations .  booking, approval, itinerary generation ,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### The Workflow

```
exr_collect
    │
    ▼
exr_categorize
    │
    ▼
exr_submit
    │
    ▼
exr_approve
    │
    ▼
exr_reimburse
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
java -jar target/expense-reporting-1.0.0.jar
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
java -jar target/expense-reporting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow exr_expense_reporting \
  --version 1 \
  --input '{"employeeId": "TEST-001", "tripId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w exr_expense_reporting -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real expense systems .  an OCR service for receipt parsing, your ERP for categorization rules, your payroll system for reimbursement, and the workflow runs identically in production.

- **CollectWorker** (`exr_collect`): pull receipts from your expense management system (SAP Concur, Expensify, Brex) or OCR-scan uploaded receipt images via Google Vision or AWS Textract
- **CategorizeWorker** (`exr_categorize`): apply your company's expense categorization rules, matching merchant codes to categories and flagging out-of-policy items
- **SubmitWorker** (`exr_submit`): create the expense report in your ERP (SAP, Oracle, NetSuite) with line items, categories, and supporting receipt attachments
- **ApproveWorker** (`exr_approve`): route the report to the manager via your approval platform (ServiceNow, Slack workflow) and record the approval decision
- **ReimburseWorker** (`exr_reimburse`): process the reimbursement through payroll (ADP, Workday) or initiate a direct bank transfer for the approved amount

Swap receipt OCR tools or reimbursement systems and the reporting pipeline remains unchanged.

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
expense-reporting-expense-reporting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/expensereporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExpenseReportingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── CategorizeWorker.java
│       ├── CollectWorker.java
│       ├── ReimburseWorker.java
│       └── SubmitWorker.java
└── src/test/java/expensereporting/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── CategorizeWorkerTest.java        # 2 tests
    ├── CollectWorkerTest.java        # 2 tests
    ├── ReimburseWorkerTest.java        # 2 tests
    └── SubmitWorkerTest.java        # 2 tests
```
