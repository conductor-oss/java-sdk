# Government Case Management in Java with Conductor :  Intake, Investigation, Evaluation, and Resolution

A Java Conductor workflow example for government case management .  opening cases from citizen reports, conducting investigations, evaluating findings, rendering decisions, and closing cases with a full audit trail. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage government cases from intake through resolution. A citizen or agency files a report, which opens a case. An investigator gathers evidence and interviews witnesses. An evaluator reviews the findings and assesses severity. A decision-maker renders a formal determination based on the evaluation. Finally, the case is closed with a timestamp and resolution record. Each step depends on the output of the previous one .  you cannot evaluate without investigation findings, and you cannot decide without an evaluation.

Without orchestration, you'd build a monolithic service that tracks case state in a database, calls each downstream service in sequence, and handles failures inline. If the investigation service is slow or unavailable, you'd need retry logic and timeout handling. If the system crashes after investigation but before evaluation, you'd need to figure out where to resume. Regulators and FOIA requests demand a complete record of every case action, timing, and decision.

## The Solution

**You just write the case intake, investigation, findings evaluation, decision rendering, and case closure logic. Conductor handles assignment retries, investigation sequencing, and case lifecycle audit trails.**

Each stage of the case lifecycle is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of running them in sequence, passing investigation findings to the evaluator, feeding the evaluation into the decision step, retrying if any service is temporarily unavailable, and maintaining a complete audit trail of every case from open to close. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Case creation, assignment, investigation, and resolution workers track government cases through a structured lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **OpenCaseWorker** | `cmg_open_case` | Creates a new case record from the reporter's complaint, assigns a case ID and case type |
| **InvestigateWorker** | `cmg_investigate` | Gathers evidence, interviews witnesses, and documents findings with severity assessment |
| **EvaluateWorker** | `cmg_evaluate` | Reviews investigation findings and produces a formal evaluation with recommendations |
| **DecideWorker** | `cmg_decide` | Renders a formal decision (approve, deny, refer) based on the evaluation |
| **CloseWorker** | `cmg_close` | Closes the case with a resolution record and timestamp |

Workers simulate government operations .  application processing, compliance checks, notifications ,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cmg_open_case
    │
    ▼
cmg_investigate
    │
    ▼
cmg_evaluate
    │
    ▼
cmg_decide
    │
    ▼
cmg_close
```

## Example Output

```
=== Example 522: Case Management (Gov) ===

Step 1: Registering task definitions...
  Registered: cmg_open_case, cmg_investigate, cmg_evaluate, cmg_decide, cmg_close

Step 2: Registering workflow 'cmg_case_management_gov'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [close] Processing
  [decide] Processing
  [evaluate] Findings evaluated .  moderate severity
  [investigate] Processing
  [open_case] Processing

  Status: COMPLETED
  Output: {closedAt=..., archived=..., decision=..., actionPlan=...}

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
java -jar target/case-management-gov-1.0.0.jar
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
java -jar target/case-management-gov-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cmg_case_management_gov \
  --version 1 \
  --input '{"caseType": "regulatory-violation", "regulatory-violation": "reporterId", "reporterId": "INSP-10", "INSP-10": "description", "description": "Non-compliant waste disposal", "Non-compliant waste disposal": "sample-Non-compliant waste disposal"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cmg_case_management_gov -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real case systems .  your case management platform for intake, your investigation tools for evidence gathering, your adjudication system for resolution decisions, and the workflow runs identically in production.

- **OpenCaseWorker** → integrate with your agency's case management database to create real case records with auto-generated case numbers
- **InvestigateWorker** → connect to your evidence management system and witness interview scheduling tools
- **EvaluateWorker** → plug in a scoring engine or risk assessment model that weighs evidence factors against agency guidelines
- **DecideWorker** → integrate with your adjudication system and generate formal determination letters
- Add a **NotifyReporterWorker** after case closure to send the complainant a resolution summary
- Add a **EscalateWorker** with a SWITCH on severity to route high-severity cases to senior reviewers

Swap assignment algorithms or tracking systems and the case pipeline keeps its structure.

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
case-management-gov-case-management-gov/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/casemanagementgov/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CaseManagementGovExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseWorker.java
│       ├── DecideWorker.java
│       ├── EvaluateWorker.java
│       ├── InvestigateWorker.java
│       └── OpenCaseWorker.java
└── src/test/java/casemanagementgov/workers/
    ├── DecideWorkerTest.java
    └── OpenCaseWorkerTest.java
```
