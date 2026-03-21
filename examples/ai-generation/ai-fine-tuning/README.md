# AI Fine-Tuning Pipeline in Java Using Conductor : Prepare Dataset, Configure, Train, Evaluate, Deploy

## Fine-Tuning Is a Pipeline, Not a Single Step

Fine-tuning an LLM on your domain data involves five distinct stages, each with different requirements and failure modes. Dataset preparation (formatting to chat/instruction format, train/val split, deduplication) is CPU-bound. Configuration (learning rate, batch size, LoRA rank, epochs) requires domain knowledge. Training is GPU-bound and can take hours. Evaluation compares the fine-tuned model against the base model on held-out data. Deployment makes the model available for inference.

If training crashes after 3 hours (GPU out of memory, training divergence), you need to adjust configuration and restart training. not re-prepare the dataset. If evaluation shows the fine-tuned model is worse than the base model (catastrophic forgetting), you need to try different hyperparameters. Each stage needs independent retry and tracking.

## The Solution

**You just write the dataset preparation, hyperparameter configuration, training execution, model evaluation, and deployment logic. Conductor handles training checkpointing, evaluation sequencing, and deployment rollback tracking.**

`PrepareDatasetWorker` formats the raw data into the training format (instruction/response pairs, chat turns), performs train/validation split, and validates quality. `ConfigureWorker` sets hyperparameters (learning rate, batch size, epochs, LoRA rank) based on the task type and base model. `TrainWorker` executes the fine-tuning job with the prepared data and configuration. `EvaluateWorker` compares the fine-tuned model against the base model on held-out validation data, computing task-specific metrics. `DeployWorker` deploys the fine-tuned model to a serving endpoint if evaluation passes quality gates. Conductor tracks each training run with its hyperparameters and evaluation results for experiment management.

### What You Write: Workers

The fine-tuning pipeline uses separate workers for dataset prep, hyperparameter config, training, evaluation, and deployment. Swap any step without affecting the rest.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureWorker** | `aft_configure` | Configures and returns config, learning rate, epochs |
| **DeployWorker** | `aft_deploy` | Deploys the fine-tuned model to a production endpoint with 3 replicas for serving |
| **EvaluateWorker** | `aft_evaluate` | Evaluates the training checkpoint on the validation set. accuracy: 0.952, determines if quality threshold is met |
| **PrepareDatasetWorker** | `aft_prepare_dataset` | Prepares the training dataset. formats 10K samples with 80/20 train/validation split |
| **TrainWorker** | `aft_train` | Trains the input and returns model id, checkpoint id, final loss, training time |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
aft_prepare_dataset
 │
 ▼
aft_configure
 │
 ▼
aft_train
 │
 ▼
aft_evaluate
 │
 ▼
aft_deploy

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
