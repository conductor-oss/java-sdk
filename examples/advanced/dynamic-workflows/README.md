# Dynamic Workflows

A multi-tenant platform needs to execute different workflow definitions for each tenant, and tenants can modify their workflows at runtime. The system must load the correct workflow definition dynamically, validate it, execute it, and handle the case where a tenant's definition is invalid without affecting other tenants.

## Pipeline

```
[dw_validate]
     |
     v
[dw_transform]
     |
     v
[dw_enrich]
     |
     v
[dw_publish]
```

**Workflow inputs:** `pipelineName`, `payload`

## Workers

**DwEnrichWorker** (task: `dw_enrich`)

Enrich step in a dynamic pipeline.

- Sets `result` = `"enrich_complete"`
- Reads `config`. Writes `result`, `stepType`

**DwPublishWorker** (task: `dw_publish`)

Publish step in a dynamic pipeline.

- Sets `result` = `"publish_complete"`
- Reads `config`. Writes `result`, `stepType`

**DwTransformWorker** (task: `dw_transform`)

Transform step in a dynamic pipeline.

- Sets `result` = `"transform_complete"`
- Reads `config`. Writes `result`, `stepType`

**DwValidateWorker** (task: `dw_validate`)

Validate step in a dynamic pipeline.

- Sets `result` = `"validate_complete"`
- Reads `stepId`, `config`. Writes `result`, `stepType`

---

**28 tests** | Workflow: `dynamic_workflow_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
