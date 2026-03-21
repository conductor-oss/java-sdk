# Incident AI in Java with Conductor :  Detect, Diagnose, Fix, and Verify Production Incidents

A Java Conductor workflow that handles production incidents end-to-end. detecting an anomaly from an alert, diagnosing the root cause, suggesting a fix, executing the remediation, and verifying the service has recovered. Given an `alertId`, `serviceName`, and `severity`, the pipeline produces a diagnosis, fix action, execution result, and verification status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-step incident response pipeline.

## Responding to Incidents Faster Than Humans Can

When a production alert fires at 3 AM, the response time depends on how fast an engineer can wake up, read the alert, diagnose the issue, apply a fix, and verify recovery. Automating this pipeline reduces mean time to resolution (MTTR) from hours to minutes. Each step in the response chain builds on the previous one. diagnosis requires the detection context, the suggested fix depends on the diagnosis, execution depends on the fix plan, and verification confirms the fix actually worked.

This workflow models the incident response lifecycle. The detector analyzes the incoming alert. The diagnoser identifies the root cause from the detection output. The fix suggester proposes a remediation based on the diagnosis. The executor applies the fix. The verifier checks that the service has recovered. The five-step chain ensures every incident is handled systematically, with full traceability from alert to resolution.

## The Solution

**You just write the detection, diagnosis, fix-suggestion, execution, and verification workers. Conductor handles the five-step incident response chain.**

Five workers handle the incident lifecycle. detection, diagnosis, fix suggestion, fix execution, and verification. The detector correlates alert data. The diagnoser identifies root causes. The fix suggester proposes remediations. The executor applies the chosen fix. The verifier confirms service recovery. Conductor sequences the five steps and passes alert context, diagnoses, fix plans, and execution results between them automatically.

### What You Write: Workers

DetectWorker correlates alert data, DiagnoseWorker identifies root causes, SuggestFixWorker proposes remediation, ExecuteFixWorker applies it, and VerifyWorker confirms service recovery. Five steps from alert to resolution.

| Worker | Task | What It Does |
|---|---|---|
| **DetectWorker** | `iai_detect` | Correlates the incoming alert with service health data and determines the anomaly type. |
| **DiagnoseWorker** | `iai_diagnose` | Identifies the root cause of the detected anomaly (e.g., memory leak, traffic spike, dependency failure). |
| **ExecuteFixWorker** | `iai_execute_fix` | Applies the suggested remediation (restart, scale up, roll back, etc.). |
| **SuggestFixWorker** | `iai_suggest_fix` | Proposes a remediation action based on the root cause diagnosis. |
| **VerifyWorker** | `iai_verify` | Checks that the service has recovered after the fix by running health checks. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
iai_detect
    │
    ▼
iai_diagnose
    │
    ▼
iai_suggest_fix
    │
    ▼
iai_execute_fix
    │
    ▼
iai_verify

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
java -jar target/incident-ai-1.0.0.jar

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
java -jar target/incident-ai-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow iai_incident_ai \
  --version 1 \
  --input '{"alertId": "TEST-001", "serviceName": "test", "severity": "sample-severity"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w iai_incident_ai -s COMPLETED -c 5

```

## How to Extend

Each worker handles one incident response step. connect your alerting system (PagerDuty, OpsGenie) for detection and your infrastructure API (AWS, Kubernetes) for remediation execution, and the incident-response workflow stays the same.

- **DetectWorker** (`iai_detect`): integrate with PagerDuty, OpsGenie, or Datadog for real alert correlation and anomaly detection
- **DiagnoseWorker** (`iai_diagnose`): connect to logging (ELK, Splunk) and tracing (Jaeger, Zipkin) systems for real root cause analysis
- **ExecuteFixWorker** (`iai_execute_fix`): integrate with Kubernetes or Terraform to execute real remediations (restarts, rollbacks, scaling)

Connect your observability stack and runbook automation and the five-step incident response chain continues to function as designed.

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
incident-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/incidentai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IncidentAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectWorker.java
│       ├── DiagnoseWorker.java
│       ├── ExecuteFixWorker.java
│       ├── SuggestFixWorker.java
│       └── VerifyWorker.java
└── src/test/java/incidentai/workers/
    ├── DetectWorkerTest.java        # 2 tests
    ├── DiagnoseWorkerTest.java        # 2 tests
    ├── ExecuteFixWorkerTest.java        # 2 tests
    ├── SuggestFixWorkerTest.java        # 2 tests
    └── VerifyWorkerTest.java        # 2 tests

```
