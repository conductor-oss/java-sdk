# Healthcare Appointment Scheduling in Java Using Conductor :  Provider Availability, Booking, Confirmation, and Reminders

A Java Conductor workflow example for healthcare appointment scheduling .  checking provider availability for a preferred date, booking the selected time slot, sending the patient a confirmation, and scheduling appointment reminders. Uses [Conductor](https://github.

## The Problem

You need to schedule patient appointments across a healthcare organization. A scheduling request comes in with a provider ID, preferred date, and visit type. The system must query the provider's calendar for open slots (and suggest alternates if the preferred time is taken), reserve the chosen slot, send the patient a confirmation with the appointment details, and schedule a reminder notification before the visit. Each step depends on the previous one .  you cannot book without an available slot, and you cannot confirm without a booking.

Without orchestration, you'd build a monolithic scheduling service that queries the EHR calendar, writes to the booking database, calls the notification API, and sets up the reminder .  all in a single class with inline error handling. If the EHR is briefly unavailable, you'd need retry logic. If the system crashes after booking but before confirming, the patient never receives their appointment details, and the front desk has no record that the confirmation failed.

## The Solution

**You just write the scheduling workers. Availability checks, slot booking, patient confirmation, and reminder setup. Conductor handles step sequencing, automatic retries when the EHR is briefly unavailable, and a complete scheduling audit trail.**

Each step of the scheduling process is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of checking availability before booking, sending confirmations only after a successful booking, retrying if the EHR calendar API is temporarily unavailable, and maintaining a complete record of every scheduling attempt. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers divide the scheduling lifecycle: CheckAvailabilityWorker queries provider calendars, BookWorker reserves the slot, ConfirmWorker notifies the patient, and RemindWorker sets up pre-visit reminders.

| Worker | Task | What It Does |
|---|---|---|
| **CheckAvailabilityWorker** | `apt_check_availability` | Queries the provider's calendar for the preferred date, returns the best available slot and alternate options |
| **BookWorker** | `apt_book` | Reserves the selected time slot on the provider's calendar for the patient |
| **ConfirmWorker** | `apt_confirm` | Sends appointment confirmation (date, time, location, provider) to the patient via SMS or email |
| **RemindWorker** | `apt_remind` | Schedules a reminder notification to be sent before the appointment (e.g., 24 hours prior) |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### The Workflow

```
Input -> BookWorker -> CheckAvailabilityWorker -> ConfirmWorker -> RemindWorker -> Output

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
java -jar target/appointment-scheduling-1.0.0.jar

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
java -jar target/appointment-scheduling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow appointment_scheduling \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w appointment_scheduling -s COMPLETED -c 5

```

## How to Extend

Connect CheckAvailabilityWorker to your EHR scheduling API (Epic, Cerner), BookWorker to your practice management system, and ConfirmWorker to Twilio or SendGrid for patient notifications. The workflow definition stays exactly the same.

- **CheckAvailabilityWorker** → integrate with your EHR scheduling API (Epic FHIR, Cerner Open, athenahealth) to pull real provider calendars
- **BookWorker** → write the appointment to your practice management system with patient demographics and insurance info
- **ConfirmWorker** → send real SMS/email confirmations via Twilio or your patient portal messaging system
- **RemindWorker** → schedule reminders through your notification platform with escalation if the patient does not confirm
- Add an **InsuranceVerifyWorker** before booking to check the patient's coverage for the visit type
- Add a **WaitlistWorker** with a SWITCH on availability to place patients on a waitlist when no slots are open

Swap in real EHR calendar and notification APIs while preserving the same output fields, and the scheduling workflow continues without modification.

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
appointment-scheduling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/appointmentscheduling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AppointmentSchedulingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BookWorker.java
│       ├── CheckAvailabilityWorker.java
│       ├── ConfirmWorker.java
│       └── RemindWorker.java
└── src/test/java/appointmentscheduling/workers/
    ├── BookWorkerTest.java        # 8 tests
    ├── CheckAvailabilityWorkerTest.java        # 8 tests
    ├── ConfirmWorkerTest.java        # 7 tests
    └── RemindWorkerTest.java        # 8 tests

```
