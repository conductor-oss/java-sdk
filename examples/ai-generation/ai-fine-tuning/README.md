# Fine-Tuning Pipeline: Prepare, Configure, Train, Evaluate, Deploy

End-to-end fine-tuning: prepare the dataset, configure hyperparameters, train the model, evaluate against validation (accuracy, F1 via LLM), and deploy to endpoint `https://api.example.com/v1/ft-804`.

## Workflow

```
baseModel, datasetId, taskType
  -> aft_prepare_dataset -> aft_configure -> aft_train -> aft_evaluate -> aft_deploy
```

## Workers

**PrepareDatasetWorker** (`aft_prepare_dataset`) -- Prepares training data.

**ConfigureWorker** (`aft_configure`) -- Sets training hyperparameters.

**TrainWorker** (`aft_train`) -- Executes training run.

**EvaluateWorker** (`aft_evaluate`) -- Evaluates checkpoint against validation set. Uses LLM when API key is set.

**DeployWorker** (`aft_deploy`) -- Deploys to `endpoint: "https://api.example.com/v1/ft-804"`.

## Tests

2 tests cover the pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
