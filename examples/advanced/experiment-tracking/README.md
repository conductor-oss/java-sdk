# Experiment Tracking

A data science team runs dozens of experiments per week but loses track of which hyperparameters produced which results. The tracking pipeline needs to log experiment configuration, record metrics at each epoch, compare against baseline results, and register the best-performing run.

## Pipeline

```
[ext_define_experiment]
     |
     v
[ext_run_experiment]
     |
     v
[ext_log_metrics]
     |
     v
[ext_compare]
     |
     v
[ext_decide]
```

**Workflow inputs:** `experimentName`, `hypothesis`, `baselineMetric`

## Workers

**ExtCompareWorker** (task: `ext_compare`)

- Uses `math.abs()`, formats output strings
- Reads `currentMetric`, `baselineMetric`. Writes `improvement`, `statSignificant`

**ExtDecideWorker** (task: `ext_decide`)

- Reads `significant`. Writes `decision`

**ExtDefineExperimentWorker** (task: `ext_define_experiment`)

- Records wall-clock milliseconds
- Writes `experimentId`, `config`

**ExtLogMetricsWorker** (task: `ext_log_metrics`)

- Writes `logged`, `runUrl`

**ExtRunExperimentWorker** (task: `ext_run_experiment`)

- Writes `metrics`, `primaryMetric`

---

**20 tests** | Workflow: `experiment_tracking_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
