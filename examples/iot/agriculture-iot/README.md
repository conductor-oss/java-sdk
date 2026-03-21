# Agriculture IoT in Java with Conductor

## Why Smart Irrigation Needs Orchestration

Deciding whether to irrigate a field requires data from multiple sources fed through a decision pipeline. You read soil sensors to get current moisture levels and pH for the crop type. You fetch weather data to check temperature and rain probability. irrigating before a rainstorm wastes water and can damage crops. You feed soil and weather data into an irrigation decision engine that determines whether to irrigate, for how long, and which zones. Finally, you actuate the irrigation valves for the selected zones and duration.

Each step depends on the previous one. the decision engine needs both soil and weather data, and the actuator needs the decision output. If a soil sensor read fails, you need to retry without re-fetching weather data that is still valid. Without orchestration, you'd build a monolithic irrigation controller that mixes sensor polling, weather API calls, decision logic, and valve control, making it impossible to swap weather providers, test irrigation rules independently, or audit which sensor readings triggered which irrigation events.

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

the workflow and alerting logic stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
