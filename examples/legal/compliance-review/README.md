# Compliance Review in Java with Conductor

A Java Conductor workflow example demonstrating Compliance Review. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

A regulatory audit is approaching. You need to identify the applicable compliance requirements (e.g., 45 controls), assess your organization's current posture against each one, perform a gap analysis to find the 7 unmet controls (like missing encryption), and create a remediation plan to close those gaps before the deadline. Failing to identify a critical gap can result in regulatory fines, consent orders, or loss of operating licenses.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the requirement identification, gap analysis, remediation planning, and compliance certification logic. Conductor handles assessment retries, gap analysis sequencing, and compliance audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Regulatory identification, compliance assessment, gap analysis, and remediation planning workers each tackle one phase of the compliance review lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `cmr_identify` | Identifies applicable regulatory requirements (45 controls) for the entity under review and maps them to the relevant compliance framework |
| **AssessWorker** | `cmr_assess` | Evaluates current compliance posture, scoring 84 out of 100 with 38 controls met and 7 not met, and flags critical gaps like missing encryption |
| **GapAnalysisWorker** | `cmr_gap_analysis` | Analyzes the 7 unmet controls in detail, categorizing each gap by severity (e.g., encryption flagged as critical) and producing a prioritized gap report |
| **RemediateWorker** | `cmr_remediate` | Creates a remediation plan (REM-695) with specific action items to close each identified gap before the audit deadline |

Workers simulate legal operations .  document review, compliance checks, approval routing ,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
cmr_identify
    │
    ▼
cmr_assess
    │
    ▼
cmr_gap_analysis
    │
    ▼
cmr_remediate
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
java -jar target/compliance-review-1.0.0.jar
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
java -jar target/compliance-review-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cmr_compliance_review \
  --version 1 \
  --input '{"regulationType": "test-value", "entityId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cmr_compliance_review -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real compliance tools .  your regulatory database for requirement lookups, your document management system for policy reviews, your GRC platform for audit reporting, and the workflow runs identically in production.

- **IdentifyWorker** (`cmr_identify`): integrate with a GRC (Governance, Risk, Compliance) platform like ServiceNow GRC, OneTrust, or LogicGate to pull applicable regulatory frameworks and control catalogs
- **AssessWorker** (`cmr_assess`): connect to automated compliance scanning tools like Vanta, Drata, or AWS Audit Manager to gather real-time control evidence and posture scores
- **GapAnalysisWorker** (`cmr_gap_analysis`): query your security tooling (Qualys, Tenable, CrowdStrike) to validate control effectiveness and map findings to specific regulatory gaps
- **RemediateWorker** (`cmr_remediate`): create remediation tickets in Jira or ServiceNow, assign owners, set SLAs, and trigger automated fixes where possible (e.g., enabling encryption via cloud provider APIs)

Update regulatory frameworks or assessment criteria and the review pipeline adapts without structural changes.

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
compliance-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/compliancereview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplianceReviewExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── GapAnalysisWorker.java
│       ├── IdentifyWorker.java
│       └── RemediateWorker.java
└── src/test/java/compliancereview/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── GapAnalysisWorkerTest.java        # 2 tests
    ├── IdentifyWorkerTest.java        # 2 tests
    └── RemediateWorkerTest.java        # 2 tests
```
