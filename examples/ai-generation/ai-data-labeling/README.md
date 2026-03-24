# Parallel Data Labeling with Reconciliation

Two independent labelers classify a batch of 500 samples in parallel via FORK_JOIN, each with their own system prompt and confidence scoring. A reconciliation step resolves disagreements between the labelers, and the final labeled dataset is exported.

## Workflow

```
datasetId, labelType
  -> adl_prepare_data (batch of 500 samples)
  -> FORK_JOIN(adl_labeler_1 | adl_labeler_2)
  -> adl_reconcile (resolve disagreements via LLM)
  -> adl_export
```

## Workers

**PrepareDataWorker** (`adl_prepare_data`) -- Returns `batch: {sampleCount: 500, type: labelType}`.

**Labeler1Worker** (`adl_labeler_1`) -- Uses system prompt: `"You are an expert data labeler..."`. Calls LLM when API key is set.

**Labeler2Worker** (`adl_labeler_2`) -- Independent second opinion with its own system prompt.

**ReconcileWorker** (`adl_reconcile`) -- Reconciles disagreements between labelers. Calls LLM when API key is set.

**ExportWorker** (`adl_export`) -- Exports the reconciled labeled dataset.

## Tests

2 tests cover the labeling pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
