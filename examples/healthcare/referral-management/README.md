# Referral Management in Java Using Conductor :  Creation, Specialist Matching, Scheduling, Tracking, and Closure

A Java Conductor workflow example for referral management .  creating a referral from a PCP with clinical reason, matching the patient to an in-network specialist by specialty and availability, scheduling the specialist appointment, tracking the referral through completion, and closing with the consultation report. Uses [Conductor](https://github.## The Problem

You need to manage patient referrals from creation through specialist consultation. A primary care physician creates a referral specifying the patient, specialty needed, and clinical reason. The system must match the patient to an appropriate in-network specialist based on specialty, location, availability, and insurance acceptance. An appointment must be scheduled with the matched specialist. The referral must be tracked to ensure the patient actually sees the specialist and the consultation report is sent back to the referring provider. Finally, the referral is closed when the consultation is complete. Lost referrals .  where patients never see the specialist or reports never reach the PCP ,  lead to gaps in care and potential liability.

Without orchestration, you'd build a monolithic referral system that creates the order, queries the provider directory, calls the scheduling API, monitors for completion, and archives the referral. If the provider directory is temporarily unavailable, the patient cannot be matched. If the system crashes after scheduling but before tracking begins, the referral falls through the cracks. Value-based care contracts and quality measures require referral loop closure rates for performance reporting.

## The Solution

**You just write the referral workers. Order creation, specialist matching, appointment scheduling, completion tracking, and loop closure. Conductor handles lifecycle sequencing, automatic retries when the provider directory is temporarily unavailable, and full referral loop visibility for quality reporting.**

Each stage of the referral lifecycle is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of matching specialists only after the referral is created, scheduling only after a specialist is matched, tracking through completion, closing with the consultation report, and providing full visibility into every referral's status. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the referral lifecycle: CreateReferralWorker generates the order, MatchSpecialistWorker finds the best in-network provider, ScheduleReferralWorker books the appointment, TrackReferralWorker monitors completion, and CloseReferralWorker closes the loop with the consultation report.

| Worker | Task | What It Does |
|---|---|---|
| **CreateReferralWorker** | `ref_create` | Creates the referral order with patient, specialty, clinical reason, and referring provider |
| **MatchSpecialistWorker** | `ref_match_specialist` | Finds the best-match in-network specialist based on specialty, location, availability, and insurance |
| **ScheduleReferralWorker** | `ref_schedule` | Books the appointment with the matched specialist and notifies the patient |
| **TrackReferralWorker** | `ref_track` | Monitors the referral for appointment completion and consultation report receipt |
| **CloseReferralWorker** | `ref_close` | Closes the referral loop when the consultation report is received by the referring provider |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### The Workflow

```
ref_create
    │
    ▼
ref_match_specialist
    │
    ▼
ref_schedule
    │
    ▼
ref_track
    │
    ▼
ref_close
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
java -jar target/referral-management-1.0.0.jar
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
java -jar target/referral-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow referral_management_workflow \
  --version 1 \
  --input '{"referralId": "TEST-001", "patientId": "TEST-001", "specialty": "test-value", "reason": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w referral_management_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CreateReferralWorker to your EHR referral module, MatchSpecialistWorker to your provider directory and network database, and ScheduleReferralWorker to your outpatient scheduling system. The workflow definition stays exactly the same.

- **CreateReferralWorker** → create referral orders in your EHR via FHIR ServiceRequest resources with clinical attachments
- **MatchSpecialistWorker** → query your provider directory with network status, panel capacity, and patient insurance matching
- **ScheduleReferralWorker** → book real specialist appointments through your scheduling system with patient notifications via SMS/email
- **TrackReferralWorker** → monitor referral completion via your care coordination platform and send reminder outreach for no-shows
- **CloseReferralWorker** → close the referral loop in your EHR when the consultation note arrives via Direct messaging or FHIR DocumentReference
- Add a **PriorAuthWorker** after specialist matching to obtain prior authorization if required by the patient's insurance plan

Point each worker at your EHR referral module and provider directory while preserving the same output structure, and the referral management workflow needs no changes.

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
referral-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/referralmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReferralManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseReferralWorker.java
│       ├── CreateReferralWorker.java
│       ├── MatchSpecialistWorker.java
│       ├── ScheduleReferralWorker.java
│       └── TrackReferralWorker.java
└── src/test/java/referralmanagement/workers/
    ├── CloseReferralWorkerTest.java        # 2 tests
    ├── CreateReferralWorkerTest.java        # 2 tests
    ├── MatchSpecialistWorkerTest.java        # 2 tests
    ├── ScheduleReferralWorkerTest.java        # 2 tests
    └── TrackReferralWorkerTest.java        # 2 tests
```
