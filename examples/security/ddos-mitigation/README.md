# Implementing DDoS Mitigation in Java with Conductor :  Traffic Detection, Attack Classification, Mitigation, and Service Verification

A Java Conductor workflow example for DDoS mitigation .  detecting abnormal traffic patterns, classifying the attack type (volumetric, protocol, application-layer), applying mitigation measures, and verifying that the protected service is still available.

## The Problem

Your service is under a DDoS attack .  traffic volume is spiking abnormally. You need to detect the anomaly, classify the attack type (SYN flood vs HTTP flood vs DNS amplification ,  each requires different mitigation), apply the right countermeasures (rate limiting, traffic scrubbing, blackholing), and verify that legitimate traffic can still reach the service after mitigation is applied.

Without orchestration, DDoS response is a runbook executed by an on-call engineer under pressure. They log into the CDN console, enable rate limiting, check if the service is still up, and adjust settings manually. Each attack is handled slightly differently, response time is measured in minutes, and there's no audit trail of what mitigation was applied and when.

## The Solution

**You just write the traffic detection and mitigation rules. Conductor handles rapid sequential execution under attack conditions, retries on CDN API failures, and a timestamped record of every mitigation action and service availability check.**

Each mitigation step is an independent worker .  traffic detection, attack classification, mitigation application, and service verification. Conductor runs them in sequence: detect the anomaly, classify the attack, apply mitigation, then verify the service. Every mitigation action is tracked with traffic metrics, attack classification, and service availability status. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The mitigation chain runs DetectWorker to spot traffic anomalies, ClassifyWorker to identify the attack vector (SYN flood, HTTP flood, DNS amplification), MitigateWorker to activate rate limiting and scrubbing, and VerifyServiceWorker to confirm legitimate traffic is flowing again.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `ddos_classify` | Classifies the attack type (e.g., Layer 7 HTTP flood from botnet) |
| **DetectWorker** | `ddos_detect` | Detects anomalous traffic spikes by comparing current volume against baseline thresholds |
| **MitigateWorker** | `ddos_mitigate` | Activates mitigation controls .  rate limiting, challenge pages, and IP blocking |
| **VerifyServiceWorker** | `ddos_verify_service` | Verifies service is restored .  confirms latency is normal and legitimate traffic is flowing |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
ddos_detect
    │
    ▼
ddos_classify
    │
    ▼
ddos_mitigate
    │
    ▼
ddos_verify_service
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
java -jar target/ddos-mitigation-1.0.0.jar
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
java -jar target/ddos-mitigation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ddos_mitigation_workflow \
  --version 1 \
  --input '{"targetService": "test-value", "trafficAnomaly": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ddos_mitigation_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one mitigation phase .  connect DetectWorker to Cloudflare or AWS Shield metrics, MitigateWorker to your CDN's rate-limiting API, and the detect-classify-mitigate-verify workflow stays the same.

- **ClassifyWorker** (`ddos_classify`): classify attacks using traffic analysis .  packet types, source distribution, request patterns ,  to determine the attack vector
- **DetectWorker** (`ddos_detect`): monitor real traffic metrics from CloudFlare, AWS Shield, Akamai, or your load balancer for anomalous patterns
- **MitigateWorker** (`ddos_mitigate`): apply real mitigation via Cloudflare API (rate limiting, challenge pages), AWS Shield Advanced, or BGP blackholing

Hook up your CDN's real rate-limiting and scrubbing APIs, and the detect-classify-mitigate-verify sequence continues without any workflow edits.

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
ddos-mitigation-ddos-mitigation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ddosmitigation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── DetectWorker.java
│       ├── MitigateWorker.java
│       └── VerifyServiceWorker.java
└── src/test/java/ddosmitigation/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
