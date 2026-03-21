# Lab Results Processing in Java Using Conductor :  Sample Collection, Processing, Analysis, Reporting, and Physician Notification

A Java Conductor workflow example for laboratory results processing. collecting patient samples, processing specimens through the lab, running analyses to produce test results, generating the lab report with reference ranges and interpretations, and notifying the ordering physician. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to manage the lifecycle of a lab order from sample collection through physician notification. A lab order comes in with a patient ID, order ID, and test type (CBC, BMP, lipid panel, etc.). The sample must be collected and accessioned with a barcode linking it to the order. The specimen is processed (centrifuged, aliquoted, loaded onto the analyzer). The analyzer runs the test and produces raw values. Those results must be formatted into a report with reference ranges, abnormal flags, and critical value alerts. Finally, the ordering physician must be notified. immediately for critical values, routinely for normal results. A lost sample or unreported critical value can delay diagnosis or endanger the patient.

Without orchestration, you'd build a monolithic LIS (Laboratory Information System) integration that tracks the sample through each stage, polls the analyzer for results, formats the report, and sends notifications. If the analyzer interface is down, you'd need retry logic. If the system crashes after analysis but before reporting, results sit unreported. CAP and CLIA accreditation require a complete chain of custody from sample receipt to result delivery.

## The Solution

**You just write the lab pipeline workers. Sample accessioning, specimen processing, test analysis, report generation, and physician notification. Conductor handles pipeline sequencing, automatic retries when an analyzer interface is temporarily offline, and a complete chain of custody for CAP/CLIA compliance.**

Each stage of the lab pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of processing only after collection, analyzing only after processing, generating the report only after analysis completes, notifying the physician after the report is finalized, and maintaining a complete chain of custody for CAP/CLIA compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the lab pipeline: CollectSampleWorker accessions specimens, ProcessSampleWorker prepares them for analysis, AnalyzeSampleWorker runs the ordered tests, LabReportWorker generates results with reference ranges, and LabNotifyWorker alerts the ordering physician.

| Worker | Task | What It Does |
|---|---|---|
| **CollectSampleWorker** | `lab_collect_sample` | Accessions the sample with a barcode, records collection time and specimen type |
| **ProcessSampleWorker** | `lab_process` | Processes the specimen. centrifugation, aliquoting, and loading onto the appropriate analyzer |
| **AnalyzeSampleWorker** | `lab_analyze` | Runs the ordered test on the analyzer and produces raw result values |
| **LabReportWorker** | `lab_report` | Generates the lab report with results, reference ranges, abnormal flags, and critical value alerts |
| **LabNotifyWorker** | `lab_notify` | Sends the results to the ordering physician. immediate notification for critical values, routine for normal |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
lab_collect_sample
    │
    ▼
lab_process
    │
    ▼
lab_analyze
    │
    ▼
lab_report
    │
    ▼
lab_notify

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
java -jar target/lab-results-1.0.0.jar

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
java -jar target/lab-results-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lab_results_workflow \
  --version 1 \
  --input '{"orderId": "TEST-001", "patientId": "TEST-001", "testType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lab_results_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectSampleWorker to your LIS accessioning system, AnalyzeSampleWorker to your analyzer middleware (Data Innovations), and LabNotifyWorker to HL7v2 ORU messaging for result delivery. The workflow definition stays exactly the same.

- **CollectSampleWorker** → integrate with your LIS accessioning module and barcode scanning system for real specimen tracking
- **ProcessSampleWorker** → connect to your lab automation track (Beckman, Roche, Siemens) for automated specimen routing
- **AnalyzeSampleWorker** → pull real results from your analyzer middleware (Data Innovations, Instrument Manager) via HL7v2 or ASTM
- **LabReportWorker** → generate reports in your LIS with delta checking, auto-verification rules, and pathologist sign-off workflows
- **LabNotifyWorker** → deliver results via HL7v2 ORU messages to the EHR or FHIR DiagnosticReport to the patient portal
- Add a **CriticalValueWorker** with a SWITCH on result severity to trigger immediate phone calls for panic values per CAP requirements

Replace simulated outputs with real LIS, analyzer middleware, and HL7 notification integrations while keeping the same field structure, and the lab pipeline runs identically.

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
lab-results/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/labresults/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LabResultsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeSampleWorker.java
│       ├── CollectSampleWorker.java
│       ├── LabNotifyWorker.java
│       ├── LabReportWorker.java
│       └── ProcessSampleWorker.java
└── src/test/java/labresults/workers/
    ├── AnalyzeSampleWorkerTest.java        # 2 tests
    └── LabNotifyWorkerTest.java        # 2 tests

```
