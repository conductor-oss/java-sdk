# Change Management in Java with Conductor :  Submit, Assess Risk, Approve, Implement

Automates ITIL-style change management using [Conductor](https://github.com/conductor-oss/conductor). This workflow submits a change request with a tracking ID, assesses the risk level (low/medium/high), routes through Change Advisory Board (CAB) approval, and implements the approved change.

## Controlled Changes, Not Cowboy Deploys

An engineer wants to modify the production database connection pool settings. Without a process, they SSH in and change it. With change management, the request is submitted and tracked, risk is assessed (is this a low-risk config change or a high-risk schema migration?), CAB approval is obtained for anything above low risk, and only then is the change implemented. If the approval is denied or the risk assessment flags concerns, the workflow stops cleanly.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the risk assessment and approval logic. Conductor handles the submit-assess-approve-implement pipeline and the full change audit trail.**

`SubmitChangeWorker` creates the change request with description, justification, affected systems, implementation plan, and rollback procedure. `AssessRiskWorker` evaluates the change's risk level based on affected systems, blast radius, change complexity, and historical failure rates for similar changes. `ApproveChangeWorker` routes the change through the approval process .  auto-approving low-risk changes or queuing for CAB review. `ImplementChangeWorker` executes the approved change with monitoring and records the outcome. Conductor tracks the full change lifecycle for compliance auditing.

### What You Write: Workers

Four workers implement the change process. Submitting the request, assessing risk, routing through CAB approval, and implementing the change.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveChange** | `cm_approve` | Processes CAB (Change Advisory Board) approval for a change. |
| **AssessRisk** | `cm_assess_risk` | Assesses risk level for a submitted change request. |
| **ImplementChange** | `cm_implement` | Implements the approved change. |
| **SubmitChange** | `cm_submit` | Submits a change request and generates a tracking ID. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
Input -> ApproveChange -> AssessRisk -> ImplementChange -> SubmitChange -> Output

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
java -jar target/change-management-1.0.0.jar

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
java -jar target/change-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow change_management \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w change_management -s COMPLETED -c 5

```

## How to Extend

Each worker handles one ITIL change stage .  replace the simulated calls with ServiceNow, Jira Service Management, or Ansible for real ticketing, approval gates, and change execution, and the management workflow runs unchanged.

- **SubmitChange** (`cm_submit_change`): create change records in ServiceNow, Jira Service Management, or a custom change tracking system with description, risk category, and affected services
- **AssessRisk** (`cm_assess_risk`): use historical change data from ServiceNow or Jira to score risk based on similar past changes and their success/failure rates
- **ApproveChange** (`cm_approve_change`): integrate with ServiceNow Change Management, Jira Service Management, or PagerDuty for real approval workflows with SLA tracking
- **ImplementChange** (`cm_implement_change`): execute changes via Ansible playbooks, Terraform applies, or Kubernetes manifests with automatic rollback on health check failure

Integrate with ServiceNow or Jira for real change tracking; the approval pipeline keeps the same interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
change-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/changemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChangeManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveChange.java
│       ├── AssessRisk.java
│       ├── ImplementChange.java
│       └── SubmitChange.java
└── src/test/java/changemanagement/workers/
    ├── ApproveChangeTest.java        # 8 tests
    ├── AssessRiskTest.java        # 8 tests
    ├── ImplementChangeTest.java        # 8 tests
    └── SubmitChangeTest.java        # 9 tests

```
