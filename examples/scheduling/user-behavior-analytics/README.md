# User Behavior Analytics

A user's activity needs anomaly analysis. The pipeline collects events for the specified time range, groups them into sessions, analyzes behavioral patterns across sessions, and flags anomalies when the risk score exceeds the configured threshold.

## Workflow

```
uba_collect_events ──> uba_sessionize ──> uba_analyze_patterns ──> uba_flag_anomalies
```

Workflow `user_behavior_analytics_429` accepts `userId`, `timeRange`, and `riskThreshold`. Times out after `60` seconds.

## Workers

**CollectEventsWorker** (`uba_collect_events`) -- collects events for the specified user and time range.

**SessionizeWorker** (`uba_sessionize`) -- groups the collected events into sessions.

**AnalyzePatternsWorker** (`uba_analyze_patterns`) -- analyzes behavioral patterns across the sessions.

**FlagAnomaliesWorker** (`uba_flag_anomalies`) -- compares the user's risk score against the threshold and flags anomalies.

## Workflow Output

The workflow produces `eventCount`, `sessionCount`, `riskScore`, `flagged` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `user_behavior_analytics_429` defines 4 tasks with input parameters `userId`, `timeRange`, `riskThreshold` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify event collection, sessionization, pattern analysis, and anomaly flagging.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
