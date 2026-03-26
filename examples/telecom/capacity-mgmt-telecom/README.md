# Capacity Mgmt Telecom

Orchestrates capacity mgmt telecom through a multi-stage Conductor workflow.

**Input:** `region`, `networkType` | **Timeout:** 60s

## Pipeline

```
cmt_monitor
    │
cmt_forecast
    │
cmt_plan
    │
cmt_provision
    │
cmt_verify
```

## Workers

**ForecastWorker** (`cmt_forecast`)

Outputs `forecast`.

**MonitorWorker** (`cmt_monitor`)

Reads `region`. Outputs `utilization`, `growthRate`, `peakHours`.

**PlanWorker** (`cmt_plan`)

Reads `region`. Outputs `plan`.

**ProvisionWorker** (`cmt_provision`)

Outputs `provisionId`, `newCapacity`, `towersAdded`.

**VerifyWorker** (`cmt_verify`)

Reads `region`. Outputs `verified`, `newUtilization`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
