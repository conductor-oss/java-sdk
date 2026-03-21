# AI Model Evaluation in Java Using Conductor :  Load Model, Prepare Test Set, Run Inference, Compute Metrics, Report

A Java Conductor workflow that evaluates a machine learning model end-to-end. loading the model artifacts, preparing the test dataset, running inference on all test samples, computing evaluation metrics (accuracy, F1, precision, recall, latency), and generating a comprehensive evaluation report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage evaluation pipeline as independent workers,  you write the evaluation logic, Conductor handles sequencing, retries, durability, and observability.

## Model Evaluation Must Be Systematic and Reproducible

Evaluating a model by running a few test queries manually tells you nothing about its actual performance. Systematic evaluation requires loading the exact model version, running inference on a standardized test set (not cherry-picked examples), computing metrics that match the task type (accuracy for classification, BLEU for translation, ROUGE for summarization), and generating a report that enables comparison across model versions.

Each step must be reproducible: the same model version, the same test set, the same metrics computation. If inference fails mid-batch (out of memory, model crash), you need to retry without reloading the model or re-preparing the test set. And the report must include not just aggregate metrics but per-sample analysis to identify systematic failure patterns.

## The Solution

**You just write the model loading, test set preparation, batch inference, metrics computation, and evaluation reporting logic. Conductor handles inference retries, metric aggregation sequencing, and full evaluation audit trails.**

`LoadModelWorker` loads the model artifacts (weights, config, tokenizer) and verifies integrity. `PrepareTestSetWorker` loads and preprocesses the test dataset. applying the same transformations used during training. `RunInferenceWorker` runs the model on all test samples and collects predictions with confidence scores. `ComputeMetricsWorker` calculates task-appropriate metrics,  accuracy, precision, recall, F1, confusion matrix for classification; BLEU, ROUGE for generation; latency percentiles for performance. `ReportWorker` generates the evaluation report with metric summaries, per-class breakdowns, and failure analysis. Conductor tracks each evaluation run for model comparison over time.

### What You Write: Workers

Each evaluation stage: model loading, test preparation, inference, metric computation, and reporting, runs as its own worker with no shared state between steps.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeMetricsWorker** | `ame_compute_metrics` | Computes evaluation metrics from predictions and ground truth. accuracy (0.937), F1 (0.929), and AUC (0.981) |
| **LoadModelWorker** | `ame_load_model` | Loads the model artifacts (500M parameters) and returns the inference endpoint URL |
| **ReportWorker** | `ame_report` | Generates a comprehensive evaluation report with metric summaries and model comparison data |
| **RunInferenceWorker** | `ame_run_inference` | Runs batch inference on test samples, returning predictions with P50 (12ms) and P99 (45ms) latency metrics |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
ame_load_model
    │
    ▼
ame_prepare_test_set
    │
    ▼
ame_run_inference
    │
    ▼
ame_compute_metrics
    │
    ▼
ame_report

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
java -jar target/ai-model-evaluation-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live AI evaluation (optional. falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-model-evaluation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ame_model_evaluation \
  --version 1 \
  --input '{"modelId": "TEST-001", "testDatasetId": "TEST-001", "taskType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ame_model_evaluation -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real ML infrastructure. SageMaker or Vertex AI for inference, scikit-learn or DeepEval for metrics, MLflow or Weights & Biases for experiment tracking and reporting, and the workflow runs identically in production.

- **RunInferenceWorker** (`ame_run_inference`): use ONNX Runtime or TorchServe for model serving, or call cloud inference endpoints (SageMaker, Vertex AI) for GPU-accelerated batch inference
- **ComputeMetricsWorker** (`ame_compute_metrics`): integrate with scikit-learn metrics (via Jython or subprocess), DeepEval for LLM-specific metrics, or custom domain metrics for specialized tasks
- **ReportWorker** (`ame_report`): generate HTML/PDF reports with charts (matplotlib via subprocess), push results to MLflow or Weights & Biases for experiment tracking and model comparison

Change your inference runtime or metrics library and the evaluation pipeline adapts without restructuring.

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
ai-model-evaluation-ai-model-evaluation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aimodelevaluation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiModelEvaluationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComputeMetricsWorker.java
│       ├── LoadModelWorker.java
│       ├── ReportWorker.java
│       └── RunInferenceWorker.java
└── src/test/java/aimodelevaluation/workers/
    ├── LoadModelWorkerTest.java        # 1 tests
    └── ReportWorkerTest.java        # 1 tests

```
