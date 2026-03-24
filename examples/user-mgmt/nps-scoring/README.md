# Nps Scoring

Orchestrates nps scoring through a multi-stage Conductor workflow.

**Input:** `campaignId`, `period` | **Timeout:** 60s

## Pipeline

```
nps_collect_responses
    │
nps_calculate
    │
nps_segment
    │
nps_act
```

## Workers

**ActWorker** (`nps_act`)

Reads `npsScore`, `segments`. Outputs `actionsTriggered`, `totalActions`.

**CalculateNpsWorker** (`nps_calculate`)

```java
double npsScore = ((double) (promoters - detractors) / (promoters + passives + detractors)) * 100;
```

Reads `responses`. Outputs `promoters`, `passives`, `detractors`, `npsScore`.

**CollectResponsesWorker** (`nps_collect_responses`)

```java
.mapToObj(i -> Map.<String, Object>of(
```

Reads `campaignId`, `period`. Outputs `responses`, `totalResponses`.

**SegmentWorker** (`nps_segment`)

Reads `detractors`, `passives`, `promoters`. Outputs `segments`, `totalSegments`.

## Tests

**17 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
