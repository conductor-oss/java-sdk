# Smart Home

Orchestrates smart home through a multi-stage Conductor workflow.

**Input:** `eventId`, `sensorId`, `eventType`, `value` | **Timeout:** 60s

## Pipeline

```
smh_detect_event
    │
smh_evaluate_rules
    │
smh_action_switch [SWITCH]
  ├─ lights: smh_actuate_lights
  ├─ thermostat: smh_actuate_thermostat
  └─ security: smh_actuate_security
    │
smh_log_event
```

## Workers

**ActuateLightsWorker** (`smh_actuate_lights`)

Reads `actuated`. Outputs `actuated`, `device`, `brightness`, `color`.

**ActuateSecurityWorker** (`smh_actuate_security`)

Reads `actuated`, `zone`. Outputs `actuated`, `device`, `zone`.

**ActuateThermostatWorker** (`smh_actuate_thermostat`)

Reads `actuated`, `targetTemp`. Outputs `actuated`, `device`, `setTemp`.

**DetectEventWorker** (`smh_detect_event`)

Reads `context`. Outputs `context`, `occupancy`, `mode`, `currentTemp`.

**EvaluateRulesWorker** (`smh_evaluate_rules`)

Reads `ruleId`. Outputs `ruleId`.

**LogEventWorker** (`smh_log_event`)

Reads `logged`. Outputs `logged`, `logId`, `loggedAt`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
