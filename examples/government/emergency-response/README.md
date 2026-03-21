# Emergency Response in Java with Conductor :  Incident Detection, Severity Classification, Dispatch, and Debrief

A Java Conductor workflow example for emergency response .  detecting incidents, classifying severity, dispatching response units to a location, coordinating the on-scene response, and conducting a post-incident debrief. Uses [Conductor](https://github.

## The Problem

You need to manage the lifecycle of an emergency incident from the moment it is reported through post-incident review. A report comes in with an incident type and location. The system must register the incident, classify its severity (fire, medical, hazmat, etc.), dispatch the appropriate response units based on severity and proximity, coordinate multi-agency action on scene, and produce a debrief record when the incident is resolved. Each step feeds the next .  you cannot dispatch without a severity classification, and you cannot debrief without knowing which units responded and what the outcome was.

Without orchestration, you'd build a monolithic dispatch system that calls each service in sequence, handles retries when the CAD (computer-aided dispatch) system is slow, and tries to recover gracefully if the system crashes mid-incident. In an emergency, a missed step or lost state can delay response times and cost lives. After-action reviews require a precise timeline of every action taken.

## The Solution

**You just write the incident detection, severity classification, unit dispatch, response coordination, and post-incident debrief logic. Conductor handles dispatch retries, coordination sequencing, and incident response audit trails.**

Each stage of the emergency response is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of running them in sequence, passing the incident ID and severity classification downstream to dispatch and coordination, retrying if the dispatch system is temporarily unavailable, and maintaining a complete timeline of every action for the post-incident debrief. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Incident detection, resource dispatch, field coordination, and situation reporting workers each handle one phase of the emergency response chain.

| Worker | Task | What It Does |
|---|---|---|
| **DetectWorker** | `emr_detect` | Registers the incoming incident report, assigns an incident ID, and records the location and type |
| **ClassifySeverityWorker** | `emr_classify_severity` | Determines incident severity (low/moderate/high/critical) based on incident type and reported details |
| **DispatchWorker** | `emr_dispatch` | Selects and dispatches the appropriate response units (fire, EMS, police) based on severity and location |
| **CoordinateWorker** | `emr_coordinate` | Manages multi-unit coordination on scene and tracks the incident outcome |
| **DebriefWorker** | `emr_debrief` | Produces the post-incident report with timeline, outcome, and lessons learned |

Workers simulate government operations .  application processing, compliance checks, notifications ,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
emr_detect
    │
    ▼
emr_classify_severity
    │
    ▼
emr_dispatch
    │
    ▼
emr_coordinate
    │
    ▼
emr_debrief

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
java -jar target/emergency-response-1.0.0.jar

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
java -jar target/emergency-response-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow emr_emergency_response \
  --version 1 \
  --input '{"incidentType": "TEST-001", "location": "us-east-1", "reportedBy": "sample-reportedBy"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w emr_emergency_response -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real emergency systems .  your CAD system for incident detection, your dispatch platform for unit assignment, your incident command tools for on-scene coordination, and the workflow runs identically in production.

- **DetectWorker** → integrate with your 911/CAD intake system to pull real incident reports as they arrive
- **ClassifySeverityWorker** → connect to your agency's severity classification matrix or an ML-based triage model trained on historical incidents
- **DispatchWorker** → call your CAD dispatch API with automatic vehicle locator (AVL) data to assign the nearest available units
- **CoordinateWorker** → integrate with your incident command system (ICS) for real-time multi-agency coordination
- **DebriefWorker** → write after-action reports to your records management system (RMS) with NFIRS/NEMSIS-compliant data
- Add a **NotifyPublicWorker** to push emergency alerts via Wireless Emergency Alerts (WEA) or Everbridge

Update dispatch protocols or coordination procedures and the response pipeline adapts without changes.

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
emergency-response-emergency-response/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/emergencyresponse/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EmergencyResponseExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifySeverityWorker.java
│       ├── CoordinateWorker.java
│       ├── DebriefWorker.java
│       ├── DetectWorker.java
│       └── DispatchWorker.java
└── src/test/java/emergencyresponse/workers/
    ├── DetectWorkerTest.java
    └── DispatchWorkerTest.java

```
