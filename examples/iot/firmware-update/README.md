# OTA Firmware Update in Java with Conductor :  Version Check, Download, Validation, Deployment, and Verification

A Java Conductor workflow example that orchestrates over-the-air firmware updates for IoT devices .  checking for new versions, downloading firmware binaries, validating checksums and code signatures, deploying to the device with scheduled reboots, and verifying the device boots successfully on the new version. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Firmware Updates Need Orchestration

Pushing a firmware update to an IoT device is a multi-step process where failure at any stage can brick the device. You check whether a newer version exists and get the download URL and SHA-256 checksum. You download the binary (potentially over a slow, unreliable link). You validate the checksum matches, the code signature is authentic, and the firmware is compatible with the device hardware. You deploy the firmware and schedule a reboot. After reboot, you verify the device came back online running the correct version, the self-test passed, and a rollback image is available.

If the download fails at 80%, you need to resume without restarting. If checksum validation fails, you must not proceed to deployment. If the device does not come back after reboot, you need to know exactly where the process stopped so you can trigger a rollback. Without orchestration, you'd build a brittle OTA updater that mixes network code, cryptographic validation, device management, and recovery logic in one class .  making it dangerous to change and impossible to audit.

## How This Workflow Solves It

**You just write the firmware update workers. Version checking, binary download, checksum validation, deployment, and post-reboot verification. Conductor handles strict validation-before-deployment gating, download resume on failure, and exact state recording so bricked devices can be diagnosed.**

Each firmware update stage is an independent worker .  check version, download, validate, deploy, verify. Conductor sequences them strictly, ensures a failed checksum validation stops the pipeline before deployment, retries interrupted downloads automatically, and records the exact state of every update attempt. If the process crashes after deployment but before verification, Conductor resumes at the verify step rather than re-deploying.

### What You Write: Workers

Five workers manage OTA updates: CheckVersionWorker detects available firmware, DownloadWorker fetches the binary, ValidateWorker verifies checksums and code signatures, DeployWorker pushes to the device, and VerifyWorker confirms successful boot on the new version.

| Worker | Task | What It Does |
|---|---|---|
| **CheckVersionWorker** | `fw_check_version` | Checks whether a newer firmware version exists and returns the download URL and SHA-256 checksum. |
| **DeployWorker** | `fw_deploy` | Pushes the validated firmware to the device and schedules a reboot. |
| **DownloadWorker** | `fw_download` | Downloads the firmware binary from the repository to local staging. |
| **ValidateWorker** | `fw_validate` | Verifies the firmware checksum, code signature, and hardware compatibility. |
| **VerifyWorker** | `fw_verify` | Confirms the device rebooted successfully on the new firmware version and self-test passed. |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs .  the workflow and alerting logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
fw_check_version
    │
    ▼
fw_download
    │
    ▼
fw_validate
    │
    ▼
fw_deploy
    │
    ▼
fw_verify
```

## Example Output

```
=== Example 533: Firmware Update ===

Step 1: Registering task definitions...
  Registered: fw_check_version, fw_download, fw_validate, fw_deploy, fw_verify

Step 2: Registering workflow 'firmware_update_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [check] Processing
  [deploy] Processing
  [download] Processing
  [validate] Processing
  [verify] Processing

  Status: COMPLETED
  Output: {updateAvailable=..., downloadUrl=..., checksum=..., releaseNotes=...}

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
java -jar target/firmware-update-1.0.0.jar
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
java -jar target/firmware-update-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow firmware_update_workflow \
  --version 1 \
  --input '{"deviceId": "DEV-533-GW-001", "DEV-533-GW-001": "currentVersion", "currentVersion": "2.4.1", "2.4.1": "targetVersion", "targetVersion": "2.5.0", "2.5.0": "sample-2.5.0"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w firmware_update_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CheckVersionWorker to your firmware repository, ValidateWorker to your code signing infrastructure, and DeployWorker to your OTA update service (AWS IoT Jobs, Mender, balena). The workflow definition stays exactly the same.

- **CheckVersionWorker** (`fw_check_version`): query your firmware repository (S3 bucket manifest, Artifactory, custom registry) to compare the device's current version against the latest available, returning the download URL, SHA-256 checksum, and release notes
- **DownloadWorker** (`fw_download`): download the firmware binary over HTTPS or MQTT with resumable transfers, writing to local staging and tracking download time and size
- **ValidateWorker** (`fw_validate`): verify the SHA-256 checksum matches, validate the code signature against your signing certificate, and run hardware compatibility checks against the device model
- **DeployWorker** (`fw_deploy`): push the firmware to the device via your OTA mechanism (AWS IoT Jobs, Azure Device Update, custom MQTT channel), schedule the reboot window, and record the deployment ID
- **VerifyWorker** (`fw_verify`): poll the device after reboot to confirm it is running the target version, the self-test passed, boot time is within expected range, and a rollback partition is available

Connect each worker to your firmware repository or device management platform while preserving output fields, and the update pipeline needs no changes.

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
firmware-update/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/firmwareupdate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FirmwareUpdateExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckVersionWorker.java
│       ├── DeployWorker.java
│       ├── DownloadWorker.java
│       ├── ValidateWorker.java
│       └── VerifyWorker.java
└── src/test/java/firmwareupdate/workers/
    ├── CheckVersionWorkerTest.java        # 2 tests
    └── DownloadWorkerTest.java        # 2 tests
```
