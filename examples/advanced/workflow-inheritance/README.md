# Workflow Inheritance in Java Using Conductor :  Base Workflow with Init, Validate, Process, Finalize

A Java Conductor workflow example for workflow inheritance .  defining a base workflow pattern (init, validate, process, finalize) that can be specialized for different processing tiers. The standard variant processes requests with standard logic, while other variants (premium, enterprise) can override the processing step while keeping the same init/validate/finalize structure. Uses [Conductor](https://github.

## Different Customer Tiers Need Different Processing, Same Structure

Every customer request follows the same lifecycle: initialize the processing context, validate the input, process the request, and finalize (cleanup, notifications). But premium customers get faster processing with dedicated resources, enterprise customers get custom transformation logic, and standard customers get the default path. Without inheritance, you'd duplicate the entire workflow for each tier, copy-pasting the init/validate/finalize steps and only changing the processing step.

Workflow inheritance lets you define the common structure once (init-validate-process-finalize) and override just the processing step per tier. This is the template method pattern applied to workflows .  same skeleton, different specializations.

## The Solution

**You write the tier-specific processing step. Conductor handles the shared lifecycle, retries, and variant tracking.**

`WiInitWorker` initializes the processing context for the request. `WiValidateWorker` validates the input against business rules. `WiProcessStandardWorker` (in this variant) processes the request with standard logic .  other workflow variants can swap this step for `WiProcessPremiumWorker` or a custom processor. `WiFinalizeWorker` handles cleanup, notifications, and status updates. Conductor runs the same init-validate-finalize structure for every tier, and each execution records which processing variant was used.

### What You Write: Workers

Five workers implement the template method pattern: shared init, validation, and finalization steps plus two tier-specific processors (standard and premium), specializing only the processing stage per customer tier.

| Worker | Task | What It Does |
|---|---|---|
| **WiFinalizeWorker** | `wi_finalize` | Completes the request by finalizing the processing result regardless of variant |
| **WiInitWorker** | `wi_init` | Initializes the request by ID and determines the processing variant (standard/premium) |
| **WiProcessPremiumWorker** | `wi_process_premium` | Processes the request using the premium path with a 2-hour SLA |
| **WiProcessStandardWorker** | `wi_process_standard` | Processes the request using the standard path with a 24-hour SLA |
| **WiValidateWorker** | `wi_validate` | Validates the input data before routing to the variant-specific processor |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
wi_init
    │
    ▼
wi_validate
    │
    ▼
wi_process_standard
    │
    ▼
wi_finalize

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
java -jar target/workflow-inheritance-1.0.0.jar

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
java -jar target/workflow-inheritance-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow workflow_inheritance_standard_demo \
  --version 1 \
  --input '{"requestId": "TEST-001", "data": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_inheritance_standard_demo -s COMPLETED -c 5

```

## How to Extend

Each worker implements one lifecycle phase .  replace the simulated tier-specific processing with real premium or enterprise service integrations and the init-validate-process-finalize skeleton runs unchanged.

- **WiProcessStandardWorker** (`wi_process_standard`): implement real tier-specific processing: standard database queries tier, dedicated Redis cache lookups for premium, or custom ML inference for enterprise
- **WiValidateWorker** (`wi_validate`): run real input validation: JSON Schema validation, business rule engines (Drools), or tier-specific validation rules from a config database
- **WiFinalizeWorker** (`wi_finalize`): perform real cleanup: close database connections, send completion notifications via SES/Slack, update request status in your tracking system

The init-validate-finalize skeleton stays fixed. Adding a new processing tier means adding one worker variant, not duplicating the shared lifecycle steps.

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
workflow-inheritance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowinheritance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowInheritanceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WiFinalizeWorker.java
│       ├── WiInitWorker.java
│       ├── WiProcessPremiumWorker.java
│       ├── WiProcessStandardWorker.java
│       └── WiValidateWorker.java
└── src/test/java/workflowinheritance/workers/
    ├── WiFinalizeWorkerTest.java        # 4 tests
    ├── WiInitWorkerTest.java        # 4 tests
    ├── WiProcessPremiumWorkerTest.java        # 4 tests
    ├── WiProcessStandardWorkerTest.java        # 4 tests
    └── WiValidateWorkerTest.java        # 4 tests

```
