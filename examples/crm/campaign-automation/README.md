# Campaign Automation

Orchestrates campaign automation through a multi-stage Conductor workflow.

**Input:** `campaignName`, `type`, `budget` | **Timeout:** 60s

## Pipeline

```
cpa_design
    │
cpa_target
    │
cpa_execute
    │
cpa_measure
```

## Workers

**DesignWorker** (`cpa_design`)

```java
String campaignId = "CMP-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
```

Reads `name`, `type`. Outputs `campaignId`, `type`, `creativeAssets`, `channels`.

**ExecuteWorker** (`cpa_execute`)

```java
String execId = "EXC-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
```

Outputs `executionId`, `impressions`, `clicks`, `sentAt`.

**MeasureWorker** (`cpa_measure`)

Outputs `roi`, `metrics`.

**TargetWorker** (`cpa_target`)

Reads `budget`, `campaignId`. Outputs `audience`, `audienceSize`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
