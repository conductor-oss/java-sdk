# Smart Home Automation in Java with Conductor :  Event Detection, Rule Evaluation, and Device Actuation

A Java Conductor workflow example that orchestrates smart home automation .  detecting sensor events (occupancy, temperature changes, motion), evaluating automation rules against home context (mode, current temperature), routing to the correct device actuator via SWITCH (lights, thermostat, or security system), and logging every automation event. Uses [Conductor](https://github.## Why Smart Home Automation Needs Orchestration

A smart home automation rule involves a decision chain: a sensor fires an event, the system evaluates context (is someone home? what mode is active? what is the current temperature?), and based on the matched rule, it actuates the right device .  dim the lights to 80% warm white, set the thermostat to a target temperature, or arm the security system for a specific zone. Different event types route to entirely different actuators, and every actuation must be logged for audit and debugging.

This is a natural fit for conditional routing. A motion sensor at the front door should turn on lights when the home is in "home" mode but arm the security system when in "away" mode. Without orchestration, you'd build a monolithic automation engine with a growing if/else tree that handles every sensor-to-actuator mapping, mixing event detection, rule matching, device control, and logging in one tangled class. Adding a new device type (blinds, sprinklers, garage door) means modifying the core engine. Conductor's SWITCH task handles the routing declaratively .  add a new case and a new worker, and the existing automation rules are untouched.

## How This Workflow Solves It

**You just write the smart home workers. Event detection, rule evaluation, and device-specific actuators for lights, thermostat, and security. Conductor handles SWITCH-based device routing, device unreachability retries, and logged records for every automation action.**

Each automation concern is an independent worker .  detect events, evaluate rules, actuate lights/thermostat/security, log the event. Conductor sequences detection and rule evaluation, then uses a SWITCH task to route to the correct actuator based on the matched rule. If a device is temporarily unreachable, Conductor retries the actuation. Adding a new device type means adding a new SWITCH case and a new worker ,  no changes to existing code.

### What You Write: Workers

Six workers handle home automation: DetectEventWorker captures sensor events, EvaluateRulesWorker matches automation rules, ActuateLightsWorker controls brightness, ActuateThermostatWorker sets temperature, ActuateSecurityWorker arms zones, and LogEventWorker records every action.

| Worker | Task | What It Does |
|---|---|---|
| **ActuateLightsWorker** | `smh_actuate_lights` | Controls lights .  sets brightness, color temperature, and scene for the target room. |
| **ActuateSecurityWorker** | `smh_actuate_security` | Arms or disarms the security system for a specific zone based on the automation rule. |
| **ActuateThermostatWorker** | `smh_actuate_thermostat` | Sets the thermostat to a target temperature and HVAC mode. |
| **DetectEventWorker** | `smh_detect_event` | Detects sensor events (motion, temperature change, occupancy) and enriches with home context. |
| **EvaluateRulesWorker** | `smh_evaluate_rules` | Matches the event against automation rules to determine the action type, target room, and parameters. |
| **LogEventWorker** | `smh_log_event` | Logs the automation event with action type, parameters, and outcome for audit and debugging. |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs .  the workflow and alerting logic stay the same.

### The Workflow

```
smh_detect_event
    │
    ▼
smh_evaluate_rules
    │
    ▼
SWITCH (smh_switch_ref)
    ├── lights: smh_actuate_lights
    ├── thermostat: smh_actuate_thermostat
    ├── security: smh_actuate_security
    │
    ▼
smh_log_event
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
java -jar target/smart-home-1.0.0.jar
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
java -jar target/smart-home-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow smart_home_workflow \
  --version 1 \
  --input '{"eventId": "TEST-001", "sensorId": "TEST-001", "eventType": "test-value", "value": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w smart_home_workflow -s COMPLETED -c 5
```

## How to Extend

Connect DetectEventWorker to your home hub sensor events (Z-Wave, Zigbee), EvaluateRulesWorker to your automation rules engine, and the actuator workers to your smart device APIs (Hue, Nest, Ring). The workflow definition stays exactly the same.

- **DetectEventWorker** (`smh_detect_event`): subscribe to real sensor events via Zigbee/Z-Wave hub (Home Assistant API, SmartThings), MQTT, or a cloud IoT platform and enrich with occupancy and mode context
- **EvaluateRulesWorker** (`smh_evaluate_rules`): query your rule engine or database for matching automation rules based on event type, time of day, occupancy, and home mode
- **ActuateLightsWorker** (`smh_actuate_lights`): control real lights via Philips Hue, LIFX, or Zigbee bridge API to set brightness, color temperature, and scene
- **ActuateThermostatWorker** (`smh_actuate_thermostat`): call your smart thermostat API (Nest, Ecobee, Honeywell) to set target temperature and HVAC mode
- **ActuateSecurityWorker** (`smh_actuate_security`): arm/disarm your security system via its API (Ring, SimpliSafe, ADT Pulse) with zone-specific control
- **LogEventWorker** (`smh_log_event`): write automation events to your home activity log, time-series database, or smart home dashboard for review and debugging

Connect each worker to your smart home hub or device API while preserving output fields, and adding a new device type means adding a SWITCH case and worker only.

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
smart-home/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/smarthome/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SmartHomeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActuateLightsWorker.java
│       ├── ActuateSecurityWorker.java
│       ├── ActuateThermostatWorker.java
│       ├── DetectEventWorker.java
│       ├── EvaluateRulesWorker.java
│       └── LogEventWorker.java
└── src/test/java/smarthome/workers/
    ├── DetectEventWorkerTest.java        # 2 tests
    └── EvaluateRulesWorkerTest.java        # 2 tests
```
