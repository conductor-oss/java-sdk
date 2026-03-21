# Workflow Versioning in Java with Conductor

Run multiple versions of the same workflow side by side. version 1 does calculate-then-audit, version 2 adds a bonus step between them. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to evolve a workflow without breaking existing executions. Version 1 does a calculation (value * 2) then an audit. Version 2 adds a bonus step (+10) between calculation and audit. Both versions must coexist. In-flight v1 executions continue on v1, while new executions can use v2. You need to start workflows against a specific version and compare their outputs.

Without versioning, changing a workflow definition affects all executions immediately. Conductor's versioning lets you deploy v2 while v1 is still running, roll back to v1 if v2 has issues, and compare outputs between versions.

## The Solution

**You just write the calculation, bonus, and audit workers. Conductor handles running multiple workflow versions side by side with independent execution histories.**

This example registers two versions of the same workflow and runs them side by side. Version 1 runs `ver_calc` (multiplies the input value by 2) then `ver_audit` (marks the result as audited). Version 2 inserts a `ver_bonus` step between them that adds 10 to the calculated result before auditing. The example code registers both workflow definitions, starts executions against each version explicitly, and compares their outputs. V1 produces `value * 2`, v2 produces `(value * 2) + 10`. In-flight v1 executions continue running on v1 even after v2 is deployed. You can roll back to v1 at any time or run A/B comparisons between versions.

### What You Write: Workers

Three workers support side-by-side workflow versioning: VerCalcWorker performs a calculation, VerBonusWorker adds a bonus (used only in v2), and VerAuditWorker records the final result, the same worker pool serves both workflow versions simultaneously.

| Worker | Task | What It Does |
|---|---|---|
| **VerAuditWorker** | `ver_audit` | Audit worker for the versioned workflow. Takes the final result, marks it as audited, and passes it through. |
| **VerBonusWorker** | `ver_bonus` | Bonus worker for the versioned workflow. Takes a base result and adds 10. |
| **VerCalcWorker** | `ver_calc` | Calculation worker for the versioned workflow. Takes a numeric value and returns value * 2. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
Input -> VerAuditWorker -> VerBonusWorker -> VerCalcWorker -> Output

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
java -jar target/workflow-versioning-1.0.0.jar

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
java -jar target/workflow-versioning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow workflow_versioning \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_versioning -s COMPLETED -c 5

```

## How to Extend

Replace the arithmetic workers with your real pricing engine, loyalty service, and compliance audit system, and the side-by-side versioning works unchanged.

- **VerAuditWorker** (`ver_audit`): write an audit record to your compliance database or event log, capturing the final result, timestamp, and workflow version for traceability
- **VerBonusWorker** (`ver_bonus`): call your incentive or loyalty service to compute the actual bonus amount based on customer tier, campaign rules, or promotional offers
- **VerCalcWorker** (`ver_calc`): perform the real computation against your pricing engine, scoring model, or analytics service instead of the simple multiply-by-two formula

Replacing the arithmetic workers with real pricing, loyalty, or compliance logic and deploying new workflow versions does not affect existing in-flight executions, since Conductor runs each version independently with its own execution history.

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
workflow-versioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowversioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowVersioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── VerAuditWorker.java
│       ├── VerBonusWorker.java
│       └── VerCalcWorker.java
└── src/test/java/workflowversioning/workers/
    ├── VerAuditWorkerTest.java        # 5 tests
    ├── VerBonusWorkerTest.java        # 5 tests
    └── VerCalcWorkerTest.java        # 6 tests

```
