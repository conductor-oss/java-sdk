# Care Coordination in Java Using Conductor :  Needs Assessment, Care Planning, Team Assembly, and Patient Monitoring

A Java Conductor workflow example for care coordination. assessing a patient's clinical needs based on condition and acuity, building a personalized care plan, assembling the right care team (PCP, specialist, social worker), and activating ongoing monitoring. Uses [Conductor](https://github.

## The Problem

You need to coordinate care for patients with chronic conditions or complex medical needs. When a patient is flagged for care coordination, their clinical condition and acuity must be assessed to determine what services they need. Based on that assessment, a care plan is created with specific goals, interventions, and timelines. The right care team must then be assembled. a primary care physician, relevant specialists, a care manager, and potentially a social worker or behavioral health provider. Finally, the patient must be enrolled in ongoing monitoring so the care team can track progress against the plan.

Without orchestration, you'd build a monolithic care management application that queries the patient's clinical records, runs the needs assessment, writes the care plan to the EHR, sends team assignment notifications, and activates monitoring. all in one service. If the EHR is temporarily unavailable, you'd need retry logic. If the system crashes after creating the care plan but before assigning the team, the patient has a plan but no one to execute it. CMS and NCQA require documentation of every coordination activity for quality measures and accreditation.

## The Solution

**You just write the care coordination workers. Needs assessment, care plan creation, team assembly, and patient monitoring activation. Conductor handles sequencing, automatic retries when an EHR endpoint is slow, and timestamped records for CMS quality reporting.**

Each stage of care coordination is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of assessing needs before building the plan, assembling the team only after the plan defines what specialties are needed, activating monitoring with the assigned team members, and maintaining a complete audit trail for quality reporting. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the coordination lifecycle: AssessNeedsWorker evaluates clinical needs, CreatePlanWorker builds the care plan, AssignTeamWorker assembles the multidisciplinary team, and MonitorWorker activates ongoing tracking.

| Worker | Task | What It Does |
|---|---|---|
| **AssessNeedsWorker** | `ccr_assess_needs` | Evaluates the patient's clinical condition and acuity level to determine required services and interventions |
| **CreatePlanWorker** | `ccr_create_plan` | Builds a personalized care plan with goals, interventions, and timelines based on the needs assessment |
| **AssignTeamWorker** | `ccr_assign_team` | Assembles the multidisciplinary care team (PCP, specialists, care manager) based on the care plan and acuity |
| **MonitorWorker** | `ccr_monitor` | Enrolls the patient in ongoing monitoring and links the care team for progress tracking |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
ccr_assess_needs
    │
    ▼
ccr_create_plan
    │
    ▼
ccr_assign_team
    │
    ▼
ccr_monitor

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
java -jar target/care-coordination-1.0.0.jar

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
java -jar target/care-coordination-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow care_coordination_workflow \
  --version 1 \
  --input '{"patientId": "TEST-001", "condition": "sample-condition", "acuity": "sample-acuity"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w care_coordination_workflow -s COMPLETED -c 5

```

## How to Extend

Connect AssessNeedsWorker to your clinical decision support tool, CreatePlanWorker to your EHR care plan module, and AssignTeamWorker to your provider directory and team assignment system. The workflow definition stays exactly the same.

- **AssessNeedsWorker** → integrate with your clinical decision support system or risk stratification model (e.g., HCC scores, LACE index)
- **CreatePlanWorker** → write care plans to your EHR via FHIR CarePlan resources with structured goals and interventions
- **AssignTeamWorker** → query your provider directory for available specialists and auto-assign based on panel capacity and patient location
- **MonitorWorker** → connect to your remote patient monitoring platform or chronic care management (CCM) system
- Add a **SocialDeterminantsWorker** to screen for SDOH factors (housing, food insecurity, transportation) and link to community resources
- Add a **TransitionOfCareWorker** for patients moving between care settings (inpatient to home health)

Connect each worker to your EHR and care management platform while maintaining the same output contract, and the coordination workflow operates identically.

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
care-coordination/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/carecoordination/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CareCoordinationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessNeedsWorker.java
│       ├── AssignTeamWorker.java
│       ├── CreatePlanWorker.java
│       └── MonitorWorker.java
└── src/test/java/carecoordination/workers/
    ├── AssessNeedsWorkerTest.java        # 9 tests
    ├── AssignTeamWorkerTest.java        # 8 tests
    ├── CreatePlanWorkerTest.java        # 8 tests
    └── MonitorWorkerTest.java        # 8 tests

```
