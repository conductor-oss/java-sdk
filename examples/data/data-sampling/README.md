# Data Sampling

A machine learning team has a dataset of 10 million records but only needs a representative sample of 10,000 for model prototyping. Simple random sampling would under-represent rare classes. They need stratified sampling that preserves class distributions, validates statistical properties, and outputs a balanced subset.

## Pipeline

```
[sm_load_dataset]
     |
     v
[sm_draw_sample]
     |
     v
[sm_run_quality_checks]
     |
     v
     <SWITCH>
       |-- pass -> [sm_approve_dataset]
       +-- default -> [sm_flag_for_review]
```

**Workflow inputs:** `records`, `sampleRate`, `threshold`

## Workers

**ApproveDatasetWorker** (task: `sm_approve_dataset`)

Approves a dataset that passed quality checks.

- Sets `status` = `"APPROVED"`
- Reads `totalRecords`, `qualityScore`. Writes `approved`, `status`

**DrawSampleWorker** (task: `sm_draw_sample`)

Draws a deterministic sample from the dataset.

- Parses strings to `double`, applies `math.floor()`
- Reads `records`, `sampleRate`. Writes `sample`, `sampleSize`

**FlagForReviewWorker** (task: `sm_flag_for_review`)

Flags a dataset for review when quality checks fail.

- Sets `status` = `"FLAGGED_FOR_REVIEW"`
- Reads `totalRecords`, `qualityScore`, `issues`. Writes `approved`, `status`

**LoadDatasetWorker** (task: `sm_load_dataset`)

Loads a dataset from the input records.

- Reads `records`. Writes `records`, `count`

**RunQualityChecksWorker** (task: `sm_run_quality_checks`)

Runs quality checks on the sampled data.

- Parses strings to `double`
- Reads `sample`, `threshold`. Writes `qualityScore`, `decision`, `issues`, `validCount`, `checkedCount`

---

**44 tests** | Workflow: `data_sampling_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
