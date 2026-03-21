# Budget Approval in Java with Conductor

Budget approval with SWITCH for approve/revise/reject decisions. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to route budget requests through an approval workflow. A department submits a budget request with amount and justification, a reviewer evaluates it, and the outcome is one of three paths: approve (release funds), revise (send back with feedback), or reject (deny with explanation). The routing decision depends on the reviewer's assessment of the amount, justification quality, and department priority.

Without orchestration, you'd build an approval service with if/else routing for approve/revise/reject, manually tracking request state through email chains, handling escalation when a reviewer is unavailable, and logging every decision to satisfy audit requirements for budget governance.

## The Solution

**You just write the budget workers. Request submission, review, and three-way routing for approve, revise, or reject decisions. Conductor handles three-way SWITCH routing for approve, revise, and reject decisions, automatic retries, and a complete audit trail for budget governance.**

Each approval concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of submitting the request, routing via a SWITCH task to the correct outcome (approve, revise, reject), retrying if the review system is unavailable, and tracking every budget request's lifecycle with full audit trail. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Six workers manage the approval lifecycle: SubmitBudgetWorker captures the request, ReviewBudgetWorker evaluates it, and SWITCH routes to ApproveBudgetWorker, ReviseBudgetWorker, or RejectBudgetWorker based on the review, with AllocateFundsWorker releasing approved funds.

| Worker | Task | What It Does |
|---|---|---|
| **AllocateFundsWorker** | `bgt_allocate_funds` | Allocates funds |
| **ApproveBudgetWorker** | `bgt_approve_budget` | Approves the budget |
| **RejectBudgetWorker** | `bgt_reject_budget` | Rejects the budget |
| **ReviewBudgetWorker** | `bgt_review_budget` | Reviews the budget |
| **ReviseBudgetWorker** | `bgt_revise_budget` | Revise Budget. Computes and returns revised, revised amount, revision id |
| **SubmitBudgetWorker** | `bgt_submit_budget` | Handles submit budget |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
bgt_submit_budget
    │
    ▼
bgt_review_budget
    │
    ▼
SWITCH (bgt_switch_ref)
    ├── approve: bgt_approve_budget
    ├── revise: bgt_revise_budget
    ├── reject: bgt_reject_budget
    │
    ▼
bgt_allocate_funds

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
java -jar target/budget-approval-1.0.0.jar

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
java -jar target/budget-approval-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow budget_approval_workflow \
  --version 1 \
  --input '{"budgetId": "TEST-001", "department": "engineering", "amount": 100, "justification": "sample-justification"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w budget_approval_workflow -s COMPLETED -c 5

```

## How to Extend

Connect SubmitBudgetWorker to your financial planning system, ReviewBudgetWorker to your approval routing rules, and AllocateFundsWorker to your GL and budgeting platform. The workflow definition stays exactly the same.

- **Budget submitter**: persist budget requests to your financial planning system (Adaptive Insights, Anaplan, SAP BPC) with validation rules
- **Reviewer**: route to the appropriate approver based on amount thresholds and department hierarchy; use a WAIT task for human approval
- **Approve handler**: release funds in your ERP system (SAP, Oracle, NetSuite) and update budget tracking
- **Revise/Reject handlers**: send feedback notifications to the requester with revision guidelines or rejection reasons

Connect each worker to your ERP budgeting module and approval engine while preserving the same output fields, and the approval workflow: including SWITCH routing, runs without changes.

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
budget-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/budgetapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BudgetApprovalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AllocateFundsWorker.java
│       ├── ApproveBudgetWorker.java
│       ├── RejectBudgetWorker.java
│       ├── ReviewBudgetWorker.java
│       ├── ReviseBudgetWorker.java
│       └── SubmitBudgetWorker.java
└── src/test/java/budgetapproval/workers/
    ├── AllocateFundsWorkerTest.java        # 3 tests
    └── ReviewBudgetWorkerTest.java        # 3 tests

```
