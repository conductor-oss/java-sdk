# Project Risk Management in Java with Conductor :  Identification, Severity Assessment, and Mitigation Planning

A Java Conductor workflow example for project risk management. identifying risks, assessing their severity (high/medium/low), routing to severity-appropriate handling, and producing mitigation plans. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to manage risks across a project portfolio. When a new risk is reported. "key vendor may miss delivery deadline," "database migration could cause downtime",  someone has to identify and categorize it, assess severity and probability, route it to the right response process (executive escalation for high-severity, team-level review for medium, backlog tracking for low), and generate a mitigation plan. Each of these steps depends on the output of the previous one, and the routing logic changes based on the assessed severity.

Without orchestration, you'd wire all of this into a single monolithic class. if/else chains to route by severity, try/catch blocks around every step, manual logging to audit which risks were escalated and why. That code becomes brittle as severity categories change, hard to extend when you add new response processes, and impossible to audit when stakeholders ask "why wasn't this risk escalated?"

## The Solution

**You just write the risk identification, severity assessment, severity-based handling, and mitigation planning logic. Conductor handles assessment retries, mitigation routing, and risk tracking audit trails.**

Each concern is a simple, independent worker. a plain Java class that does one thing: identify the risk, assess its severity, handle the high/medium/low response, or build the mitigation plan. Conductor takes care of executing them in the right order, routing to the correct severity handler via a SWITCH task, retrying on failure, tracking every execution, and resuming if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Risk identification, probability assessment, mitigation planning, and monitoring workers each address one stage of project risk governance.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `rkm_identify` | Parses the risk description, assigns a risk ID, and categorizes it (technical, schedule, resource, external) |
| **AssessWorker** | `rkm_assess` | Evaluates severity (high/medium/low), probability, and business impact of the identified risk |
| **HighWorker** | `rkm_high` | Triggers executive escalation and immediate response actions for high-severity risks |
| **MediumWorker** | `rkm_medium` | Schedules team-level review and assigns a risk owner for medium-severity risks |
| **LowWorker** | `rkm_low` | Logs the risk to the backlog for periodic review (default path for low-severity risks) |
| **MitigateWorker** | `rkm_mitigate` | Generates a mitigation plan based on the project context and assessed severity |

Workers implement project management operations. task creation, status updates, notifications,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
rkm_identify
    │
    ▼
rkm_assess
    │
    ▼
SWITCH (switch_ref)
    ├── high: rkm_high
    ├── medium: rkm_medium
    └── default: rkm_low
    │
    ▼
rkm_mitigate

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
java -jar target/risk-management-1.0.0.jar

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
java -jar target/risk-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow risk_management_risk-management \
  --version 1 \
  --input '{"projectId": "PROJ-42", "PROJ-42": "riskDescription", "riskDescription": "Key engineer may leave mid-project", "Key engineer may leave mid-project": "KEY ENGINEER MAY LEAVE M-PROJECT-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w risk_management_risk-management -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real risk tools. your risk register for identification, your project analytics for severity scoring, Slack or PagerDuty for escalation routing, and the workflow runs identically in production.

- **IdentifyWorker** (`rkm_identify`): connect to your project management tool (Jira, Asana, Monday.com) to pull risk details and auto-categorize using historical data
- **AssessWorker** (`rkm_assess`): integrate a risk scoring model or lookup table that calculates severity from probability and impact matrices
- **HighWorker** (`rkm_high`): trigger real escalation: create a Jira P1 ticket, send a Slack message to the executive channel, schedule an emergency review meeting via Google Calendar API
- **MediumWorker** (`rkm_medium`): assign a risk owner in your PM tool and create a follow-up task with a review deadline
- **MitigateWorker** (`rkm_mitigate`): generate mitigation plans using templates from a knowledge base or invoke an LLM to draft context-aware response strategies

Change probability models or mitigation strategies and the risk pipeline adapts without structural changes.

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
risk-management-risk-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/riskmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RiskManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── HighWorker.java
│       ├── IdentifyWorker.java
│       ├── LowWorker.java
│       ├── MediumWorker.java
│       └── MitigateWorker.java
└── src/test/java/riskmanagement/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── HighWorkerTest.java        # 2 tests
    ├── IdentifyWorkerTest.java        # 2 tests
    ├── LowWorkerTest.java        # 2 tests
    ├── MediumWorkerTest.java        # 2 tests
    └── MitigateWorkerTest.java        # 2 tests

```
