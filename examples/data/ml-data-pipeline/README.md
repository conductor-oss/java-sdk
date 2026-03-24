# ML Data Pipeline in Java Using Conductor : Data Collection, Cleaning, Train/Test Split, Model Training, and Evaluation

## The Problem

Training a model requires a strict sequence of data preparation steps, and each depends on the output of the previous one. You collect labeled records from a data source, but some have null features or malformed labels that would corrupt training. You clean those out, but then you need to split the surviving records into train and test sets at a specific ratio (say, 80/20), and the split must happen after cleaning, not before, or your test set contains dirty data. You train the model (a random forest classifier, for example) on the training partition, but evaluation must use the held-out test partition from the same split. Not a different random sample. If training crashes after 30 minutes of GPU time, you don't want to re-collect and re-clean the data from scratch.

Without orchestration, you'd write a Jupyter notebook or a monolithic training script that collects, cleans, splits, trains, and evaluates in one run. If the data source is temporarily unavailable, the entire pipeline fails with no retry. There's no record of how many records were collected vs: cleaned vs: used for training, making it impossible to debug why accuracy dropped. When you want to experiment with a different split ratio or model type, you'd modify deeply coupled code with no visibility into whether the data pipeline or the model itself is the bottleneck.

## The Solution

**You just write the data collection, cleaning, train/test splitting, model training, and evaluation workers. Conductor handles strict sequencing so training uses only cleaned data, retries when data sources are unavailable, and record count tracking at every stage for experiment reproducibility.**

Each stage of the ML pipeline is a simple, independent worker. The data collector fetches labeled records from the configured data source. The cleaner removes records with null features or invalid labels and reports how many survived. The splitter divides clean data into train and test partitions at the configured ratio (e.g., 0.8 for 80/20). The trainer fits the specified model type (random forest, logistic regression, gradient boosting) on the training partition, producing a trained model artifact and a training loss metric. The evaluator runs the trained model against the held-out test set and computes accuracy, precision, recall, and F1 scores. Conductor executes them in strict sequence, passes the evolving dataset between stages, retries if the data source is temporarily unavailable, and tracks record counts at every stage. So you can see exactly how many raw records became clean records became training samples became evaluation metrics.

### What You Write: Workers

Five workers implement the end-to-end ML training pipeline: collecting labeled data, cleaning records with null features or invalid labels, splitting into train/test sets at a configurable ratio, training the model, and evaluating accuracy metrics against the held-out test set.

| Worker | Task | What It Does |
|---|---|---|
| **CleanDataWorker** | `ml_clean_data` | Clean Data. Computes and returns clean data, clean count, removed count |
| **CollectDataWorker** | `ml_collect_data` | Collect Data. Computes and returns data, record count |
| **EvaluateModelWorker** | `ml_evaluate_model` | Model accuracy: 94.5%, F1: 93.9% on |
| **SplitDataWorker** | `ml_split_data` | Split Data. Computes and returns train data, test data, train size, test size |
| **TrainModelWorker** | `ml_train_model` | Train Model. Computes and returns model, training loss |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
ml_collect_data
 │
 ▼
ml_clean_data
 │
 ▼
ml_split_data
 │
 ▼
ml_train_model
 │
 ▼
ml_evaluate_model

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
