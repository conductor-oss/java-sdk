# Agriculture IoT in Java with Conductor

A Java Conductor workflow example that orchestrates precision agriculture. reading soil moisture and pH from field sensors, fetching weather forecasts for rain probability and temperature, making irrigation decisions based on soil conditions, crop type, and weather outlook, and actuating irrigation valves across specific field zones for a calculated duration. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Smart Irrigation Needs Orchestration

Deciding whether to irrigate a field requires data from multiple sources fed through a decision pipeline. You read soil sensors to get current moisture levels and pH for the crop type. You fetch weather data to check temperature and rain probability. irrigating before a rainstorm wastes water and can damage crops. You feed soil and weather data into an irrigation decision engine that determines whether to irrigate, for how long, and which zones. Finally, you actuate the irrigation valves for the selected zones and duration.

Each step depends on the previous one. the decision engine needs both soil and weather data, and the actuator needs the decision output. If a soil sensor read fails, you need to retry without re-fetching weather data that is still valid. Without orchestration, you'd build a monolithic irrigation controller that mixes sensor polling, weather API calls, decision logic, and valve control,  making it impossible to swap weather providers, test irrigation rules independently, or audit which sensor readings triggered which irrigation events.

## The Solution

**You just write the precision agriculture workers. Soil sensor reads, weather data fetches, irrigation decisions, and valve actuation. Conductor handles sensor-to-actuator sequencing, weather API retries, and timestamped records linking readings to irrigation events.**

Each worker handles one IoT operation. data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Four workers manage precision irrigation: SoilSensorsWorker reads moisture and pH, WeatherDataWorker fetches forecasts, IrrigationDecisionWorker evaluates whether to irrigate, and ActuateWorker opens field valves for the calculated duration.

| Worker | Task | What It Does |
|---|---|---|
| **ActuateWorker** | `agr_actuate` | Sends irrigation commands to field valves for specified zones and duration. |
| **IrrigationDecisionWorker** | `agr_irrigation_decision` | Evaluates soil moisture, pH, weather forecast, and crop type to decide whether to irrigate, for how long, and which zones. |
| **SoilSensorsWorker** | `agr_soil_sensors` | Reads soil moisture and pH levels from field sensors for a given crop type. |
| **WeatherDataWorker** | `agr_weather_data` | Fetches current temperature and rain probability forecast for the field location. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
agr_soil_sensors
    │
    ▼
agr_weather_data
    │
    ▼
agr_irrigation_decision
    │
    ▼
agr_actuate

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
java -jar target/agriculture-iot-1.0.0.jar

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
java -jar target/agriculture-iot-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow agriculture_iot_demo \
  --version 1 \
  --input '{"fieldId": "TEST-001", "cropType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agriculture_iot_demo -s COMPLETED -c 5

```

## How to Extend

Connect SoilSensorsWorker to your field sensor gateway, WeatherDataWorker to a weather API (OpenWeatherMap, Tomorrow.io), and ActuateWorker to your irrigation controller. The workflow definition stays exactly the same.

- **SoilSensorsWorker** (`agr_soil_sensors`): read real soil data from IoT sensors via MQTT, LoRaWAN gateways, or a precision agriculture platform (John Deere Operations Center, Arable, CropX)
- **WeatherDataWorker** (`agr_weather_data`): call a weather API (OpenWeatherMap, Tomorrow.io, NOAA) to get real temperature and rain probability forecasts for the field's GPS coordinates
- **IrrigationDecisionWorker** (`agr_irrigation_decision`): implement real agronomic decision logic using crop water requirements tables, evapotranspiration models (Penman-Monteith), or a farm management decision engine
- **ActuateWorker** (`agr_actuate`): send valve control commands to your irrigation controller (Lindsay FieldNET, Valley AgSense, Netafim) via API or MQTT to open/close zone valves for the calculated duration

Connect each worker to your sensor gateway or weather API while maintaining output fields, and the irrigation pipeline adapts without modification.

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
agriculture-iot/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agricultureiot/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgricultureIotExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActuateWorker.java
│       ├── IrrigationDecisionWorker.java
│       ├── SoilSensorsWorker.java
│       └── WeatherDataWorker.java
└── src/test/java/agricultureiot/workers/
    ├── SoilSensorsWorkerTest.java        # 2 tests
    └── WeatherDataWorkerTest.java        # 2 tests

```
