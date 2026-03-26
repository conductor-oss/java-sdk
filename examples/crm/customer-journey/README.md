# Customer Journey

Orchestrates customer journey through a multi-stage Conductor workflow.

**Input:** `customerId`, `timeWindow` | **Timeout:** 60s

## Pipeline

```
cjy_track_touchpoints
    │
cjy_map_journey
    │
cjy_analyze
    │
cjy_optimize
```

## Workers

**AnalyzeWorker** (`cjy_analyze`): Analyzes the customer journey for insights.

Outputs `insights`.

**MapJourneyWorker** (`cjy_map_journey`): Maps customer touchpoints into journey stages.

```java
Map<String, Integer> journeyMap = Map.of(
```

Outputs `journeyMap`, `stageCount`.

**OptimizeWorker** (`cjy_optimize`): Generates optimization recommendations based on journey insights.

Outputs `recommendations`.

**TrackTouchpointsWorker** (`cjy_track_touchpoints`): Tracks customer touchpoints across channels.

Reads `customerId`. Outputs `touchpoints`, `touchpointCount`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
