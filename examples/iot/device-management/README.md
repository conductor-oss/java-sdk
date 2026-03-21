# IoT Device Lifecycle Management in Java with Conductor: Registration, Provisioning, and Health Monitoring

You have 10,000 temperature sensors deployed across 200 facilities, all running firmware v2.1. A critical security vulnerability is discovered in v2.1's MQTT authentication. an attacker on the same network can spoof telemetry data. The patch is in v2.3, and it needs to roll out now. But 1,400 of those sensors are on flaky cellular connections in rural warehouses. Push the update too aggressively and you brick sensors that lose connectivity mid-flash. Skip them and you leave 1,400 attack vectors in the field. You need an update pipeline that tracks each device's state, retries on connection drops, and never leaves a sensor half-updated. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full device lifecycle, registration, provisioning, configuration, health monitoring, and firmware updates, as independent workers.

## Why Device Onboarding Needs Orchestration

Bringing a new IoT device online requires a strict sequence of steps that each depend on the previous one. You register the device in your fleet registry to get a registration ID. You use that ID to provision TLS certificates and an MQTT endpoint. You use those credentials to push telemetry configuration. Reporting intervals, topic subscriptions, device shadow creation. Only then can you start health monitoring (battery level, signal strength, uptime) and check whether the device needs a firmware update.

If provisioning fails halfway through, you need to know exactly which step succeeded so you can resume without re-registering or issuing duplicate certificates. If health monitoring reveals a degraded device, you need that signal to flow into the firmware update step. Without orchestration, you'd build this as a monolithic onboarding script. Manually threading credentials between steps, wrapping every call in retry logic, and writing state-tracking code so you can recover from partial failures. That script becomes the single point of failure for your entire fleet onboarding process.

## How This Workflow Solves It

**You just write the device lifecycle workers. Registration, credential provisioning, telemetry configuration, health monitoring, and firmware updates. Conductor handles strict onboarding sequencing, MQTT broker retries, and durable state tracking so partial failures resume exactly where they stopped.**

Each lifecycle stage is a standalone worker that does one thing. Register, provision, configure, monitor, or update. Conductor chains them together, passes each worker's output as the next worker's input, retries transient failures (network timeouts to your device registry, MQTT broker unavailability), and resumes from the exact step that failed if the process crashes. You get durable, observable device onboarding without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the device lifecycle: RegisterDeviceWorker adds the device to the fleet registry, ProvisionWorker issues TLS credentials, ConfigureWorker sets telemetry parameters, MonitorHealthWorker checks battery and signal strength, and PushUpdateWorker delivers firmware patches.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureWorker** | `dev_configure` | Configures device settings including reporting interval and telemetry topics. |
| **MonitorHealthWorker** | `dev_monitor_health` | Monitors device health status. |
| **ProvisionWorker** | `dev_provision` | Provisions credentials and connectivity for a device. |
| **PushUpdateWorker** | `dev_push_update` | Checks for and pushes firmware updates to a device. |
| **RegisterDeviceWorker** | `dev_register_device` | Registers a new IoT device in the device registry. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs, the workflow and alerting logic stay the same.

### The Workflow

```
Input -> ConfigureWorker -> MonitorHealthWorker -> ProvisionWorker -> PushUpdateWorker -> RegisterDeviceWorker -> Output

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
java -jar target/device-management-1.0.0.jar

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
java -jar target/device-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow device_management \
  --version 1 \
  --input '{"deviceId": "DEV-532-TEMP-001", "deviceType": "temperature_sensor", "fleetId": "FLEET-WAREHOUSE", "firmwareVersion": "2.4.1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w device_management -s COMPLETED -c 5

```

## How to Extend

Connect RegisterDeviceWorker to your IoT device registry (AWS IoT Core, Azure IoT Hub), ProvisionWorker to your certificate authority, and MonitorHealthWorker to your device health dashboard. The workflow definition stays exactly the same.

- **RegisterDeviceWorker** (`dev_register_device`): call AWS IoT Core `RegisterThing`, Azure IoT Hub device provisioning, or your own device registry API to create the device record and get a registration ID
- **ProvisionWorker** (`dev_provision`): generate X.509 certificates via AWS IoT, provision MQTT credentials, and attach IoT policies for topic-level access control
- **ConfigureWorker** (`dev_configure`): push reporting interval and telemetry topic subscriptions to the device shadow (AWS IoT Device Shadow, Azure Device Twin) and confirm the shadow was created
- **MonitorHealthWorker** (`dev_monitor_health`): query real device telemetry (battery level, signal strength, last-seen timestamp) from your time-series store or device shadow
- **PushUpdateWorker** (`dev_push_update`): check your firmware repository for the latest version, trigger an OTA update job if the device is behind, and track rollout status

Wire each worker to your IoT platform or device registry while preserving output fields, and the onboarding pipeline adapts seamlessly.

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
device-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/devicemanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DeviceManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfigureWorker.java
│       ├── MonitorHealthWorker.java
│       ├── ProvisionWorker.java
│       ├── PushUpdateWorker.java
│       └── RegisterDeviceWorker.java
└── src/test/java/devicemanagement/workers/
    ├── ConfigureWorkerTest.java        # 8 tests
    ├── MonitorHealthWorkerTest.java        # 8 tests
    ├── ProvisionWorkerTest.java        # 8 tests
    ├── PushUpdateWorkerTest.java        # 8 tests
    └── RegisterDeviceWorkerTest.java        # 8 tests

```
