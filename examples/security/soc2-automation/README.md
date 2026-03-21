# Implementing SOC2 Automation in Java with Conductor :  Control Collection, Effectiveness Testing, Exception Identification, and Evidence Generation

A Java Conductor workflow example for automating SOC2 compliance .  collecting control implementations, testing their effectiveness, identifying exceptions, and generating example evidence collection workflow.

## The Problem

SOC2 audits require demonstrating that security controls are implemented and effective across trust service criteria (security, availability, confidentiality). For each control, you must collect evidence of implementation, test that the control actually works (not just documented), identify any exceptions or gaps, and package everything for the auditor. This happens continuously, not just at audit time.

Without orchestration, SOC2 compliance is a frantic evidence-gathering exercise before each audit. Someone screenshots configurations from 15 different consoles, writes narratives explaining each control, and assembles a 200-page evidence package. Controls that looked effective in the screenshot may have been misconfigured yesterday.

## The Solution

Each SOC2 step is an independent worker .  control collection, effectiveness testing, exception identification, and evidence generation. Conductor runs them in sequence: collect controls, test effectiveness, identify exceptions, then generate evidence. Every compliance cycle is tracked, building a continuous compliance record rather than a point-in-time snapshot. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers automate the SOC2 cycle: CollectControlsWorker gathers control implementations across trust service criteria, IdentifyExceptionsWorker flags gaps like missing MFA enforcement, and GenerateEvidenceWorker assembles audit-ready packages for the auditor.

| Worker | Task | What It Does |
|---|---|---|
| **CollectControlsWorker** | `soc2_collect_controls` | Collects all SOC 2 controls for the specified trust service category (e.g., security) |
| **GenerateEvidenceWorker** | `soc2_generate_evidence` | Generates an evidence package for auditors from test results and exception data |
| **IdentifyExceptionsWorker** | `soc2_identify_exceptions` | Identifies control exceptions (e.g., MFA enforcement gap, backup testing overdue) |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
soc2_collect_controls
    │
    ▼
soc2_test_effectiveness
    │
    ▼
soc2_identify_exceptions
    │
    ▼
soc2_generate_evidence

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
java -jar target/soc2-automation-1.0.0.jar

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
java -jar target/soc2-automation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow soc2_automation_workflow \
  --version 1 \
  --input '{"trustServiceCriteria": "order-service", "period": "sample-period"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w soc2_automation_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker tackles one compliance phase .  connect CollectControlsWorker to pull from AWS Config and Kubernetes policies, GenerateEvidenceWorker to compile packages in Vanta or Drata, and the collect-test-evidence workflow stays the same.

- **CollectControlsWorker** (`soc2_collect_controls`): query real control implementations. AWS Config rules, Kubernetes policies, IAM configurations .  via their APIs
- **GenerateEvidenceWorker** (`soc2_generate_evidence`): compile evidence packages for auditors .  screenshots, configuration exports, test results ,  in a GRC platform (Vanta, Drata)
- **IdentifyExceptionsWorker** (`soc2_identify_exceptions`): compare test results against SOC2 criteria, flag gaps, and compute a compliance score per trust service criterion

Wire each worker to your GRC platform and cloud config APIs, and the continuous compliance orchestration keeps running with no workflow adjustments.

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
soc2-automation-soc2-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/soc2automation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectControlsWorker.java
│       ├── GenerateEvidenceWorker.java
│       └── IdentifyExceptionsWorker.java
└── src/test/java/soc2automation/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
