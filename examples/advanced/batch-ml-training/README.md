# Batch ML Model Training in Java Using Conductor : Prepare Data, Train in Parallel, Evaluate

## Running ML Experiments Without Losing Your Mind

Training a single model is straightforward. Comparing multiple model architectures on the same dataset is where things get messy. You need to prepare the raw data (cleaning, feature engineering), split it consistently so every model trains on the same 80/20 partition, train a random forest and a gradient boosting model simultaneously to cut wall-clock time in half, then evaluate both against the held-out test set to determine which one wins.

Doing this manually means managing parallel training threads, ensuring both models use the exact same split, handling the case where one training run OOMs while the other succeeds, and logging hyperparameters and accuracy metrics for reproducibility. Every failed experiment leaves you wondering whether the data prep was different or the split was wrong.

## The Solution

**You write the data prep and training logic. Conductor handles parallel execution, retries, and experiment lineage.**

`BmlPrepareDataWorker` loads and cleans the dataset. `BmlSplitDataWorker` partitions it into train and test sets at the configured ratio (default 80/20). A `FORK_JOIN` then trains both models in parallel. `BmlTrainModel1Worker` fits a random forest while `BmlTrainModel2Worker` fits a gradient boosting model on the same training data. Once both finish, `BmlEvaluateWorker` compares their accuracy scores against the test set and declares the winner. Every step's inputs and outputs are recorded, so you can trace exactly which dataset version, split, and hyperparameters produced each accuracy number.

### What You Write: Workers

Five workers cover the training pipeline: data preparation, splitting, two parallel model trainers (random forest and gradient boosting), and evaluation, each isolated to one ML concern.### The Workflow

```
bml_prepare_data
 │
 ▼
bml_split_data
 │
 ▼
FORK_JOIN
 ├── bml_train_model_1
 └── bml_train_model_2
 │
 ▼
JOIN (wait for all branches)
bml_evaluate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
