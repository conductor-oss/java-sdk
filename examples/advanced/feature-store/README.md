# Feature Store

A machine learning platform serves features to both training pipelines and online inference endpoints. Features must be computed once, stored with versioning, served with low latency, and kept consistent between offline training and online serving. A stale feature in production means the model sees data it was never trained on.

## Pipeline

```
[fst_compute_features]
     |
     v
[fst_validate]
     |
     v
[fst_register]
     |
     v
[fst_serve]
```

**Workflow inputs:** `featureGroupName`, `sourceTable`, `entityKey`

## Workers

**FstComputeFeaturesWorker** (task: `fst_compute_features`)

- Writes `features`, `featureCount`, `stats`

**FstRegisterWorker** (task: `fst_register`)

- Records wall-clock milliseconds
- Writes `registryId`, `registered`

**FstServeWorker** (task: `fst_serve`)

- Writes `serving`, `endpoint`

**FstValidateWorker** (task: `fst_validate`)

- Reads `features`. Writes `valid`, `validFeatures`, `version`

---

**16 tests** | Workflow: `feature_store_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
