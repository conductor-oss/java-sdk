# Quality Inspection in Java with Conductor :  Sampling, Testing, Pass/Fail Routing, and Results Recording

A Java Conductor workflow example for quality inspection. pulling samples from a production batch, running standardized tests against acceptance criteria, routing to pass or fail handlers based on test results, and recording the inspection outcome for compliance and traceability. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to inspect production batches before they ship. A sample must be pulled from the batch according to AQL (Acceptable Quality Level) sampling tables. The sample undergoes standardized tests. dimensional measurements, material composition, functional testing. Based on results, the batch is either accepted for shipment (pass) or quarantined for rework/scrap (fail). Regardless of outcome, the inspection results must be recorded for traceability, regulatory compliance (ISO 9001, FDA 21 CFR Part 820), and supplier quality scorecards.

Without orchestration, inspectors fill out paper forms, test results live in a disconnected lab system, and pass/fail decisions are communicated verbally. If a batch fails but the fail handler doesn't trigger. the batch ships anyway. When a customer reports a defect, tracing back to the inspection record requires searching through filing cabinets. There is no automated linkage between test results and the disposition decision.

## The Solution

**You just write the inspection workers. Sampling, testing, pass/fail routing, and results recording. Conductor handles SWITCH-based pass/fail routing, test retries, and timestamped inspection records for ISO 9001 compliance.**

Each step of the quality inspection process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so samples are pulled before testing, test results drive the pass/fail routing decision (via a SWITCH task), and results are recorded regardless of which path was taken. If the test worker fails mid-measurement, Conductor retries without re-pulling the sample. Every sample selection, test measurement, disposition decision, and recording is tracked with timestamps for full batch traceability.

### What You Write: Workers

Four workers drive the inspection process: SampleWorker pulls items per AQL tables and runs tests, HandlePassWorker approves the batch, HandleFailWorker quarantines it, and RecordWorker logs results for traceability.

| Worker | Task | What It Does |
|---|---|---|
| **HandleFailWorker** | `qi_handle_fail` | Quarantines the batch for rework or scrap when inspection fails. |
| **HandlePassWorker** | `qi_handle_pass` | Approves the batch for shipment when inspection passes. |
| **RecordWorker** | `qi_record` | Records inspection results for traceability, regulatory compliance, and supplier scorecards. |
| **SampleWorker** | `qi_sample` | Pulls samples from the production batch according to AQL sampling tables and runs standardized tests. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
qi_sample
    │
    ▼
qi_test
    │
    ▼
SWITCH (qi_switch_ref)
    ├── pass: qi_handle_pass
    └── default: qi_handle_fail
    │
    ▼
qi_record

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
java -jar target/quality-inspection-1.0.0.jar

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
java -jar target/quality-inspection-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow qi_quality_inspection \
  --version 1 \
  --input '{"batchId": "TEST-001", "product": "widget-pro", "sampleSize": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w qi_quality_inspection -s COMPLETED -c 5

```

## How to Extend

Connect SampleWorker to your lab LIMS for AQL sampling, the test workers to your measurement instruments, and RecordWorker to your QMS for ISO 9001 traceability. The workflow definition stays exactly the same.

- **SampleWorker** (`qi_sample`): pull samples per AQL tables from your MES (Manufacturing Execution System) or WMS, logging sample IDs and batch traceability data
- **TestWorker** (`qi_test`): record test measurements from lab instruments (calipers, spectrometers, functional test rigs) via LIMS integration or manual data entry API
- **HandlePassWorker** (`qi_handle_pass`): release the batch for shipment in your ERP/WMS, update the quality status, and notify the shipping team
- **HandleFailWorker** (`qi_handle_fail`): quarantine the batch in your WMS, create a non-conformance report (NCR) in your QMS (ETQ, MasterControl), and notify the supplier quality engineer
- **RecordWorker** (`qi_record`): store the complete inspection record (sample data, test results, disposition) in your QMS for regulatory audits and supplier scorecards

Swap any worker for a real lab system or QMS integration while preserving its output schema, and the inspection pipeline operates unchanged.

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
quality-inspection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/qualityinspection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── QualityInspectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HandleFailWorker.java
│       ├── HandlePassWorker.java
│       ├── RecordWorker.java
│       └── SampleWorker.java
└── src/test/java/qualityinspection/workers/
    ├── HandleFailWorkerTest.java        # 2 tests
    ├── HandlePassWorkerTest.java        # 2 tests
    ├── RecordWorkerTest.java        # 2 tests
    ├── SampleWorkerTest.java        # 2 tests
    └── TestWorkerTest.java        # 2 tests

```
