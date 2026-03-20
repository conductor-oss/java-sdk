# IoT Security in Java with Conductor :  Network Scanning, Vulnerability Detection, Automated Patching, and Verification

A Java Conductor workflow example that orchestrates IoT security operations .  scanning the device network to discover connected devices, detecting vulnerabilities on each device (expired certificates, default credentials, known CVEs), pushing security patches to affected devices, and verifying all devices pass a post-patch security check. Uses [Conductor](https://github.## Why IoT Security Workflows Need Orchestration

Securing an IoT network is a strict pipeline where skipping a step or executing out of order creates real risk. You scan the network to discover all connected devices .  cameras, sensors, gateways. You run vulnerability detection against the inventory to find devices with expired TLS certificates, default passwords, or firmware versions affected by known CVEs. You push security patches to the specific affected devices. After patching, you re-verify that every device on the network passes the security check.

If the vulnerability scanner finds issues on some devices but the patch delivery fails partway through, you need to know exactly which devices were patched and which still need attention. If you skip verification after patching, you cannot confirm the vulnerability was actually remediated. Without orchestration, you'd build a monolithic security script that mixes network discovery, CVE database lookups, patch distribution, and verification .  making it impossible to audit which vulnerability triggered which patch or to retry a failed patch without rescanning the entire network.

## How This Workflow Solves It

**You just write the IoT security workers. Network scanning, vulnerability detection, patch deployment, and remediation verification. Conductor handles scan-to-verify sequencing, patch delivery retries, and audit trails linking every vulnerability to its remediation proof.**

Each security operation is an independent worker .  scan devices, detect vulnerabilities, apply patches, verify remediation. Conductor sequences them strictly, passes the affected device list from detection to patching, retries if a patch delivery times out, and maintains a complete audit trail linking every vulnerability to its patch and verification result.

### What You Write: Workers

Four workers secure the IoT network: ScanDevicesWorker discovers connected devices, DetectVulnerabilitiesWorker checks for CVEs and expired certificates, PatchWorker deploys security fixes, and VerifyWorker confirms remediation succeeded.

| Worker | Task | What It Does |
|---|---|---|
| **DetectVulnerabilitiesWorker** | `ios_detect_vulnerabilities` | Checks discovered devices for expired certificates, default credentials, and known CVEs. |
| **PatchWorker** | `ios_patch` | Pushes security patches to affected devices and tracks patch application status. |
| **ScanDevicesWorker** | `ios_scan_devices` | Scans the IoT network to discover all connected devices and their firmware versions. |
| **VerifyWorker** | `ios_verify` | Re-scans patched devices to confirm vulnerabilities are remediated and security baseline is met. |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs .  the workflow and alerting logic stay the same.

### The Workflow

```
ios_scan_devices
    │
    ▼
ios_detect_vulnerabilities
    │
    ▼
ios_patch
    │
    ▼
ios_verify
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
java -jar target/iot-security-1.0.0.jar
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
java -jar target/iot-security-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow iot_security_demo \
  --version 1 \
  --input '{"networkId": "TEST-001", "scanDepth": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w iot_security_demo -s COMPLETED -c 5
```

## How to Extend

Connect ScanDevicesWorker to your network discovery tool (Nmap, IoT Inspector), DetectVulnerabilitiesWorker to your CVE database, and PatchWorker to your device management platform for OTA patching. The workflow definition stays exactly the same.

- **ScanDevicesWorker** (`ios_scan_devices`): run a real network scan using Nmap, your device registry API, or an IoT platform inventory (AWS IoT Device Defender, Azure IoT Hub) to discover all connected devices
- **DetectVulnerabilitiesWorker** (`ios_detect_vulnerabilities`): check each device against a CVE database (NVD API), validate TLS certificates, test for default credentials, and flag devices running vulnerable firmware versions
- **PatchWorker** (`ios_patch`): push security patches via your OTA update mechanism, rotate device certificates, or force credential resets on affected devices
- **VerifyWorker** (`ios_verify`): re-scan patched devices to confirm vulnerabilities are remediated, certificates are valid, and all devices pass your security compliance baseline

Wire each worker to your vulnerability scanner or patch management system while keeping the same return schema, and the security pipeline stays intact.

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
iot-security/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/iotsecurity/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IotSecurityExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectVulnerabilitiesWorker.java
│       ├── PatchWorker.java
│       ├── ScanDevicesWorker.java
│       └── VerifyWorker.java
└── src/test/java/iotsecurity/workers/
    ├── DetectVulnerabilitiesWorkerTest.java        # 2 tests
    └── ScanDevicesWorkerTest.java        # 2 tests
```
