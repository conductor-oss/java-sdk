# Travel Request Approval in Java with Conductor

An engineer submits a $4,000 conference trip request. Their manager approves it by replying "looks good" to an email -- but the reply lands in a spam filter. The engineer assumes it's approved (no rejection came back either), books the flights and hotel on their corporate card, and flies to Austin. Finance catches it during month-end reconciliation: no approval on file, $4,000 expensed against a cost center that already blew its Q3 travel budget. Now the manager has to retroactively justify it, the engineer gets flagged for policy violation, and nobody is sure whether the reply-based approval system ever worked. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate approval routing as independent workers -- you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to approve employee travel requests with different approval paths based on estimated cost. An employee submits a travel request with destination, purpose, and estimated cost. The workflow estimates the total trip cost (flights, hotel, per diem). If the estimate falls below the auto-approval threshold ($1,000), the request is approved instantly. If it exceeds the threshold, the request routes to the employee's manager for manual review.

Without orchestration, you'd hardcode the approval threshold in the submission handler, mix cost estimation logic with approval routing, and lose visibility into which requests were auto-approved vs. manager-approved. Changing the threshold or adding a VP-level approval tier means rewriting the entire flow. A SWITCH task cleanly separates the routing decision from the approval logic.

## The Solution

**You just write the request submission, cost estimation, and approval routing logic. Conductor handles policy check retries, approval routing, and authorization audit trails.**

SubmitWorker captures the travel request details (employee, destination, purpose, dates). EstimateWorker calculates the total estimated cost and determines the approval type -- auto-approve for requests under the threshold, manager review for higher amounts. A SWITCH task routes based on the approval type: AutoApproveWorker instantly approves compliant requests, while ManagerApproveWorker routes to the manager for review and records who approved it. Each worker is a standalone Java class -- Conductor handles the routing, retries, and execution tracking.

### What You Write: Workers

Request submission, policy check, manager review, and approval notification workers each handle one gate in the corporate travel authorization process.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **SubmitWorker** | `tva_submit` | Captures travel request details (employee ID, destination) and returns a unique request ID | Simulated |
| **EstimateWorker** | `tva_estimate` | Evaluates the estimated cost against the $1,000 policy threshold and returns the approval type (`auto_approve` or `manager`) and total estimate | Simulated |
| **AutoApproveWorker** | `tva_auto_approve` | Marks requests under the threshold as approved with method `auto` | Simulated |
| **ManagerApproveWorker** | `tva_manager_approve` | Routes requests over the threshold to a manager for review and records the approver identity | Simulated |

Workers simulate travel operations -- booking, approval, itinerary generation -- with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
tva_submit
    │
    ▼
tva_estimate
    │
    ▼
SWITCH (tva_switch_ref)
    ├── auto_approve: tva_auto_approve
    ├── manager: tva_manager_approve
```

## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/travel-approval-1.0.0.jar
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
java -jar target/travel-approval-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tva_travel_approval \
  --version 1 \
  --input '{
    "employeeId": "EMP-4521",
    "destination": "San Francisco",
    "purpose": "Q3 product launch planning",
    "estimatedCost": 2500
  }'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tva_travel_approval -s COMPLETED -c 5
```

## Example Output

```
=== Travel Request Approval ===

Step 1: Registering task definitions...
  Registered: tva_submit, tva_estimate, tva_auto_approve, tva_manager_approve

Step 2: Registering workflow 'tva_travel_approval'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  [submit] Travel request: EMP-800 to New York
  [estimate] Cost: $2500 -- route: manager
  [manager] Request TVA-1001 approved by manager ($2500)

  Status: COMPLETED
  Request ID: TVA-1001
  Approval type: manager

Result: PASSED
```

## How to Extend

Connect each worker to your real travel systems -- your cost estimation service for trip pricing, your HR directory for manager lookup, Slack or email for approval notifications -- and the workflow runs identically in production.

- **SubmitWorker** (`tva_submit`) -- create the travel request in your travel management system (SAP Concur, Navan/TripActions) and return the request ID
- **EstimateWorker** (`tva_estimate`) -- query flight and hotel APIs (Google Flights, Amadeus) to compute the total estimated cost, then apply your corporate policy threshold rules
- **AutoApproveWorker** (`tva_auto_approve`) -- mark the request as approved in your TMS, update the budget allocation, and notify the employee via Slack or email that they can proceed with booking
- **ManagerApproveWorker** (`tva_manager_approve`) -- send an approval request to the manager via a Slack approval bot or email, wait for their response (using a HUMAN task or callback), and record the approval decision with the approver's identity
- Add a **VP approval tier** -- add a second threshold (e.g., $10,000) in EstimateWorker, add a new SWITCH case, and create a VpApproveWorker
- Add a **corporate policy engine** -- integrate with your company's travel policy service to dynamically determine approval thresholds by department, role, or destination

Change policy rules or approval hierarchies and the approval pipeline adapts transparently.

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
travel-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/travelapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TravelApprovalExample.java   # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AutoApproveWorker.java   # Auto-approves requests under threshold
│       ├── EstimateWorker.java      # Evaluates cost against policy threshold
│       ├── ManagerApproveWorker.java # Routes to manager for review
│       └── SubmitWorker.java        # Captures travel request details
└── src/test/java/travelapproval/workers/
    ├── AutoApproveWorkerTest.java   # 4 tests
    ├── EstimateWorkerTest.java      # 5 tests
    ├── ManagerApproveWorkerTest.java # 4 tests
    └── SubmitWorkerTest.java        # 4 tests
```
