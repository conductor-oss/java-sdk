# AI Model Evaluation: Load, Test, Infer, Measure, Report

A machine learning team deploys a new model version and needs to know whether it meets production quality bars before routing live traffic to it. Running inference manually against a test set, computing metrics, and writing a report is tedious and error-prone. The evaluation pipeline needs to be reproducible and automated.

This workflow loads a model, prepares a test dataset, runs inference, computes accuracy and latency metrics, and generates a structured evaluation report.

## Pipeline Architecture

```
modelId, testDatasetId, taskType
         |
         v
  ame_load_model         (endpoint="http://model-server/v1/predict", params="500M")
         |
         v
  ame_prepare_test_set   (sampleCount=2000, classes=10)
         |
         v
  ame_run_inference      (predictions=2000, latencyP50=12ms, latencyP99=45ms)
         |
         v
  ame_compute_metrics    (metrics="accuracy=0.937")
         |
         v
  ame_report             (reportId="EVAL-ai-model-evaluation-001", generated=true)
```

## Worker: LoadModel (`ame_load_model`)

Loads the model identified by `modelId` and returns its serving `endpoint: "http://model-server/v1/predict"` and parameter count `params: "500M"`. This establishes the inference target for downstream stages.

## Worker: PrepareTestSet (`ame_prepare_test_set`)

Prepares a test dataset from the `testDatasetId` for the given `taskType`. Returns `sampleCount: 2000` test samples distributed across `classes: 10` categories. The sample count feeds into the inference runner to determine batch size.

## Worker: RunInference (`ame_run_inference`)

Runs the 2000 test samples against the model endpoint. Returns `predictions: 2000` (matching the sample count), with latency measurements: `latencyP50: 12` milliseconds at the median and `latencyP99: 45` milliseconds at the 99th percentile. These latency numbers help assess whether the model meets real-time serving requirements.

## Worker: ComputeMetrics (`ame_compute_metrics`)

Computes evaluation metrics from the predictions and ground truth. Returns `metrics: "accuracy=0.937"`. When an LLM is available, the worker can delegate metric computation to the model for more detailed analysis including F1, precision, recall, and AUC. Falls back to the hardcoded accuracy string otherwise.

## Worker: Report (`ame_report`)

Generates the final evaluation report. Returns `reportId: "EVAL-ai-model-evaluation-001"` and `generated: true`. When an LLM is available, also returns `reportContent` with a detailed narrative analysis. The report ID is deterministic, derived from the example project name.

## Tests

2 tests cover the model evaluation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
