# Performance Profiling

A service needs performance analysis. The pipeline instruments the service for the specified profile type, collects samples for the configured duration, analyzes hotspots in the sample data, and generates optimization recommendations.

## Workflow

```
prf_instrument ──> prf_collect_profile ──> prf_analyze_hotspots ──> prf_recommend
```

Workflow `performance_profiling_430` accepts `serviceName`, `profilingDuration`, and `profileType`. Times out after `60` seconds.

## Workers

**PrfInstrumentWorker** (`prf_instrument`) -- instruments the service for the specified profile type.

**CollectProfileWorker** (`prf_collect_profile`) -- collects profile data for the configured duration.

**AnalyzeHotspotsWorker** (`prf_analyze_hotspots`) -- analyzes the collected samples for hotspots.

**RecommendWorker** (`prf_recommend`) -- generates optimization recommendations based on the hotspot count.

## Workflow Output

The workflow produces `sampleCount`, `hotspotCount`, `topHotspot`, `recommendationCount`, `reportUrl` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `performance_profiling_430` defines 4 tasks with input parameters `serviceName`, `profilingDuration`, `profileType` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify instrumentation, profile collection, hotspot analysis, and recommendation generation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
