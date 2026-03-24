# Model Serving

A real-time recommendation system needs to load a trained model, accept inference requests, run predictions, and return results within a 50ms latency budget. The serving pipeline needs model loading, input validation, batch inference for efficiency, and response formatting.

## Pipeline

```
[msv_load_model]
     |
     v
[msv_validate]
     |
     v
[msv_deploy]
     |
     v
[msv_test]
     |
     v
[msv_promote]
```

**Workflow inputs:** `modelName`, `modelVersion`, `modelPath`

## Workers

**MsvDeployWorker** (task: `msv_deploy`)

- Writes `endpointUrl`, `replicas`

**MsvLoadModelWorker** (task: `msv_load_model`)

- Writes `loaded`, `signature`, `inputShape`, `sizeBytes`

**MsvPromoteWorker** (task: `msv_promote`)

- Reads `testsPassed`. Writes `promoted`

**MsvTestWorker** (task: `msv_test`)

- Writes `allPassed`, `latencyP50`, `latencyP99`

**MsvValidateWorker** (task: `msv_validate`)

- Writes `valid`, `warningCount`

---

**20 tests** | Workflow: `model_serving_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
