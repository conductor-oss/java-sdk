# Expense Approval in Java Using Conductor: Policy Validation, SWITCH for Auto-Approve vs. Manager Approval via WAIT, and Processing

A Java Conductor workflow example for expense approval. validating an expense against policy rules (amount > $100 or category "travel" requires approval), using SWITCH to route expenses that need approval to a WAIT task for manager review, and processing the expense after approval or auto-approval. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Expenses Above Policy Thresholds Need Human Approval

An employee submits an expense report, but not all expenses can be auto-approved. The workflow validates the expense against policy rules. If the amount exceeds $100 or the category is "travel", a human manager must approve it via a WAIT task. Otherwise, the expense is processed automatically. The SWITCH task routes between the approval path and the direct-processing path based on the policy check. If processing fails after approval, you need to retry it without asking the manager to re-approve.

## The Solution

**You just write the policy validation and expense processing workers. Conductor handles the routing, approval holds, and retries.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. Your code handles the decision logic.

### What You Write: Workers

ValidatePolicyWorker checks expense amounts and categories, while ProcessWorker handles reimbursement. Neither knows about the SWITCH or WAIT tasks that connect them.

| Worker | Task | What It Does |
|---|---|---|
| **ValidatePolicyWorker** | `exp_validate_policy` | Checks the expense amount and category against policy rules. amounts over $100 or "travel" category require manager approval; returns `approvalRequired: "true"` or `"false"` as a string for the SWITCH evaluator |
| *SWITCH task* | `approval_switch` | Routes based on `approvalRequired`. `"true"` sends to a WAIT task for manager approval, default skips straight to processing | Built-in Conductor SWITCH.; no worker needed |
| *WAIT task* | `wait_for_approval` | Pauses the workflow until a manager approves or rejects the expense via `POST /tasks/{taskId}`. See [Completing the WAIT Task](#completing-the-wait-task-human-approval) | Built-in Conductor WAIT.; no worker needed |
| **ProcessWorker** | `exp_process` | Finalizes the approved expense: posts the reimbursement to payroll, updates the general ledger, and sends the employee a confirmation email; returns `processed: true` |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
exp_validate_policy
    │
    ▼
SWITCH (approval_switch)
    ├── "true": wait_for_approval (WAIT)
    │
    ▼
exp_process

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
java -jar target/expense-approval-1.0.0.jar

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

## Example Output

```
=== Expense Approval Demo: Policy Validation with Conditional Approval ===

Step 1: Registering task definitions...
  Registered: exp_validate_policy, exp_process

Step 2: Registering workflow 'expense_approval'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow (amount=50, category=office)...
  [exp_validate_policy] amount=50.0, category=office -> approvalRequired=false

  Workflow ID: b3f1a2c4-...

Step 5: Waiting for completion...
  [exp_process] Processing expense...
  [exp_process] Expense processed.
  Status: COMPLETED
  Output: {approvalRequired=false, processed=true}

Result: PASSED

```

## Using the Conductor CLI

You can use the [Conductor CLI](https://github.com/conductor-oss/conductor-cli) to register definitions, start workflows, and inspect executions. The CLI handles the Conductor server side; but **workers must still be running** to poll and execute tasks.

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/expense-approval-1.0.0.jar --workers

```

Then use the CLI in a separate terminal to start and manage workflows.

### Install the CLI

```bash
# macOS/Linux
brew tap conductor-oss/conductor && brew install conductor

# or via npm
npm install -g @conductor-oss/conductor-cli

# or direct download
curl -fsSL https://raw.githubusercontent.com/conductor-oss/conductor-cli/main/install.sh | sh

```

### Start Conductor locally

```bash
conductor server start
conductor server status

```

### Register tasks and workflow

```bash
# Register task definitions
conductor task create src/main/resources/task-defs.json

# Register the workflow
conductor workflow create src/main/resources/workflow.json

```

### Start a workflow run

Small expense (auto-approved, no WAIT):

```bash
conductor workflow start \
  --workflow expense_approval \
  --version 1 \
  --input '{"amount": 50, "category": "office", "submitter": "alice@example.com", "description": "Printer paper and toner"}'

```

Large expense (requires manager approval via WAIT):

```bash
conductor workflow start \
  --workflow expense_approval \
  --version 1 \
  --input '{"amount": 2500, "category": "conference", "submitter": "alice@example.com", "description": "AWS re:Invent registration and travel"}'

```

Travel expense (requires manager approval regardless of amount):

```bash
conductor workflow start \
  --workflow expense_approval \
  --version 1 \
  --input '{"amount": 75, "category": "travel", "submitter": "bob@example.com", "description": "Taxi to client site"}'

```

### Check workflow status

```bash
# Get status of a running workflow
conductor workflow status <workflow_id>

# Get full execution details including task inputs/outputs
conductor workflow get-execution <workflow_id> -c

# Search for recent runs
conductor workflow search -w expense_approval -s COMPLETED -c 5

```

### Completing the WAIT task (human approval)

When the workflow hits the `wait_for_approval` WAIT task, it pauses until an external signal completes the task. The workflow status will show as `RUNNING` with the WAIT task `IN_PROGRESS`.

**Step 1: Find the WAIT task ID**

```bash
# Get the execution details: look for the task named "wait_for_approval"
conductor workflow get-execution <workflow_id> -c

```

The task ID is in the `taskId` field of the `wait_for_approval` task.

**Step 2: Approve the expense**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<task_id>", "status": "COMPLETED", "outputData": {"approved": true, "approvedBy": "manager@example.com", "approvedAt": "2026-03-14T10:30:00Z"}}'

```

**Step 2 (alternative): Reject the expense**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<task_id>", "status": "FAILED", "reasonForIncompletion": "Expense rejected. Exceeds department budget", "outputData": {"approved": false, "rejectedBy": "manager@example.com", "reason": "Exceeds department budget for Q1"}}'

```

After approval, the workflow automatically resumes and the `exp_process` worker finalizes the expense.

### Debug a failed workflow

```bash
# Find failed runs
conductor workflow search -w expense_approval -s FAILED

# Inspect execution details
conductor workflow get-execution <workflow_id> -c

# Retry a failed workflow
conductor workflow retry <workflow_id>

# Restart from the beginning
conductor workflow restart <workflow_id>

```

### List registered definitions

```bash
conductor workflow list
conductor task list

```

## How to Extend

Each worker wraps a single expense operation. Plug in your policy engine (SAP Concur, Expensify, Brex) or ERP (SAP, NetSuite, QuickBooks) and the workflow runs identically without any changes.

**Example. Make `ValidatePolicyWorker` real with a policy engine:**

```java
// Before (simulated):
boolean needsApproval = amount > 100 || "travel".equals(category);

// After (real. Load rules from a database):
PolicyEngine engine = PolicyEngine.forDepartment(submitterDepartment);
boolean needsApproval = engine.evaluate(amount, category, submitter);
// Supports per-department thresholds, category-specific rules,
// and spending limits that update without code changes

```

Swap in a real policy engine and the expense approval pipeline continues without any workflow changes.

**Other production swaps:**
- `ValidatePolicyWorker` → SAP Concur policy API, Expensify rules engine, or Brex spend controls
- `ProcessWorker` → SAP ERP for GL posting, NetSuite for reimbursement, QuickBooks for small-business accounting
- `wait_for_approval` → trigger from a Slack approval bot (Slack sends the `POST /tasks/{taskId}` call when the manager clicks Approve), or from an email link that hits your approval API
- **Add a manager-lookup step** → insert a worker before the WAIT task that resolves the submitter's manager from your HRIS (Workday, BambooHR) and routes the approval notification to the right person

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
expense-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   ├── workflow.json                # Workflow definition (SWITCH + WAIT for approval)
│   └── task-defs.json               # Task definitions for CLI registration
├── src/main/java/expenseapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExpenseApprovalExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ValidatePolicyWorker.java # Policy check: amount > $100 or travel
│       └── ProcessWorker.java        # Expense finalization
└── src/test/java/expenseapproval/workers/
    ├── ValidatePolicyWorkerTest.java  # 10 tests. Boundary values, categories, types
    └── ProcessWorkerTest.java         # 4 tests. Completion, output shape

```
