# Batch Ml Training

A machine learning platform needs to run training jobs in batches: prepare the dataset, configure hyperparameters, execute the training run, evaluate the model, and register the result. Each step depends on the previous one, and a failed training run should not require re-preparing the entire dataset.

## Pipeline

```
[bml_prepare_data]
     |
     v
[bml_split_data]
     |
     v
     +───────────────────────────────────────────+
     | [bml_train_model_1] | [bml_train_model_2] |
     +───────────────────────────────────────────+
     [join]
     |
     v
[bml_evaluate]
```

**Workflow inputs:** `datasetId`, `experimentName`

## Workers

**BmlEvaluateWorker** (task: `bml_evaluate`)

- Clamps with `math.max()`
- Reads `model1Accuracy`, `model2Accuracy`. Writes `bestModel`, `bestAccuracy`

**BmlPrepareDataWorker** (task: `bml_prepare_data`)

- Writes `dataPath`, `samples`, `features`

**BmlSplitDataWorker** (task: `bml_split_data`)

- Writes `trainPath`, `testPath`, `trainSamples`, `testSamples`

**BmlTrainModel1Worker** (task: `bml_train_model_1`)

- Writes `modelPath`, `accuracy`, `f1Score`

**BmlTrainModel2Worker** (task: `bml_train_model_2`)

- Writes `modelPath`, `accuracy`, `f1Score`

---

**20 tests** | Workflow: `batch_ml_training_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
