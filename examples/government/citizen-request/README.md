# Citizen Request in Java with Conductor

Processes citizen service requests (pothole repairs, streetlight outages, noise complaints): submitting, classifying by type and urgency, routing to the responsible department, resolving, and notifying the citizen. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process a citizen service request (pothole repair, streetlight outage, noise complaint, etc.). The request is submitted, classified by type and urgency, routed to the appropriate department or crew, resolved by the responsible team, and the citizen is notified of the resolution. Misclassifying a request sends it to the wrong department; failing to notify leaves citizens wondering if their government is responsive.

Without orchestration, you'd manage service requests through a call center or web form, manually classifying and routing them to departments via email, tracking status in spreadsheets, and following up with citizens by phone. losing requests in inter-department handoffs and missing response-time SLAs.

## The Solution

**You just write the request submission, classification, department routing, resolution, and citizen notification logic. Conductor handles fulfillment retries, routing logic, and citizen request audit trails.**

Each service request concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, classify, route, resolve, notify), tracking every request with timestamps and department assignments, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Request intake, routing, fulfillment, and response workers handle citizen service requests as a traceable chain of government actions.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `ctz_submit` | Receives the citizen's service request (e.g., pothole report) and assigns a tracking ID |
| **ClassifyWorker** | `ctz_classify` | Classifies the request type (infrastructure, sanitation, parks, etc.) for department routing |
| **RouteWorker** | `ctz_route` | Routes the classified request to the responsible department (e.g., Public Works for infrastructure) |
| **ResolveWorker** | `ctz_resolve` | The assigned department resolves the request and records the resolution (e.g., "Pothole repaired at Main St") |
| **NotifyWorker** | `ctz_notify` | Sends a notification to the citizen that their request has been resolved |

Workers implement government operations. application processing, compliance checks, notifications,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
ctz_submit
    │
    ▼
ctz_classify
    │
    ▼
ctz_route
    │
    ▼
ctz_resolve
    │
    ▼
ctz_notify

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
java -jar target/citizen-request-1.0.0.jar

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
java -jar target/citizen-request-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ctz_citizen_request \
  --version 1 \
  --input '{"citizenId": "TEST-001", "requestType": "standard", "description": "sample-description"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ctz_citizen_request -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real 311 systems. your citizen request portal for intake, your department routing engine for assignment, your work order system for resolution tracking, and the workflow runs identically in production.

- **Request submitter**: accept requests via your 311 system, citizen portal, or mobile app with photo/location attachments
- **Classifier**: auto-classify request type and urgency using NLP on the description or ML models trained on historical requests
- **Router**: route to the appropriate department (Public Works, Code Enforcement, Parks) based on classification and geographic jurisdiction
- **Resolver**: integrate with your work order management system (CityWorks, Cartegraph) for dispatch and completion tracking
- **Citizen notifier**: send status updates via email, SMS, or app notification at each stage of resolution

Update routing rules or fulfillment procedures and the request pipeline handles them transparently.

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
citizen-request-citizen-request/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/citizenrequest/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CitizenRequestExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── NotifyWorker.java
│       ├── ResolveWorker.java
│       ├── RouteWorker.java
│       └── SubmitWorker.java
└── src/test/java/citizenrequest/workers/
    ├── ClassifyWorkerTest.java
    ├── NotifyWorkerTest.java
    ├── ResolveWorkerTest.java
    ├── RouteWorkerTest.java
    └── SubmitWorkerTest.java

```
