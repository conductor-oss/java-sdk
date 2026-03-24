# Model Evaluation: Load, Test Set, Inference, Metrics, Report

Evaluate a deployed model: load it (endpoint: `http://model-server/v1/predict`), prepare a test set, run inference, compute metrics (accuracy, F1, precision, recall, AUC via LLM), and generate a report.

## Workflow

```
modelId, testDatasetId, taskType
  -> ame_load_model -> ame_prepare_test_set -> ame_run_inference -> ame_compute_metrics -> ame_report
```

## Tests

2 tests cover the evaluation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
