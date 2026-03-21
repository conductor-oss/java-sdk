# Mental Health Workflow in Java Using Conductor :  Intake, Clinical Assessment, Treatment Planning, and Progress Tracking

A Java Conductor workflow example for mental health care management. performing patient intake with referral reason, conducting clinical assessments (PHQ-9, GAD-7, Columbia Suicide Severity), building an individualized treatment plan, and tracking therapeutic progress over time. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to manage the clinical pathway for behavioral health patients. A referral comes in with a patient ID, referral reason, and assigned provider. The patient must go through intake. collecting demographics, insurance, presenting concerns, and safety screening. A clinical assessment must be performed using validated instruments (PHQ-9 for depression, GAD-7 for anxiety, AUDIT for substance use). Based on the assessment scores, an individualized treatment plan is created with therapy modality (CBT, DBT, EMDR), medication management if indicated, session frequency, and measurable goals. Progress must then be tracked against the treatment plan using outcome measures at each session. A missed safety screening or delayed treatment plan can result in harm to the patient and regulatory consequences.

Without orchestration, you'd build a monolithic behavioral health EHR module that collects intake data, administers assessments, generates the treatment plan, and logs progress notes. If the assessment scoring service is unavailable, intake stalls. If the system crashes after assessment but before creating the treatment plan, the clinician has scores but no plan to act on. 42 CFR Part 2 and state mental health parity laws require strict documentation of every clinical interaction.

## The Solution

**You just write the behavioral health workers. Patient intake, clinical assessment scoring, treatment planning, and progress tracking. Conductor handles clinical step ordering, automatic retries when the assessment scoring service is unavailable, and a 42 CFR Part 2-compliant audit trail.**

Each stage of the mental health workflow is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of completing intake before assessment, building the treatment plan only after assessment scores are available, activating progress tracking after the plan is in place, and maintaining a 42 CFR Part 2-compliant audit trail of every clinical interaction. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage the behavioral health pathway: IntakeWorker collects demographics and safety screening, AssessWorker scores standardized instruments (PHQ-9, GAD-7), TreatmentPlanWorker builds an individualized plan, and TrackProgressWorker monitors therapeutic outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **IntakeWorker** | `mh_intake` | Collects patient demographics, insurance, presenting concerns, safety screening, and referral details |
| **AssessWorker** | `mh_assess` | Administers and scores standardized clinical assessments (PHQ-9, GAD-7, Columbia Suicide Severity) |
| **TreatmentPlanWorker** | `mh_treatment_plan` | Creates an individualized treatment plan with therapy modality, medication, session frequency, and goals |
| **TrackProgressWorker** | `mh_track_progress` | Tracks therapeutic progress using outcome measures against the treatment plan goals |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
mh_intake
    │
    ▼
mh_assess
    │
    ▼
mh_treatment_plan
    │
    ▼
mh_track_progress

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
java -jar target/mental-health-1.0.0.jar

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
java -jar target/mental-health-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mental_health_workflow \
  --version 1 \
  --input '{"patientId": "TEST-001", "referralReason": "sample-referralReason", "provider": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mental_health_workflow -s COMPLETED -c 5

```

## How to Extend

Connect IntakeWorker to your behavioral health EHR, AssessWorker to your validated assessment instruments (PHQ-9, GAD-7), and TreatmentPlanWorker to your treatment planning module. The workflow definition stays exactly the same.

- **IntakeWorker** → integrate with your behavioral health EHR (Qualifacts, Netsmart, Credible) for real patient intake and safety screening
- **AssessWorker** → administer PHQ-9, GAD-7, AUDIT, and Columbia Suicide Severity through your assessment platform with automated scoring
- **TreatmentPlanWorker** → generate treatment plans in your EHR with DSM-5 diagnoses, evidence-based modalities, and SMART goals
- **TrackProgressWorker** → record session outcomes using validated measures (OQ-45, PCOMS) and flag patients not meeting treatment targets
- Add a **CrisisInterventionWorker** with a SWITCH on suicide risk score to trigger safety planning protocols for high-risk patients
- Add a **PriorAuthWorker** before treatment planning to verify behavioral health carve-out benefits and session limits

Connect each worker to your behavioral health EHR and outcomes platform while returning the same fields, and the clinical pathway workflow operates without changes.

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
mental-health/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mentalhealth/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MentalHealthExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── IntakeWorker.java
│       ├── TrackProgressWorker.java
│       └── TreatmentPlanWorker.java
└── src/test/java/mentalhealth/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── IntakeWorkerTest.java        # 2 tests
    ├── TrackProgressWorkerTest.java        # 2 tests
    └── TreatmentPlanWorkerTest.java        # 2 tests

```
