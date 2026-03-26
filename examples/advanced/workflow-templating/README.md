# Workflow Templating

A platform creates dozens of similar workflows that differ only in parameters: different API endpoints, different thresholds, different notification channels. The templating system defines a base template, accepts parameters, generates a concrete workflow definition, and validates it before registration.

## Pipeline

```
[wtm_extract]
     |
     v
[wtm_transform]
     |
     v
[wtm_load]
     |
     v
[wtm_verify]
```

**Workflow inputs:** `batchId`, `options`

## Workers

**WtmExtractWorker** (task: `wtm_extract`)

- Writes `records`, `recordCount`

**WtmLoadWorker** (task: `wtm_load`)

- Writes `loadedCount`, `destination`

**WtmTransformWorker** (task: `wtm_transform`)

- Writes `transformed`

**WtmVerifyWorker** (task: `wtm_verify`)

- Writes `verified`, `match`

---

**16 tests** | Workflow: `wtm_etl_postgres_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
