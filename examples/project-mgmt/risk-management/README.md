# Risk Management

Risk management with SWITCH for high/medium/low severity.

**Input:** `projectId`, `riskDescription` | **Timeout:** 60s

## Pipeline

```
rkm_identify
    │
rkm_assess
    │
route_risk [SWITCH]
  ├─ high: rkm_high
  ├─ medium: rkm_medium
  └─ default: rkm_low
    │
rkm_mitigate
```

## Workers

**AssessWorker** (`rkm_assess`)

Outputs `severity`, `probability`, `impact`.

**HighWorker** (`rkm_high`)

Outputs `action`.

**IdentifyWorker** (`rkm_identify`)

Reads `description`, `projectId`. Outputs `risk`.

**LowWorker** (`rkm_low`)

Outputs `action`.

**MediumWorker** (`rkm_medium`)

Outputs `action`.

**MitigateWorker** (`rkm_mitigate`)

Reads `severity`. Outputs `plan`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
