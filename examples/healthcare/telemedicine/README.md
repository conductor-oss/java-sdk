# Telemedicine Visit in Java Using Conductor :  Scheduling, Video Connection, Clinical Consultation, e-Prescribing, and Follow-Up

A Java Conductor workflow example for telemedicine visits .  scheduling the virtual appointment, establishing the secure video connection, conducting the clinical consultation, writing and transmitting e-prescriptions, and scheduling follow-up care. Uses [Conductor](https://github.## The Problem

You need to manage the full lifecycle of a telemedicine visit. A patient requests a virtual visit with a provider for a specific clinical reason. The appointment must be scheduled on both the patient's and provider's calendars. At the appointment time, a secure healthcare-pattern video connection must be established. The provider conducts the consultation .  reviewing the chief complaint, taking history, and making an assessment. If medication is indicated, an e-prescription must be transmitted to the patient's preferred pharmacy. Finally, follow-up care must be arranged ,  a future appointment, lab orders, or referral to a specialist. Each step must complete before the next ,  you cannot consult without a connection, and you cannot prescribe without a consultation.

Without orchestration, you'd build a monolithic telehealth platform that manages scheduling, video sessions, clinical documentation, EPCS (electronic prescribing for controlled substances), and follow-up .  all in one tightly coupled system. If the video platform has a brief outage, the entire visit flow fails. If the system crashes after the consultation but before the prescription is sent, the patient leaves the visit without their medication. Payers and state telehealth parity laws require documentation of the visit modality, duration, and clinical decision-making.

## The Solution

**You just write the telehealth workers. Visit scheduling, video connection, clinical consultation, e-prescribing, and follow-up arrangement. Conductor handles visit stage sequencing, automatic retries when the video platform has a brief outage, and a complete visit record for billing and telehealth parity compliance.**

Each stage of the telemedicine visit is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of scheduling before connecting, consulting only after the video session is established, prescribing based on the consultation findings, arranging follow-up as the final step, and maintaining a complete visit record for billing and compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the virtual visit lifecycle: ScheduleWorker books the appointment, ConnectWorker establishes the secure video session, ConsultWorker records the clinical encounter, PrescribeWorker transmits e-prescriptions, and FollowUpWorker arranges post-visit care.

| Worker | Task | What It Does |
|---|---|---|
| **ScheduleWorker** | `tlm_schedule` | Books the virtual visit on both patient and provider calendars with visit link and instructions |
| **ConnectWorker** | `tlm_connect` | Establishes the secure, healthcare-pattern video connection between patient and provider |
| **ConsultWorker** | `tlm_consult` | Records the clinical consultation .  chief complaint, history, assessment, and plan |
| **PrescribeWorker** | `tlm_prescribe` | Writes and transmits the e-prescription to the patient's pharmacy via Surescripts |
| **FollowUpWorker** | `tlm_followup` | Arranges follow-up care .  future appointments, lab orders, referrals, or patient education |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### The Workflow

```
tlm_schedule
    │
    ▼
tlm_connect
    │
    ▼
tlm_consult
    │
    ▼
tlm_prescribe
    │
    ▼
tlm_followup
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
java -jar target/telemedicine-1.0.0.jar
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
java -jar target/telemedicine-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow telemedicine_workflow \
  --version 1 \
  --input '{"visitId": "TEST-001", "patientId": "TEST-001", "providerId": "TEST-001", "reason": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w telemedicine_workflow -s COMPLETED -c 5
```

## How to Extend

Connect ScheduleWorker to your calendar system, ConnectWorker to your healthcare-pattern video platform (Zoom for Healthcare, Doxy.me), and PrescribeWorker to your EPCS e-prescribing system via Surescripts. The workflow definition stays exactly the same.

- **ScheduleWorker** → integrate with your EHR scheduling module and send calendar invites with video links to patient and provider
- **ConnectWorker** → launch sessions on your healthcare-pattern video platform (Zoom for Healthcare, Twilio Video, Doxy.me) with waiting room management
- **ConsultWorker** → write clinical encounter notes to your EHR with structured data for assessment, diagnosis codes, and plan
- **PrescribeWorker** → transmit real e-prescriptions via Surescripts with EPCS (electronic prescribing for controlled substances) support
- **FollowUpWorker** → create follow-up orders in your EHR, schedule appointments, and send post-visit summaries to the patient portal
- Add a **InsuranceCheckWorker** before scheduling to verify telehealth benefit coverage and state parity law eligibility

Connect each worker to your telehealth video platform, EHR, and e-prescribing system while returning the same fields, and the telemedicine workflow runs without any modifications.

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
telemedicine/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/telemedicine/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TelemedicineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConnectWorker.java
│       ├── ConsultWorker.java
│       ├── FollowUpWorker.java
│       ├── PrescribeWorker.java
│       └── ScheduleWorker.java
└── src/test/java/telemedicine/workers/
    ├── ConnectWorkerTest.java        # 2 tests
    ├── ConsultWorkerTest.java        # 2 tests
    ├── FollowUpWorkerTest.java        # 2 tests
    ├── PrescribeWorkerTest.java        # 2 tests
    └── ScheduleWorkerTest.java        # 2 tests
```
