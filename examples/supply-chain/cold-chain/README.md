# Cold Chain

Cold chain monitoring: monitor temp, check thresholds, switch on status, and act.

**Input:** `shipmentId`, `product`, `minTemp`, `maxTemp` | **Timeout:** 60s

## Pipeline

```
cch_monitor_temp
    │
cch_check_thresholds
    │
threshold_switch [SWITCH]
  ├─ ok: cch_handle_ok
  └─ default: cch_handle_alert
    │
cch_act
```

## Workers

**ActWorker** (`cch_act`)

Reads `shipmentId`, `status`. Outputs `action`, `logged`.

**CheckThresholdsWorker** (`cch_check_thresholds`)

```java
String status = (temp >= min && temp <= max) ? "ok" : "alert";
```

Reads `currentTemp`, `maxTemp`, `minTemp`. Outputs `status`.

**HandleAlertWorker** (`cch_handle_alert`)

Reads `currentTemp`, `shipmentId`. Outputs `action`, `notified`.

**HandleOkWorker** (`cch_handle_ok`)

Reads `shipmentId`. Outputs `action`.

**MonitorTempWorker** (`cch_monitor_temp`)

Reads `product`, `shipmentId`. Outputs `currentTemp`, `humidity`, `timestamp`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
