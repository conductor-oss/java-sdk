# Batch ML Model Training in Java Using Conductor :  Prepare Data, Train in Parallel, Evaluate

A Java Conductor workflow example for batch ML training .  loading a dataset, splitting it into train/test sets, training multiple model architectures in parallel (random forest and gradient boosting), and evaluating them to pick the best performer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Running ML Experiments Without Losing Your Mind

Training a single model is straightforward. Comparing multiple model architectures on the same dataset is where things get messy. You need to prepare the raw data (cleaning, feature engineering), split it consistently so every model trains on the same 80/20 partition, train a random forest and a gradient boosting model simultaneously to cut wall-clock time in half, then evaluate both against the held-out test set to determine which one wins.

Doing this manually means managing parallel training threads, ensuring both models use the exact same split, handling the case where one training run OOMs while the other succeeds, and logging hyperparameters and accuracy metrics for reproducibility. Every failed experiment leaves you wondering whether the data prep was different or the split was wrong.

## The Solution

**You write the data prep and training logic. Conductor handles parallel execution, retries, and experiment lineage.**

`BmlPrepareDataWorker` loads and cleans the dataset. `BmlSplitDataWorker` partitions it into train and test sets at the configured ratio (default 80/20). A `FORK_JOIN` then trains both models in parallel .  `BmlTrainModel1Worker` fits a random forest while `BmlTrainModel2Worker` fits a gradient boosting model on the same training data. Once both finish, `BmlEvaluateWorker` compares their accuracy scores against the test set and declares the winner. Every step's inputs and outputs are recorded, so you can trace exactly which dataset version, split, and hyperparameters produced each accuracy number.

### What You Write: Workers

Five workers cover the training pipeline: data preparation, splitting, two parallel model trainers (random forest and gradient boosting), and evaluation, each isolated to one ML concern.

| Worker | Task | What It Does |
|---|---|---|
| **BmlEvaluateWorker** | `bml_evaluate` | Compares accuracy metrics from both trained models and selects the best-performing one |
| **BmlPrepareDataWorker** | `bml_prepare_data` | Loads the raw dataset by ID and prepares it for training (samples, features) |
| **BmlSplitDataWorker** | `bml_split_data` | Splits prepared data into train (80%) and test (20%) partitions |
| **BmlTrainModel1Worker** | `bml_train_model_1` | Trains a Random Forest model on the training split and reports accuracy/F1 metrics |
| **BmlTrainModel2Worker** | `bml_train_model_2` | Trains a Gradient Boosting model on the training split and reports accuracy/F1 metrics |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

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

## Example Output

```
=== Batch ML Training Demo ===

Step 1: Registering task definitions...
  Registered: bml_prepare_data, bml_split_data, bml_train_model_1, bml_train_model_2, bml_evaluate

Step 2: Registering workflow 'batch_ml_training_demo'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [evaluate] Processing
  [prepare] Processing
  [split] Processing
  [train-M1] Processing
  [train-M2] Processing

  Status: COMPLETED
  Output: {bestModel=..., bestAccuracy=..., dataPath=..., samples=...}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/batch-ml-training-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/batch-ml-training-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow batch_ml_training_demo \
  --version 1 \
  --input '{"datasetId": "DS-CHURN-2024", "DS-CHURN-2024": "experimentName", "experimentName": "churn-prediction-v3", "churn-prediction-v3": "sample-churn-prediction-v3"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w batch_ml_training_demo -s COMPLETED -c 5
```

## How to Extend

Each worker encapsulates one ML lifecycle step .  replace the simulated training calls with real SageMaker or MLflow APIs and the parallel-train-then-evaluate pipeline runs unchanged.

- **BmlPrepareDataWorker** (`bml_prepare_data`): load real datasets from S3, BigQuery, or a feature store; run cleaning and feature engineering with Apache Spark or pandas via a subprocess
- **BmlTrainModel1Worker / BmlTrainModel2Worker**: invoke SageMaker training jobs, submit MLflow runs, or call scikit-learn/XGBoost via a Python subprocess to train on actual data
- **BmlEvaluateWorker** (`bml_evaluate`): compute real metrics (AUC, F1, RMSE) against the test set and log results to MLflow or Weights & Biases for experiment tracking

The interface contract stays fixed. Swap in a real Spark data loader or a SageMaker training call and the parallel evaluation pipeline runs unchanged.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
batch-ml-training/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/batchmltraining/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BatchMlTrainingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BmlEvaluateWorker.java
│       ├── BmlPrepareDataWorker.java
│       ├── BmlSplitDataWorker.java
│       ├── BmlTrainModel1Worker.java
│       └── BmlTrainModel2Worker.java
└── src/test/java/batchmltraining/workers/
    ├── BmlEvaluateWorkerTest.java        # 4 tests
    ├── BmlPrepareDataWorkerTest.java        # 4 tests
    ├── BmlSplitDataWorkerTest.java        # 4 tests
    ├── BmlTrainModel1WorkerTest.java        # 4 tests
    └── BmlTrainModel2WorkerTest.java        # 4 tests
```
