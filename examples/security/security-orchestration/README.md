# Implementing Security Orchestration (SOAR) in Java with Conductor :  Alert Ingestion, Enrichment, Decision, and Playbook Execution

A Java Conductor workflow example for security orchestration. ingesting security alerts, enriching with threat intelligence and asset context, making a triage decision, and executing automated response playbooks.

## The Problem

Your SOC receives hundreds of alerts daily from multiple sources. SIEM, EDR, IDS, cloud security tools. Each alert must be ingested, enriched with context (is this a known-bad IP? what asset is affected? who owns it?), triaged (true positive vs false positive, severity level), and responded to with an automated playbook (isolate host, block IP, reset credentials). Without automation, analysts manually investigate each alert, and most of their time is spent on enrichment rather than response.

Without orchestration, SOAR is a collection of scripts that run independently. Enrichment queries three different tools, the triage decision is in the analyst's head, and playbook execution is a separate runbook. There's no unified pipeline from alert to response, and no tracking of which alerts were handled and how.

## The Solution

**You just write the alert enrichment and response playbooks. Conductor handles the alert-to-playbook pipeline, retries when enrichment APIs are unavailable, and tracks mean time to respond for every incident from ingestion to resolution.**

Each SOAR step is an independent worker. alert ingestion, enrichment, triage decision, and playbook execution. Conductor runs them in sequence: ingest the alert, enrich with context, decide on action, then execute the playbook. Every alert is tracked from ingestion to resolution,  you can measure mean time to respond and audit every automated action. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The SOAR pipeline chains IngestAlertWorker to receive alerts from SIEM/EDR sources, EnrichWorker to add threat intel and asset context, DecideActionWorker to triage and select the response, and ExecutePlaybookWorker to run automated containment actions.

| Worker | Task | What It Does |
|---|---|---|
| **DecideActionWorker** | `soar_decide_action` | Determines the response actions based on enriched alert data (e.g., isolate host, block C2 domain) |
| **EnrichWorker** | `soar_enrich` | Adds threat intelligence, asset context, and user history to the raw alert |
| **ExecutePlaybookWorker** | `soar_execute_playbook` | Executes the decided response playbook. host isolation, domain blocking, forensic collection |
| **IngestAlertWorker** | `soar_ingest_alert` | Ingests a security alert from an external source (e.g., CrowdStrike, Splunk) |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
soar_ingest_alert
    │
    ▼
soar_enrich
    │
    ▼
soar_decide_action
    │
    ▼
soar_execute_playbook

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
java -jar target/security-orchestration-1.0.0.jar

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
java -jar target/security-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow security_orchestration_workflow \
  --version 1 \
  --input '{"alertId": "TEST-001", "alertSource": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w security_orchestration_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one SOAR stage. connect IngestAlertWorker to CrowdStrike or Splunk, ExecutePlaybookWorker to your firewall and EDR APIs for automated response, and the alert-enrich-decide-respond workflow stays the same.

- **DecideActionWorker** (`soar_decide_action`): apply triage rules based on severity, confidence, asset criticality, and historical false positive rates
- **EnrichWorker** (`soar_enrich`): enrich with VirusTotal reputation, asset ownership from your CMDB, user context from your IdP, and geo-IP data
- **ExecutePlaybookWorker** (`soar_execute_playbook`): run automated response. block IPs via firewall API, isolate hosts via EDR, reset passwords, create tickets

Connect to your CrowdStrike and Splunk APIs for real alerts, and the alert-to-response orchestration stays intact.

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
security-orchestration-security-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/securityorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DecideActionWorker.java
│       ├── EnrichWorker.java
│       ├── ExecutePlaybookWorker.java
│       └── IngestAlertWorker.java
└── src/test/java/securityorchestration/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
