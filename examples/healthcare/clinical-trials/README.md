# Clinical Trials in Java Using Conductor: Screening, Consent, Randomization, Monitoring, and Analysis

A promising cardiac drug candidate has 200 patients waiting to enroll. Eligibility screening is a manual chart review that takes three days per patient. Consent forms are mailed, signed, scanned, and uploaded to a shared drive. Randomization happens in a spreadsheet that the biostatistician updates on Tuesdays. The result: it takes six weeks from "patient expressed interest" to "patient is enrolled and randomized," and by then a quarter of them have dropped out or enrolled in a competing trial. When the FDA auditor asks to see the audit trail for participant SUBJ-4401. exactly when they were screened, who obtained consent, how they were randomized, the clinical ops team spends two days assembling it from three disconnected systems. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full trial enrollment pipeline, screening, consent, randomization, monitoring, and analysis, with strict step sequencing and a 21 CFR Part 11-compliant audit trail built in.

## The Problem

You need to manage participants through the lifecycle of a clinical trial. Each potential participant must be screened against the trial's inclusion and exclusion criteria. Those who qualify must provide informed consent before being enrolled. Consented participants are then randomized into treatment or control arms. Throughout the trial, participants must be monitored for adverse events and protocol deviations. At the end, trial data must be analyzed and outcomes reported. Every step requires strict sequencing. You cannot randomize without consent, and you cannot analyze without monitoring data. FDA 21 CFR Part 11 requires a complete, tamper-evident audit trail of every action.

Without orchestration, you'd build a monolithic trial management system that checks eligibility, records consent, calls the randomization service, logs monitoring events, and runs the analysis, all with inline error handling. If the randomization service fails after consent is recorded, you'd need to track where the participant is in the process. Sponsors and the FDA demand full traceability of every participant interaction for compliance audits.

## The Solution

**You just write the trial management workers. Participant screening, consent collection, arm randomization, adverse event monitoring, and outcome analysis. Conductor handles strict step sequencing, automatic retries, and a 21 CFR Part 11-compliant audit trail of every participant interaction.**

Each stage of the trial enrollment pipeline is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of running screening before consent, randomizing only after consent is obtained, triggering monitoring after randomization, analyzing only after monitoring is complete, and maintaining a 21 CFR Part 11-compliant audit trail of every step. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the trial enrollment pipeline: ScreenWorker checks eligibility, ConsentWorker records informed consent, RandomizeWorker assigns treatment arms, MonitorWorker tracks adverse events, and AnalyzeTrialWorker runs outcome analysis.

| Worker | Task | What It Does |
|---|---|---|
| **ScreenWorker** | `clt_screen` | Evaluates the participant against inclusion/exclusion criteria for the specified trial and condition |
| **ConsentWorker** | `clt_consent` | Records the participant's informed consent with electronic signature and version tracking |
| **RandomizeWorker** | `clt_randomize` | Assigns the participant to a treatment arm using stratified block randomization |
| **MonitorWorker** | `clt_monitor` | Tracks the participant through the trial for adverse events, protocol deviations, and study visits |
| **AnalyzeTrialWorker** | `clt_analyze` | Runs the statistical analysis on collected trial data and generates outcome reports |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations, the workflow and compliance logic stay the same.

### The Workflow

```
clt_screen
    │
    ▼
clt_consent
    │
    ▼
clt_randomize
    │
    ▼
clt_monitor
    │
    ▼
clt_analyze

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
java -jar target/clinical-trials-1.0.0.jar

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
java -jar target/clinical-trials-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow clinical_trials_workflow \
  --version 1 \
  --input '{"trialId": "TRIAL-2024-CARDIO-001", "participantId": "SUBJ-4401", "condition": "hypertension"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w clinical_trials_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ScreenWorker to your CTMS eligibility engine, ConsentWorker to your eConsent platform (DocuSign, REDCap), and RandomizeWorker to your IWRS randomization system. The workflow definition stays exactly the same.

- **ScreenWorker** → integrate with your CTMS to evaluate eligibility against real trial protocols and patient EHR data
- **ConsentWorker** → connect to an eConsent platform (DocuSign CLM, REDCap, Medable) for 21 CFR Part 11-compliant electronic signatures
- **RandomizeWorker** → call your IWRS/IRT system for blinded, stratified randomization with drug supply allocation
- **MonitorWorker** → integrate with your EDC system (Medidata Rave, Oracle Clinical, Veeva) for real-time adverse event tracking
- **AnalyzeTrialWorker** → trigger SAS or R pipelines for interim and final analyses with CDISC-compliant data packages
- Add a **SafetyReviewWorker** with a SWITCH on adverse event severity to trigger DSMB notifications for serious adverse events

Connect each worker to your CTMS, eConsent platform, and EDC system while keeping the same output fields, and the trial management workflow requires no modifications.

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
clinical-trials/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/clinicaltrials/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ClinicalTrialsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTrialWorker.java
│       ├── ConsentWorker.java
│       ├── MonitorWorker.java
│       ├── RandomizeWorker.java
│       └── ScreenWorker.java
└── src/test/java/clinicaltrials/workers/
    ├── AnalyzeTrialWorkerTest.java        # 2 tests
    └── ScreenWorkerTest.java        # 2 tests

```
