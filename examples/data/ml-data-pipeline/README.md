# Ml Data Pipeline

A machine learning team needs a reproducible pipeline that takes raw training data, splits it into train/validation/test sets, normalizes features, validates the splits for data leakage, and packages the result as versioned artifacts. Running these steps manually in a notebook means the model was trained on a different split than the one that was validated.

## Pipeline

```
[ml_collect_data]
     |
     v
[ml_clean_data]
     |
     v
[ml_split_data]
     |
     v
[ml_train_model]
     |
     v
[ml_evaluate_model]
```

**Workflow inputs:** `dataSource`, `modelType`, `splitRatio`

## Workers

**CleanDataWorker** (task: `ml_clean_data`)

- Reads `rawData`. Writes `cleanData`, `cleanCount`, `removedCount`

**CollectDataWorker** (task: `ml_collect_data`)

- Writes `data`, `recordCount`

**EvaluateModelWorker** (task: `ml_evaluate_model`)

- Reads `testData`. Writes `accuracy`, `metrics`

**SplitDataWorker** (task: `ml_split_data`)

- Applies `math.floor()`
- Reads `cleanData`, `splitRatio`. Writes `trainData`, `testData`, `trainSize`, `testSize`

**TrainModelWorker** (task: `ml_train_model`)

- Reads `trainData`. Writes `model`, `trainingLoss`

---

**40 tests** | Workflow: `ml_data_pipeline` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
