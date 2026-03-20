# RFP Automation in Java with Conductor :  RFP Creation, Vendor Distribution, Response Collection, Evaluation, and Vendor Selection

A Java Conductor workflow example for request-for-proposal automation .  creating an RFP for a project (e.g., "Cloud Infrastructure Migration" requiring scalability, security, and 24/7 support), distributing it to qualified vendors, collecting responses by deadline, evaluating proposals against requirements, and selecting the winning vendor. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to run a structured RFP process for a cloud infrastructure migration. The RFP must clearly specify requirements (scalability, security, 24/7 support) and a response deadline. It must be distributed to qualified vendors in your approved vendor list. Vendor responses must be collected and validated for completeness before the deadline. Each response must be evaluated against the stated requirements with consistent scoring. The vendor with the best overall score is selected, and the decision must be defensible in case of a protest.

Without orchestration, procurement creates RFPs in Word documents, emails them to vendors, and tracks responses in a spreadsheet. Some vendors never receive the RFP because the email bounced. Evaluation criteria are applied inconsistently because different reviewers use different weights. When a losing vendor asks why they weren't selected, there is no traceable scoring record linking their response to the evaluation criteria.

## The Solution

**You just write the RFP workers. Creation, vendor distribution, response collection, evaluation scoring, and vendor selection. Conductor handles vendor notification retries, consistent scoring sequencing, and traceable evaluation records for protest defense.**

Each phase of the RFP process is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so the RFP is finalized before distribution, all vendors are notified before collection begins, responses are complete before evaluation, and evaluation scores drive selection. If the distribution worker fails for one vendor, Conductor retries without re-sending to vendors already notified. Every RFP version, distribution receipt, response submission, evaluation score, and selection decision is recorded for procurement audit and vendor protest defense.

### What You Write: Workers

Five workers automate the RFP process: CreateWorker defines requirements and criteria, DistributeWorker sends to qualified vendors, CollectWorker gathers responses, EvaluateWorker scores proposals, and SelectWorker picks the winner.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWorker** | `rfp_collect` | Collects vendor responses and validates completeness before the deadline. |
| **CreateWorker** | `rfp_create` | Creates the RFP with project requirements, evaluation criteria, and response deadline. |
| **DistributeWorker** | `rfp_distribute` | Distributes the RFP to qualified vendors in the approved vendor list. |
| **EvaluateWorker** | `rfp_evaluate` | Scores each vendor response against the stated requirements with consistent criteria. |
| **SelectWorker** | `rfp_select` | Selects the winning vendor based on evaluation scores. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
rfp_create
    │
    ▼
rfp_distribute
    │
    ▼
rfp_collect
    │
    ▼
rfp_evaluate
    │
    ▼
rfp_select
```

## Example Output

```
=== Example 664: RFP Automatio ===

Step 1: Registering task definitions...
  Registered: rfp_create, rfp_distribute, rfp_collect, rfp_evaluate, rfp_select

Step 2: Registering workflow 'rfp_automation'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Received
  [create] RFP: \"" + task.getInputData().get("projectTitle") + "\"
  [distribute]
  [evaluate] Top candidate:
  [select] Selected

  Status: COMPLETED
  Output: {proposals=..., proposalCount=..., rfpId=..., distributedTo=...}

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
java -jar target/rfp-automation-1.0.0.jar
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
java -jar target/rfp-automation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rfp_automation \
  --version 1 \
  --input '{"projectTitle": "sample-projectTitle", "Cloud Infrastructure Migration": "sample-Cloud Infrastructure Migration", "requirements": "sample-requirements", "scalability": "sample-scalability", "security": "sample-security", "deadline": "sample-deadline"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rfp_automation -s COMPLETED -c 5
```

## How to Extend

Connect CreateWorker to your procurement portal, DistributeWorker to your supplier notification system, and EvaluateWorker to your scoring matrix or ML-based proposal analysis tool. The workflow definition stays exactly the same.

- **CreateWorker** (`rfp_create`): generate the RFP document from templates in your procurement platform (SAP Ariba, Jaggaer, Scout RFP), embedding project requirements and evaluation criteria
- **DistributeWorker** (`rfp_distribute`): send the RFP to vendors via your supplier portal, email API, or EDI, tracking delivery confirmations for each recipient
- **CollectWorker** (`rfp_collect`): poll the supplier portal for submitted responses, validate completeness against required sections, and lock submissions after the deadline
- **EvaluateWorker** (`rfp_evaluate`): score each response against weighted criteria (technical capability, cost, support coverage) using your evaluation matrix or AI-assisted scoring
- **SelectWorker** (`rfp_select`): rank vendors by composite score, generate the selection recommendation with justification, and notify the winning and losing vendors

Wire any worker to your procurement portal while keeping its return structure, and the evaluation pipeline runs without modification.

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
rfp-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/rfpautomation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RfpAutomationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWorker.java
│       ├── CreateWorker.java
│       ├── DistributeWorker.java
│       ├── EvaluateWorker.java
│       └── SelectWorker.java
└── src/test/java/rfpautomation/workers/
    ├── CollectWorkerTest.java        # 2 tests
    ├── CreateWorkerTest.java        # 2 tests
    ├── DistributeWorkerTest.java        # 2 tests
    ├── EvaluateWorkerTest.java        # 2 tests
    └── SelectWorkerTest.java        # 2 tests
```
