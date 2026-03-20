# Implementing Intrusion Detection in Java with Conductor :  Event Analysis, Threat Correlation, Severity Assessment, and Response

A Java Conductor workflow example for intrusion detection .  analyzing security events, correlating them with known threat patterns, assessing severity, and triggering automated response actions.

## The Problem

You detect a suspicious event .  an SSH login from an unusual IP, a port scan, an anomalous database query pattern. You need to analyze the event in context, correlate it with other events and known threat indicators (is this IP on a threat feed?), assess the severity (isolated event vs coordinated attack), and trigger the appropriate response (block IP, isolate host, alert SOC).

Without orchestration, intrusion detection is either a SIEM that generates thousands of uncorrelated alerts or a manual investigation process. Security analysts manually check threat feeds, correlate events across tools, and copy-paste IOCs between systems. Response is delayed because each step depends on the previous one and they're done sequentially by a human.

## The Solution

**You just write the event analysis and threat correlation logic. Conductor handles sequential execution, automatic retries if a threat feed is down, and a complete forensic timeline of every detection.**

Each detection step is an independent worker .  event analysis, threat correlation, severity assessment, and automated response. Conductor runs them in sequence: analyze the event, correlate with threat intelligence, assess severity, then trigger the response. Every detection is tracked with full context ,  event details, correlation results, severity score, and actions taken. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

The detection pipeline chains four focused workers: AnalyzeEventsWorker parses security events, CorrelateThreatsWorker matches against threat feeds, AssessSeverityWorker scores the risk, and RespondWorker triggers automated containment.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeEventsWorker** | `id_analyze_events` | Analyzes security events from a source IP, identifying suspicious patterns like repeated auth failures |
| **AssessSeverityWorker** | `id_assess_severity` | Assesses threat severity (e.g., active brute-force from a known malicious IP) |
| **CorrelateThreatsWorker** | `id_correlate_threats` | Correlates the event IP against threat intelligence feeds for known indicators |
| **RespondWorker** | `id_respond` | Executes automated response actions .  blocks the IP and notifies the security team |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
id_analyze_events
    │
    ▼
id_correlate_threats
    │
    ▼
id_assess_severity
    │
    ▼
id_respond
```

## Example Output

```
=== Example 359: Intrusion Detectio ===

Step 1: Registering task definitions...
  Registered: id_analyze_events, id_correlate_threats, id_assess_severity, id_respond

Step 2: Registering workflow 'intrusion_detection_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze] 198.51.100.42: repeated-auth-failure .  47 suspicious events
  [severity] Critical threat: active brute-force from known malicious IP
  [correlate] IP matched known threat intelligence feed
  [respond] Blocked IP, notified security team

  Status: COMPLETED

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/intrusion-detection-1.0.0.jar
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
java -jar target/intrusion-detection-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow intrusion_detection_workflow \
  --version 1 \
  --input '{"sourceIp": "sample-sourceIp", "198.51.100.42": "sample-198.51.100.42", "eventType": "standard"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w intrusion_detection_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one stage of the detection pipeline .  connect AnalyzeEventsWorker to your SIEM (Splunk, Elastic), CorrelateThreatsWorker to threat feeds like VirusTotal, and the detection-to-response workflow stays the same.

- **AnalyzeEventsWorker** (`id_analyze_events`): parse security events from your SIEM (Splunk, Elastic SIEM, QRadar) or log aggregator
- **AssessSeverityWorker** (`id_assess_severity`): compute severity using MITRE ATT&CK mapping, asset criticality, and attack chain analysis
- **CorrelateThreatsWorker** (`id_correlate_threats`): check events against threat intelligence feeds (VirusTotal, AlienVault OTX, MISP) for IOC matches

Point each worker at your real SIEM and threat feeds, and the detect-correlate-respond pipeline keeps working as-is.

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
intrusion-detection-intrusion-detection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/intrusiondetection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeEventsWorker.java
│       ├── AssessSeverityWorker.java
│       ├── CorrelateThreatsWorker.java
│       └── RespondWorker.java
└── src/test/java/intrusiondetection/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
