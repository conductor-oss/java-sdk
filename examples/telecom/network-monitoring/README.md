# Network Monitoring in Java Using Conductor

A Java Conductor workflow example that orchestrates telecom network monitoring .  polling metrics from a network segment, detecting issues such as high latency, packet loss, or link failures, diagnosing root causes, executing automated repairs, and verifying the network segment is healthy after repair. Uses [Conductor](https://github.## Why Network Monitoring Needs Orchestration

Monitoring a telecom network segment requires a closed-loop process from detection through resolution. You poll performance metrics (latency, jitter, packet loss, throughput) from the network segment's switches and routers. You analyze those metrics to detect issues .  threshold breaches, trending degradation, or outright failures. You diagnose the root cause by correlating the detected issues with topology, recent changes, and known failure patterns. You execute the repair ,  rerouting traffic, resetting interfaces, or rolling back a bad configuration. Finally, you verify the segment is healthy by re-polling metrics and confirming the issue is resolved.

If a repair succeeds but verification fails, the network may appear fixed while a deeper issue persists. If detection finds multiple issues but diagnosis only addresses one, the remaining issues go unresolved. Without orchestration, you'd build a monitoring loop that mixes SNMP polling, alarm correlation, and CLI-based remediation into a single script .  making it impossible to swap NMS vendors, test diagnosis rules independently, or audit which repair action resolved which alarm.

## The Solution

**You just write the metrics polling, issue detection, root cause diagnosis, automated repair, and health verification logic. Conductor handles metric collection retries, alert routing, and monitoring audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Metric collection, threshold evaluation, alert generation, and incident creation workers each own one layer of network health surveillance.

| Worker | Task | What It Does |
|---|---|---|
| **DetectIssuesWorker** | `nmn_detect_issues` | Analyzes collected metrics to detect threshold breaches, degradation trends, and link failures. |
| **DiagnoseWorker** | `nmn_diagnose` | Diagnoses root causes by correlating detected issues with network topology and recent changes. |
| **MonitorWorker** | `nmn_monitor` | Polls performance metrics (latency, jitter, packet loss, throughput) from a network segment. |
| **RepairWorker** | `nmn_repair` | Executes automated repairs .  rerouting traffic, resetting interfaces, or rolling back configurations. |
| **VerifyWorker** | `nmn_verify` | Re-polls the network segment after repair to confirm the issue is resolved and metrics are healthy. |

Workers simulate telecom operations .  provisioning, activation, billing ,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
nmn_monitor
    │
    ▼
nmn_detect_issues
    │
    ▼
nmn_diagnose
    │
    ▼
nmn_repair
    │
    ▼
nmn_verify
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
java -jar target/network-monitoring-1.0.0.jar
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
java -jar target/network-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nmn_network_monitoring \
  --version 1 \
  --input '{"networkSegment": "test-value", "checkType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nmn_network_monitoring -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real network tools. SNMP or gNMI for metrics polling, your alarm correlation engine for diagnosis, your NMS CLI for automated repair, and the workflow runs identically in production.

- **MonitorWorker** (`nmn_monitor`): poll network element metrics via SNMP (v2c/v3), gNMI streaming telemetry, or your NMS API (Nokia NSP, Ericsson ENM, Huawei iMaster NCE)
- **DetectIssuesWorker** (`nmn_detect_issues`): run threshold and anomaly detection against collected metrics using your alarm management system or an AIOps platform (Moogsoft, BigPanda)
- **DiagnoseWorker** (`nmn_diagnose`): correlate alarms with topology data from your inventory system and recent change records from your OSS to identify root cause
- **RepairWorker** (`nmn_repair`): execute automated remediation via NETCONF/RESTCONF to the affected network elements, or trigger a workflow in your OSS activation platform
- **VerifyWorker** (`nmn_verify`): re-poll the segment's KPIs via the same telemetry path and confirm all metrics are within acceptable thresholds

Update monitoring thresholds or alerting tools and the pipeline continues operating identically.

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
network-monitoring-network-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/networkmonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NetworkMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectIssuesWorker.java
│       ├── DiagnoseWorker.java
│       ├── MonitorWorker.java
│       ├── RepairWorker.java
│       └── VerifyWorker.java
└── src/test/java/networkmonitoring/workers/
    ├── MonitorWorkerTest.java        # 1 tests
    └── RepairWorkerTest.java        # 1 tests
```
