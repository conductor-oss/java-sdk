# Pipeline Pattern

A data transformation pipeline chains multiple processing stages where each stage's output feeds the next stage's input. The pipeline pattern needs to validate input, apply transformations sequentially, handle stage failures without losing upstream work, and produce the final output.

## Pipeline

```
[pip_stage_1]
     |
     v
[pip_stage_2]
     |
     v
[pip_stage_3]
     |
     v
[pip_stage_4]
```

**Workflow inputs:** `rawData`, `pipelineId`

## Workers

**PipStage1Worker** (task: `pip_stage_1`)

- Sets `stage` = `"validate"`
- Writes `data`, `stage`

**PipStage2Worker** (task: `pip_stage_2`)

- Sets `stage` = `"transform"`
- Writes `data`, `stage`

**PipStage3Worker** (task: `pip_stage_3`)

- Sets `stage` = `"enrich"`
- Writes `data`, `stage`

**PipStage4Worker** (task: `pip_stage_4`)

- Captures `instant.now()` timestamps
- Sets `stage` = `"output"`
- Writes `data`, `stage`

---

**16 tests** | Workflow: `pip_pipeline_pattern` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
