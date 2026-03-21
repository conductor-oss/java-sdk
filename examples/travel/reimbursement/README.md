# Reimbursement in Java with Conductor

Reimbursement: submit, validate, approve, process, notify. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process a travel reimbursement claim from submission through payment, the employee submits a claim with receipts and amounts, the system validates receipts and checks policy compliance, a manager approves or rejects the claim, the finance team processes the approved payment, and the employee receives notification of the reimbursement status. Each step depends on the previous one's outcome.

If validation flags a receipt as invalid but approval proceeds anyway, the company reimburses an unverified expense. If payment processing fails after approval, the employee sees "approved" but never receives the money. Without orchestration, you'd build a monolithic reimbursement handler that mixes receipt validation, approval routing, payment processing, and notifications. Making it impossible to change validation rules, add a second-level approval for large amounts, or audit which receipts were verified for which claims.

## The Solution

**You just write the claim submission, receipt validation, manager approval, payment processing, and notification logic. Conductor handles approval routing retries, payment sequencing, and reimbursement audit trails.**

SubmitWorker captures the reimbursement claim with receipt details, amounts, and expense category. ValidateWorker checks each receipt for validity (date, vendor, amount), verifies policy compliance, and confirms the total matches submitted receipts. ApproveWorker routes the validated claim to the employee's manager for approval, recording the approver and decision. ProcessWorker initiates the payment through payroll or direct deposit for the approved amount. NotifyWorker sends the employee a notification with the reimbursement status and expected payment date. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Expense validation, approval routing, payment processing, and notification workers each handle one stage of returning travel costs to the employee.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `rmb_approve` | Evaluates approval criteria and computes approved, approved by |
| **NotifyWorker** | `rmb_notify` | Notify. Computes and returns notified |
| **ProcessWorker** | `rmb_process` | Processes the reimbursement payment to the employee's account |
| **SubmitWorker** | `rmb_submit` | Submit. Computes and returns claim id, submitted |
| **ValidateWorker** | `rmb_validate` | Validate. Computes and returns valid, receipts verified |

Workers implement travel operations. booking, approval, itinerary generation,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### The Workflow

```
rmb_submit
    │
    ▼
rmb_validate
    │
    ▼
rmb_approve
    │
    ▼
rmb_process
    │
    ▼
rmb_notify

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
java -jar target/reimbursement-1.0.0.jar

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
java -jar target/reimbursement-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rmb_reimbursement \
  --version 1 \
  --input '{"employeeId": "TEST-001", "amount": 100, "category": "general", "receiptCount": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rmb_reimbursement -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real finance systems. your receipt validation service for compliance checks, your HR directory for manager approvals, your payroll API for direct deposit, and the workflow runs identically in production.

- **SubmitWorker** (`rmb_submit`): create the reimbursement claim in your expense management system (SAP Concur, Expensify, Brex) and attach receipt images
- **ValidateWorker** (`rmb_validate`): verify receipts via OCR (AWS Textract, Google Vision), check amounts against policy limits, and flag duplicates or out-of-policy expenses
- **ApproveWorker** (`rmb_approve`): route the claim to the manager via your approval platform (ServiceNow, Slack, email) and record the approval decision and timestamp
- **ProcessWorker** (`rmb_process`): initiate payment through your payroll system (ADP, Workday) or accounting platform (NetSuite, QuickBooks) for the approved amount
- **NotifyWorker** (`rmb_notify`): send the employee a notification via email or Slack with the reimbursement status, approved amount, and expected payment date

Swap approval workflows or payment processors and the reimbursement pipeline keeps working.

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
reimbursement-reimbursement/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reimbursement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReimbursementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── NotifyWorker.java
│       ├── ProcessWorker.java
│       ├── SubmitWorker.java
│       └── ValidateWorker.java
└── src/test/java/reimbursement/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    ├── ProcessWorkerTest.java        # 2 tests
    ├── SubmitWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests

```
