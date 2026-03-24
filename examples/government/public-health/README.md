# Public Health

Orchestrates public health through a multi-stage Conductor workflow.

**Input:** `region`, `disease`, `caseCount` | **Timeout:** 60s

## Pipeline

```
phw_surveillance
    │
phw_detect_outbreak
    │
route_action [SWITCH]
  ├─ alert: phw_alert
  └─ monitor: phw_monitor
    │
phw_respond
```

## Workers

**AlertWorker** (`phw_alert`)

Reads `disease`, `region`, `severity`. Outputs `alertIssued`, `alertLevel`.

**DetectOutbreakWorker** (`phw_detect_outbreak`)

```java
boolean isOutbreak = cases > baseline * 2;
```

Reads `baseline`, `caseCount`. Outputs `action`, `severity`, `nextCheck`.

**MonitorWorker** (`phw_monitor`)

Reads `nextCheckDate`, `region`. Outputs `monitoring`.

**RespondWorker** (`phw_respond`)

Reads `action`, `region`. Outputs `responseActivated`.

**SurveillanceWorker** (`phw_surveillance`)

Reads `disease`, `region`. Outputs `baseline`, `reportPeriod`.

## Tests

**3 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
