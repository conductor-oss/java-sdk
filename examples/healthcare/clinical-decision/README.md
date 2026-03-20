# Clinical Decision Support in Java Using Conductor :  Data Gathering, Guideline Application, Risk Scoring, and Treatment Recommendations

A Java Conductor workflow example for clinical decision support .  gathering patient clinical data, applying evidence-based guidelines, computing a risk score, and generating treatment recommendations for the clinician. Uses [Conductor](https://github.## The Problem

You need to provide real-time clinical decision support at the point of care. When a clinician is treating a patient, the system must pull the patient's clinical history (labs, vitals, medications, diagnoses), apply evidence-based clinical guidelines for the presenting condition, compute a risk score (e.g., Framingham for cardiovascular risk, CURB-65 for pneumonia severity), and generate specific treatment recommendations. Each step depends on the previous one .  you cannot apply guidelines without the patient's data, and you cannot recommend treatments without a risk score.

Without orchestration, you'd build a monolithic CDS engine that queries the EHR, runs the guideline rules engine, calculates the score, and formats the recommendation .  all in a single request handler. If the EHR data service is slow, the entire recommendation is delayed. If new guidelines are published, you'd need to update and redeploy the entire system. Regulatory requirements demand logging of every recommendation generated for patient safety audits.

## The Solution

**You just write the clinical decision support workers. Patient data gathering, guideline application, risk scoring, and treatment recommendation. Conductor handles data flow between stages, retries when the EHR data service is slow, and regulatory logging of every recommendation.**

Each stage of the clinical decision support pipeline is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of gathering data before applying guidelines, feeding guideline outputs into the risk scorer, generating recommendations only after scoring is complete, and logging every step for regulatory compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the CDS pipeline: GatherDataWorker pulls clinical history, ApplyGuidelinesWorker evaluates evidence-based criteria, ScoreRiskWorker computes risk scores, and RecommendWorker generates treatment recommendations.

| Worker | Task | What It Does |
|---|---|---|
| **GatherDataWorker** | `cds_gather_data` | Pulls the patient's clinical data .  labs, vitals, medications, diagnoses, and history ,  for the given condition |
| **ApplyGuidelinesWorker** | `cds_apply_guidelines` | Evaluates the patient data against evidence-based clinical guidelines (e.g., AHA, USPSTF, NICE) |
| **ScoreRiskWorker** | `cds_score_risk` | Computes a clinical risk score (Framingham, ASCVD, Wells, CURB-65) based on the guideline-filtered data |
| **RecommendWorker** | `cds_recommend` | Generates specific treatment recommendations (medications, procedures, follow-up) based on the risk score |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### The Workflow

```
cds_gather_data
    │
    ▼
cds_apply_guidelines
    │
    ▼
cds_score_risk
    │
    ▼
cds_recommend
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
java -jar target/clinical-decision-1.0.0.jar
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
java -jar target/clinical-decision-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow clinical_decision_workflow \
  --version 1 \
  --input '{"patientId": "TEST-001", "condition": "test-value", "clinicalContext": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w clinical_decision_workflow -s COMPLETED -c 5
```

## How to Extend

Connect GatherDataWorker to your EHR's FHIR API, ApplyGuidelinesWorker to your clinical rules engine (CQL, Drools), and ScoreRiskWorker to your validated risk calculator (Framingham, ASCVD). The workflow definition stays exactly the same.

- **GatherDataWorker** → pull real patient data from your EHR via FHIR R4 (Patient, Observation, MedicationRequest, Condition resources)
- **ApplyGuidelinesWorker** → integrate a CQL (Clinical Quality Language) evaluator or clinical rules engine with your organization's guideline library
- **ScoreRiskWorker** → implement validated risk calculators (Framingham, ASCVD Pooled Cohort, CHADS2-VASc, MELD) with real patient parameters
- **RecommendWorker** → generate recommendations against your formulary, check drug-drug interactions, and format CDS Hooks cards for EHR display
- Add a **DrugInteractionWorker** between guideline application and recommendation to flag contraindications with the patient's current medications

Point each worker at your real FHIR API, rules engine, and formulary while returning the same fields, and the decision support pipeline runs unchanged.

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
clinical-decision/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/clinicaldecision/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ClinicalDecisionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyGuidelinesWorker.java
│       ├── GatherDataWorker.java
│       ├── RecommendWorker.java
│       └── ScoreRiskWorker.java
└── src/test/java/clinicaldecision/workers/
    ├── ApplyGuidelinesWorkerTest.java        # 2 tests
    ├── GatherDataWorkerTest.java        # 2 tests
    ├── RecommendWorkerTest.java        # 2 tests
    └── ScoreRiskWorkerTest.java        # 2 tests
```
