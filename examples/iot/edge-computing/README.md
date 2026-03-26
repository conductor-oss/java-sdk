# Edge Computing

Orchestrates edge computing through a multi-stage Conductor workflow.

**Input:** `jobId`, `edgeNodeId`, `taskType`, `payload` | **Timeout:** 60s

## Pipeline

```
edg_offload_task
    │
edg_process_edge
    │
edg_sync_results
    │
edg_aggregate_cloud
```

## Workers

**AggregateCloudWorker** (`edg_aggregate_cloud`)

Reads `aggregated`. Outputs `aggregated`, `storedInTimeseries`, `dashboardUpdated`, `totalEdgeJobs24h`.

**OffloadTaskWorker** (`edg_offload_task`)

Reads `edgeJobId`. Outputs `edgeJobId`, `scheduledAt`, `estimatedCompletionMs`.

**ProcessEdgeWorker** (`edg_process_edge`)

Reads `edgeResult`. Outputs `edgeResult`, `objectsDetected`, `classifications`, `confidenceAvg`.

**SyncResultsWorker** (`edg_sync_results`)

Reads `edgeResult`, `syncedData`. Outputs `syncedData`, `syncedAt`, `latencyMs`, `dataSizeKb`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
