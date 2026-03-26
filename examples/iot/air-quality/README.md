# Air Quality

Orchestrates air quality through a multi-stage Conductor workflow.

**Input:** `stationId`, `region` | **Timeout:** 60s

## Pipeline

```
aq_collect_readings
    │
aq_check_standards
    │
aq_route_action [SWITCH]
  ├─ good: aq_action_good
  ├─ moderate: aq_action_moderate
  └─ poor: aq_action_poor
```

## Workers

**ActionGoodWorker** (`aq_action_good`)

Outputs `action`, `notificationSent`.

**ActionModerateWorker** (`aq_action_moderate`)

Outputs `action`, `notificationSent`.

**ActionPoorWorker** (`aq_action_poor`)

Outputs `action`, `notificationSent`.

**CheckStandardsWorker** (`aq_check_standards`)

Outputs `done`.

**CollectReadingsWorker** (`aq_collect_readings`)

Outputs `done`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
