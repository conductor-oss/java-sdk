# Hospital Discharge Planning in Java Using Conductor :  Readiness Assessment, Care Plan, Education, and Follow-Up Scheduling

A Java Conductor workflow example for hospital discharge planning .  assessing patient readiness for discharge, creating a discharge care plan with post-acute needs, coordinating services (home health, DME, pharmacy), educating the patient on self-care, and scheduling follow-up appointments. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to safely discharge patients from the hospital while preventing readmissions. Each discharge requires assessing whether the patient meets clinical readiness criteria (stable vitals, ambulatory status, pain management). A discharge plan must be created covering medications, activity restrictions, wound care, and post-acute services. Services like home health nursing, durable medical equipment, and prescription delivery must be coordinated. The patient and family need education on medication schedules, warning signs, and when to seek emergency care. Finally, follow-up appointments must be scheduled with the PCP and any specialists within the appropriate timeframe. A missed step .  like failing to schedule follow-up or educate the patient on medication changes ,  directly increases 30-day readmission risk.

Without orchestration, you'd build a monolithic discharge application that checks readiness criteria, writes the plan to the EHR, calls home health agencies, generates education materials, and books follow-up appointments. If the home health referral API is down, you'd need retry logic. If the system crashes after creating the plan but before educating the patient, the patient leaves without understanding their care instructions. CMS penalizes hospitals for excessive readmissions under HRRP, making every discharge a compliance-critical process.

## The Solution

**You just write the discharge workers. Readiness assessment, care plan creation, service coordination, patient education, and follow-up scheduling. Conductor handles step dependencies, automatic retries when a service referral API is down, and complete discharge documentation for HRRP compliance.**

Each stage of the discharge process is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of assessing readiness before creating the plan, coordinating services based on the plan's requirements, delivering education before the patient leaves, scheduling follow-up as the final step, and maintaining a complete record of every discharge for quality reporting. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the discharge process: AssessReadinessWorker checks clinical criteria, CreateDischargePlanWorker builds the care plan, CoordinateWorker arranges post-acute services, EducateWorker delivers patient instructions, and ScheduleFollowupWorker books follow-up visits.

| Worker | Task | What It Does |
|---|---|---|
| **AssessReadinessWorker** | `dsc_assess_readiness` | Evaluates clinical discharge criteria .  stable vitals, adequate mobility, pain control, safe home environment |
| **CreateDischargePlanWorker** | `dsc_create_plan` | Builds the discharge plan with medication reconciliation, activity restrictions, and post-acute service needs |
| **CoordinateWorker** | `dsc_coordinate` | Arranges post-discharge services .  home health, DME delivery, pharmacy, transportation |
| **EducateWorker** | `dsc_educate` | Delivers patient/family education on medications, warning signs, dietary restrictions, and self-care instructions |
| **ScheduleFollowupWorker** | `dsc_schedule_followup` | Books follow-up appointments with PCP and specialists within the clinically appropriate timeframe |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
dsc_assess_readiness
    │
    ▼
dsc_create_plan
    │
    ▼
dsc_coordinate
    │
    ▼
dsc_educate
    │
    ▼
dsc_schedule_followup
```

## Example Output

```
=== Example 482: Discharge Planning ===

Step 1: Registering task definitions...
  Registered: dsc_assess_readiness, dsc_create_plan, dsc_coordinate, dsc_educate, dsc_schedule_followup

Step 2: Registering workflow 'discharge_planning_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [assess] Evaluating discharge readiness for
  [coordinate] Arranging
  [plan] Creating discharge plan with
  [educate] Patient education:
  [follow-up] Scheduling

  Status: COMPLETED
  Output: {readiness=..., needs=..., lengthOfStay=..., servicesArranged=...}

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
java -jar target/discharge-planning-1.0.0.jar
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
java -jar target/discharge-planning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow discharge_planning_workflow \
  --version 1 \
  --input '{"patientId": "PAT-10234", "PAT-10234": "admissionId", "admissionId": "ADM-88201", "ADM-88201": "diagnosis", "diagnosis": "Acute myocardial infarction", "Acute myocardial infarction": "sample-Acute myocardial infarction"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w discharge_planning_workflow -s COMPLETED -c 5
```

## How to Extend

Connect AssessReadinessWorker to your EHR discharge checklist, CoordinateWorker to your home health and DME referral system, and ScheduleFollowupWorker to your outpatient scheduling API. The workflow definition stays exactly the same.

- **AssessReadinessWorker** → integrate with your EHR to pull real vitals, lab results, and mobility assessments for discharge readiness scoring
- **CreateDischargePlanWorker** → write discharge plans to the EHR via FHIR CarePlan resources with medication reconciliation from your pharmacy system
- **CoordinateWorker** → send real referrals to home health agencies, DME suppliers, and pharmacy delivery services via your referral network
- **EducateWorker** → generate personalized patient education materials from your health literacy platform (Healthwise, Krames)
- **ScheduleFollowupWorker** → book real follow-up appointments in your PCP and specialist scheduling systems
- Add a **ReadmissionRiskWorker** to score the patient's readmission risk (LACE, HOSPITAL score) and trigger enhanced follow-up for high-risk patients

Swap in your EHR discharge module and referral management system while preserving the same output contract, and the discharge workflow continues operating without changes.

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
discharge-planning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dischargeplanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DischargePlanningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessReadinessWorker.java
│       ├── CoordinateWorker.java
│       ├── CreateDischargePlanWorker.java
│       ├── EducateWorker.java
│       └── ScheduleFollowupWorker.java
└── src/test/java/dischargeplanning/workers/
    ├── AssessReadinessWorkerTest.java        # 8 tests
    ├── CoordinateWorkerTest.java        # 8 tests
    └── CreateDischargePlanWorkerTest.java        # 8 tests
```
