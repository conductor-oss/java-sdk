# Pipeline Versioning

A data team maintains multiple versions of their processing pipeline. When a new version is deployed, in-flight workflows should complete on the old version while new workflows use the new version. The versioning system needs version detection, routing, and side-by-side execution.

## Pipeline

```
[pvr_snapshot_config]
     |
     v
[pvr_tag_version]
     |
     v
[pvr_test_pipeline]
     |
     v
[pvr_promote]
```

**Workflow inputs:** `pipelineName`, `versionTag`, `environment`

## Workers

**PvrPromoteWorker** (task: `pvr_promote`)

- Reads `testsPassed`. Writes `promoted`, `environment`

**PvrSnapshotConfigWorker** (task: `pvr_snapshot_config`)

- Records wall-clock milliseconds
- Writes `configSnapshot`, `configHash`, `stepCount`

**PvrTagVersionWorker** (task: `pvr_tag_version`)

- Writes `tagged`, `versionTag`

**PvrTestPipelineWorker** (task: `pvr_test_pipeline`)

- Writes `allPassed`, `testCount`

---

**16 tests** | Workflow: `pipeline_versioning_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
