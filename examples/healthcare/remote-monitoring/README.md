# Remote Monitoring

Remote Patient Monitoring: collect vitals, analyze trends, SWITCH route, take action

**Input:** `patientId`, `deviceId` | **Timeout:** 60s

## Pipeline

```
rpm_collect_vitals
    │
rpm_analyze_trends
    │
rpm_route_condition [SWITCH]
  ├─ normal: rpm_normal_action
  ├─ alert: rpm_alert_action
  └─ default: rpm_alert_action
```

## Workers

**AlertActionWorker** (`rpm_alert_action`)

Reads `alerts`, `patientId`. Outputs `action`, `urgency`, `notifiedProvider`, `notifiedAt`.

**AnalyzeTrendsWorker** (`rpm_analyze_trends`)

```java
String status = alerts.isEmpty() ? "normal" : "alert";
```

Reads `vitals`. Outputs `status`, `alerts`, `trendDirection`.

**CollectVitalsWorker** (`rpm_collect_vitals`)

Reads `deviceId`. Outputs `vitals`, `timestamp`.

**NormalActionWorker** (`rpm_normal_action`)

Reads `patientId`. Outputs `action`, `nextReading`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
