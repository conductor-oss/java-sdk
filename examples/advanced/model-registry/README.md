# Model Registry

A machine learning platform trains new models daily but needs a central registry to track which model version is in production. The registry pipeline needs to accept a trained model, validate its metrics against minimum thresholds, register it with version metadata, and optionally promote it to production.

## Pipeline

```
[mrg_register]
     |
     v
[mrg_version]
     |
     v
[mrg_validate]
     |
     v
[mrg_approve]
     |
     v
[mrg_deploy]
```

**Workflow inputs:** `modelName`, `modelArtifact`, `metrics`

## Workers

**MrgApproveWorker** (task: `mrg_approve`)

- Writes `approved`, `approver`

**MrgDeployWorker** (task: `mrg_deploy`)

- Reads `approved`. Writes `deployed`

**MrgRegisterWorker** (task: `mrg_register`)

- Records wall-clock milliseconds
- Writes `registryId`, `artifact`

**MrgValidateWorker** (task: `mrg_validate`)

- Writes `validationResult`, `checks`

**MrgVersionWorker** (task: `mrg_version`)

- Writes `version`, `previousVersion`

---

**20 tests** | Workflow: `model_registry_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
