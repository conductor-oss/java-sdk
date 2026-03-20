# Root Cause Analysis in Java Using Conductor :  Issue Detection, Evidence Collection, Analysis, and Root Cause Identification

A Java Conductor workflow example for automated root cause analysis .  detecting an issue, collecting evidence from logs/metrics/traces, analyzing correlations, and identifying the most likely root cause.

## The Problem

An incident is happening .  high error rate, latency spike, service degradation. You need to find the root cause fast. This requires detecting the specific issue, collecting evidence from multiple sources (logs, metrics, traces, recent deployments), analyzing correlations (did the error rate spike after a deployment? does the latency correlate with CPU usage?), and identifying the root cause. Each step feeds the next ,  you can't analyze without evidence, and evidence collection depends on knowing what issue to investigate.

Without orchestration, root cause analysis is manual .  an engineer opens 5 dashboards, searches logs, checks recent deployments, and pieces together the story. This takes 30-60 minutes per incident. Automated RCA scripts exist but don't coordinate: one collects metrics, another parses logs, but they don't feed results to a common analysis step.

## The Solution

**You just write the evidence collection queries and correlation analysis logic. Conductor handles the detect-collect-analyze-identify sequence, retries when log or metric sources are temporarily unavailable, and a complete record of every RCA session's evidence and conclusions.**

Each RCA step is an independent worker .  issue detection, evidence collection, correlation analysis, and root cause identification. Conductor runs them in sequence: detect the issue, collect evidence, analyze correlations, then identify the root cause. Every RCA run is tracked ,  you can see what evidence was collected, what correlations were found, and what root cause was identified. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers automate RCA: DetectIssueWorker identifies the incident scope, CollectEvidenceWorker gathers logs/metrics/deployment data, an AnalyzeCorrelationsWorker finds patterns, and IdentifyRootCauseWorker pinpoints the most likely cause with a remediation recommendation.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEvidenceWorker** | `rca_collect_evidence` | Gathers evidence from logs, metrics, and recent deployment changes for the affected services |
| **DetectIssueWorker** | `rca_detect_issue` | Identifies the incident's time window, related services, and impact level from the reported symptom |
| **IdentifyRootCauseWorker** | `rca_identify_root_cause` | Confirms the top candidate root cause based on confidence score and suggests a remediation action |
| **RcaAnalyzeWorker** | `rca_analyze` | Analyzes collected logs and metrics to identify the most likely root cause candidate with a confidence percentage |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
rca_detect_issue
    │
    ▼
rca_collect_evidence
    │
    ▼
rca_analyze
    │
    ▼
rca_identify_root_cause
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
java -jar target/root-cause-analysis-1.0.0.jar
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
java -jar target/root-cause-analysis-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow root_cause_analysis_425 \
  --version 1 \
  --input '{"incidentId": "TEST-001", "affectedService": "test-value", "symptom": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w root_cause_analysis_425 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one RCA phase .  connect the evidence collector to Splunk and CloudWatch, the analyzer to correlate metrics with recent deployments, and the detect-collect-analyze-identify workflow stays the same.

- **CollectEvidenceWorker** (`rca_collect_evidence`): pull logs from Elasticsearch, metrics from Prometheus, traces from Jaeger, and recent deployments from CI/CD
- **DetectIssueWorker** (`rca_detect_issue`): query your monitoring system (Datadog, PagerDuty, Prometheus alerts) for the active incident details
- **IdentifyRootCauseWorker** (`rca_identify_root_cause`): rank candidate root causes by likelihood and evidence strength, or use ML-based causality analysis

Connect to your real SIEM, metrics, and deployment history, and the automated RCA pipeline operates in production without any workflow changes.

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
root-cause-analysis/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/rootcauseanalysis/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RootCauseAnalysisExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectEvidenceWorker.java
│       ├── DetectIssueWorker.java
│       ├── IdentifyRootCauseWorker.java
│       └── RcaAnalyzeWorker.java
└── src/test/java/rootcauseanalysis/workers/
    └── RcaAnalyzeWorkerTest.java        # 2 tests
```
