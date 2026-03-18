# Patient Intake in Java Using Conductor: Registration, Insurance Verification, Triage, and Provider Assignment

The patient filled out her name, date of birth, and insurance information on a clipboard in the waiting room. Then she gave the same information verbally to the nurse at the triage station. Then a registration clerk typed it into the EMR. except they entered the birth year as 1987 instead of 1978, which pulled the wrong insurance policy, which flagged her as uninsured, which delayed her triage assessment by 40 minutes while someone sorted it out. She came in with chest pain. Three handoffs, three chances for transcription error, and the data entry mistake followed her all the way to the provider assignment. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate patient intake end-to-end, registration, insurance verification, triage, and provider assignment, as a single automated pipeline where data flows once and each step depends on verified output from the last.

## The Problem

You need to process patient intake at a healthcare facility. When a patient arrives, they must be registered in the system with demographics, contact information, and initial vitals (blood pressure, heart rate, temperature, weight). Their insurance coverage must be verified to confirm active benefits and determine copay/coinsurance. A triage assessment must be performed based on the chief complaint to determine acuity level (ESI 1-5 in emergency settings, or urgency classification in clinic settings). Based on the triage outcome, the patient is assigned to the appropriate provider: a physician, nurse practitioner, or specialist. Each step must complete before the next can proceed, you cannot triage without registration data, and you cannot assign a provider without a triage level.

Without orchestration, you'd build a monolithic intake application that writes to the registration database, calls the insurance eligibility API, runs the triage assessment, and queries provider availability. If the insurance verification service is down, the entire intake process stalls. If the system crashes after registration but before triage, the patient is registered but sitting in the waiting room with no acuity level and no provider. EMTALA requires documentation of triage timing for emergency departments.

## The Solution

**You just write the intake workers. Patient registration, insurance verification, clinical triage, and provider assignment. Conductor handles step dependencies, automatic retries when the insurance eligibility API is down, and a complete intake timeline for EMTALA compliance.**

Each step of the intake process is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of registering before verifying insurance, triaging after registration, assigning providers based on triage acuity, retrying if the insurance eligibility system is temporarily unavailable, and maintaining a complete intake timeline for EMTALA and regulatory compliance. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the intake process: RegisterWorker captures demographics and vitals, VerifyInsuranceWorker confirms coverage, TriageWorker assigns acuity level, and AssignWorker routes the patient to the right provider.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **RegisterWorker** | `pit_register` | Registers the patient with demographics, contact info, and initial vitals (BP, HR, temp, weight) | Simulated. Swap in your EHR registration API for production |
| **VerifyInsuranceWorker** | `pit_verify_insurance` | Verifies active insurance coverage, benefit details, and patient financial responsibility (copay, deductible) | Simulated. Swap in your 270/271 eligibility API for production |
| **TriageWorker** | `pit_triage` | Performs triage assessment based on chief complaint and vitals, assigns acuity level (ESI 1-5) | Simulated. Swap in your clinical triage decision support tool for production |
| **AssignWorker** | `pit_assign` | Assigns the patient to an available provider based on triage acuity and provider panel capacity | Simulated. Swap in your provider scheduling and assignment system for production |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations, the workflow and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> AssignWorker -> RegisterWorker -> TriageWorker -> VerifyInsuranceWorker -> Output
```

## Example Output

```
=== Patient Intake Demo ===

Step 1: Registering task definitions...
  Registered: pit_register, pit_verify_insurance, pit_triage, pit_assign

Step 2: Registering workflow 'patient_intake_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 9062f2bc-4fe6-d6f2-585c-fc86f099ef61

  [register] Patient Sarah Johnson (PAT-10234)
  [insurance] Verifying insurance INS-BC-55012
  [triage] Complaint: \"Severe headache and dizziness\" -> Level medium (dept-value)
  [assign] Assigned to Emergency in dept-value

  Status: COMPLETED
  Output: {registrationResult=2026-03-16, insuranceVerified=true, triageLevel=medium, assignedProvider=ASSIGNEDPROVIDER-001}

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
java -jar target/patient-intake-1.0.0.jar
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
java -jar target/patient-intake-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow patient_intake \
  --version 1 \
  --input '{"patientId": "PAT-10234", "name": "Sarah Johnson", "chiefComplaint": "Severe headache and dizziness", "insuranceId": "INS-BC-55012"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w patient_intake -s COMPLETED -c 5
```

## How to Extend

Connect RegisterWorker to your EHR registration API, VerifyInsuranceWorker to your 270/271 eligibility endpoint, and TriageWorker to your clinical triage decision support tool. The workflow definition stays exactly the same.

- **RegisterWorker** → integrate with your EHR patient registration module (Epic ADT, Cerner Registration) for real demographics and vitals capture
- **VerifyInsuranceWorker** → call real 270/271 eligibility transactions and check prior authorization requirements for the visit type
- **TriageWorker** → implement the ESI (Emergency Severity Index) algorithm or your clinic's triage protocol with clinical decision support
- **AssignWorker** → query your provider scheduling system for real-time availability and panel capacity by specialty
- Add a **ConsentWorker** between registration and triage to collect HIPAA consent, treatment consent, and advance directive status
- Add a **WaitlistWorker** with a SWITCH on provider availability to place patients on a waitlist when all providers are at capacity

Point each worker at your real EHR registration, eligibility, and triage systems while keeping the same output structure, and the intake workflow runs without modification.

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
patient-intake/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/patientintake/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PatientIntakeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignWorker.java
│       ├── RegisterWorker.java
│       ├── TriageWorker.java
│       └── VerifyInsuranceWorker.java
└── src/test/java/patientintake/workers/
    ├── AssignWorkerTest.java        # 8 tests
    ├── RegisterWorkerTest.java        # 8 tests
    ├── TriageWorkerTest.java        # 8 tests
    └── VerifyInsuranceWorkerTest.java        # 8 tests
```
