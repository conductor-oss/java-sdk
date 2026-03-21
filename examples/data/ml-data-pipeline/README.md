# ML Data Pipeline in Java Using Conductor :  Data Collection, Cleaning, Train/Test Split, Model Training, and Evaluation

A Java Conductor workflow example for an end-to-end ML training pipeline: collecting labeled data from a source, cleaning it (removing records with null features or invalid labels), splitting into train and test sets at a configurable ratio, training a model (e.g., random forest) on the training set, and evaluating accuracy and metrics against the held-out test set. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Training a model requires a strict sequence of data preparation steps, and each depends on the output of the previous one. You collect labeled records from a data source, but some have null features or malformed labels that would corrupt training. You clean those out, but then you need to split the surviving records into train and test sets at a specific ratio (say, 80/20), and the split must happen after cleaning, not before, or your test set contains dirty data. You train the model (a random forest classifier, for example) on the training partition, but evaluation must use the held-out test partition from the same split. Not a different random sample. If training crashes after 30 minutes of GPU time, you don't want to re-collect and re-clean the data from scratch.

Without orchestration, you'd write a Jupyter notebook or a monolithic training script that collects, cleans, splits, trains, and evaluates in one run. If the data source is temporarily unavailable, the entire pipeline fails with no retry. There's no record of how many records were collected vs: cleaned vs: used for training, making it impossible to debug why accuracy dropped. When you want to experiment with a different split ratio or model type, you'd modify deeply coupled code with no visibility into whether the data pipeline or the model itself is the bottleneck.

## The Solution

**You just write the data collection, cleaning, train/test splitting, model training, and evaluation workers. Conductor handles strict sequencing so training uses only cleaned data, retries when data sources are unavailable, and record count tracking at every stage for experiment reproducibility.**

Each stage of the ML pipeline is a simple, independent worker. The data collector fetches labeled records from the configured data source. The cleaner removes records with null features or invalid labels and reports how many survived. The splitter divides clean data into train and test partitions at the configured ratio (e.g., 0.8 for 80/20). The trainer fits the specified model type (random forest, logistic regression, gradient boosting) on the training partition, producing a trained model artifact and a training loss metric. The evaluator runs the trained model against the held-out test set and computes accuracy, precision, recall, and F1 scores. Conductor executes them in strict sequence, passes the evolving dataset between stages, retries if the data source is temporarily unavailable, and tracks record counts at every stage. So you can see exactly how many raw records became clean records became training samples became evaluation metrics. You get all of that, without writing a single line of orchestration code.

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/ml-data-pipeline-1.0.0.jar

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
java -jar target/ml-data-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ml_data_pipeline \
  --version 1 \
  --input '{"dataSource": "api", "modelType": "standard", "splitRatio": "sample-splitRatio"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ml_data_pipeline -s COMPLETED -c 5

```

## How to Extend

Connect the collector to your training data lake, train with scikit-learn or TensorFlow, and evaluate against held-out test sets, the ML training pipeline workflow runs unchanged.

- **CollectDataWorker** → read from real data sources: feature stores (Feast, Tecton), data warehouses (Snowflake, BigQuery), labeled dataset registries (Label Studio, Labelbox), or S3/GCS Parquet files
- **CleanDataWorker** → implement real data cleaning: outlier removal (IQR or z-score), missing value imputation (mean, median, KNN), label correction, and class imbalance handling (SMOTE, undersampling)
- **SplitDataWorker** → use stratified splitting that preserves class distribution across train/test sets, support k-fold cross-validation splits, or implement time-based splits for time-series data
- **TrainModelWorker** → train real models using scikit-learn (via Jep), Tribuo, DJL (Deep Java Library), or call external training services (SageMaker, Vertex AI), with hyperparameter tuning via grid search or Optuna
- **EvaluateModelWorker** → compute real evaluation metrics (confusion matrix, ROC-AUC, precision-recall curves), generate classification reports, and log results to experiment tracking systems (MLflow, Weights & Biases)

Changing the model type or adjusting the train/test split ratio inside the workers does not affect the collect-clean-split-train-evaluate pipeline, as long as each outputs the expected record counts and metrics.

**Add new stages** by inserting tasks in `workflow.json`, for example, a feature engineering step between clean and split, a hyperparameter search step that runs multiple training configurations in parallel via FORK_JOIN, or a model registry step that versions and stores the trained model artifact for deployment.

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
ml-data-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mldatapipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MlDataPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CleanDataWorker.java
│       ├── CollectDataWorker.java
│       ├── EvaluateModelWorker.java
│       ├── SplitDataWorker.java
│       └── TrainModelWorker.java
└── src/test/java/mldatapipeline/workers/
    ├── CleanDataWorkerTest.java        # 8 tests
    ├── CollectDataWorkerTest.java        # 8 tests
    ├── EvaluateModelWorkerTest.java        # 8 tests
    ├── SplitDataWorkerTest.java        # 8 tests
    └── TrainModelWorkerTest.java        # 8 tests

```
