# Agriculture Iot

Orchestrates agriculture iot through a multi-stage Conductor workflow.

**Input:** `fieldId`, `cropType` | **Timeout:** 60s

## Pipeline

```
agr_soil_sensors
    │
agr_weather_data
    │
agr_irrigation_decision
    │
agr_actuate
```

## Workers

**ActuateWorker** (`agr_actuate`)

Outputs `done`.

**IrrigationDecisionWorker** (`agr_irrigation_decision`)

Outputs `durationMinutes`, `zones`.

**SoilSensorsWorker** (`agr_soil_sensors`)

Outputs `done`.

**WeatherDataWorker** (`agr_weather_data`)

Outputs `done`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
