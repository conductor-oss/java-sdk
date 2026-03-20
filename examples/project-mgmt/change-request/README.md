# Change Request Management in Java with Conductor :  Submit, Impact Assessment, Approval, Implementation, and Verification

A Java Conductor workflow example for managing project change requests end-to-end .  from initial submission through impact assessment, approval gate, implementation, and post-change verification. Uses [Conductor](https://github.## The Problem

You need to manage change requests across your project. When someone submits a change .  "swap the payment provider," "extend the timeline by two weeks," "add a new integration requirement" ,  the request must go through a controlled process: log the change formally, assess its impact on scope, timeline, and budget, get approval from the change control board, implement the approved change, and verify the result. Each step depends on the previous one, and you need a complete audit trail for compliance.

Without orchestration, change management devolves into email threads and spreadsheets. Impact assessments get lost, approvals happen out of order, implementations proceed without sign-off, and nobody can reconstruct the timeline when an audit asks "who approved this scope change and when?" Building this as a monolithic script means any failure in the approval step silently skips verification.

## The Solution

**You just write the change submission, impact assessment, approval gating, implementation, and verification logic. Conductor handles impact analysis retries, approval routing, and change control audit trails.**

Each step in the change request lifecycle is a simple, independent worker .  one submits and logs the request, one assesses impact, one handles the approval gate, one implements the change, one verifies the outcome. Conductor takes care of executing them in strict sequence, ensuring no step is skipped, retrying if an external system is temporarily down, and maintaining a complete execution history that serves as your audit trail. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Request intake, impact analysis, approval routing, and implementation tracking workers each manage one stage of the change control process.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `chr_submit` | Logs the change request with a unique ID, captures description, requester, and affected areas |
| **AssessImpactWorker** | `chr_assess_impact` | Evaluates the change's impact on scope, timeline, budget, and resource allocation |
| **ApproveWorker** | `chr_approve` | Processes the approval decision based on impact assessment (approve, reject, or request revision) |
| **ImplementWorker** | `chr_implement` | Executes the approved change .  updates project plans, reassigns resources, adjusts milestones |
| **VerifyWorker** | `chr_verify` | Confirms the change was implemented correctly and project artifacts are consistent |

Workers simulate project management operations .  task creation, status updates, notifications ,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
chr_submit
    │
    ▼
chr_assess_impact
    │
    ▼
chr_approve
    │
    ▼
chr_implement
    │
    ▼
chr_verify
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
java -jar target/change-request-1.0.0.jar
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
java -jar target/change-request-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow change_request_change-request \
  --version 1 \
  --input '{"changeId": "CR-101", "CR-101": "description", "description": "Add mobile support to dashboard", "Add mobile support to dashboard": "requestedBy", "requestedBy": "Product Manager", "Product Manager": "sample-Product Manager"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w change_request_change-request -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real change management systems .  your PM tool for request logging, your impact analysis framework, your approval platform for CCB sign-off, and the workflow runs identically in production.

- **SubmitWorker** (`chr_submit`): create a ticket in Jira or ServiceNow with the change request details, attach supporting documents via their APIs
- **AssessImpactWorker** (`chr_assess_impact`): query your project schedule (MS Project, Smartsheet) to calculate timeline impact, pull budget data to estimate cost delta
- **ApproveWorker** (`chr_approve`): integrate with a HUMAN task or approval workflow (Slack interactive message, email with approve/reject links) for real change control board sign-off
- **ImplementWorker** (`chr_implement`): push updates to Jira epics, update Confluence project plans, trigger CI/CD pipelines for technical changes
- **VerifyWorker** (`chr_verify`): run automated checks against the updated project plan, validate budget reconciliation, and send a confirmation report

Update impact analysis criteria or approval chains and the change pipeline adjusts transparently.

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
change-request-change-request/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/changerequest/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChangeRequestExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── AssessImpactWorker.java
│       ├── ImplementWorker.java
│       ├── SubmitWorker.java
│       └── VerifyWorker.java
└── src/test/java/changerequest/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── AssessImpactWorkerTest.java        # 2 tests
    ├── ImplementWorkerTest.java        # 2 tests
    ├── SubmitWorkerTest.java        # 2 tests
    └── VerifyWorkerTest.java        # 2 tests
```
