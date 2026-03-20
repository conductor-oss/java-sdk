# AI Fine-Tuning Pipeline in Java Using Conductor :  Prepare Dataset, Configure, Train, Evaluate, Deploy

A Java Conductor workflow that orchestrates model fine-tuning end-to-end .  preparing the training dataset (formatting, splitting, validation), configuring hyperparameters, running the training job, evaluating the fine-tuned model against the base model, and deploying if the evaluation passes. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage fine-tuning pipeline as independent workers ,  you write the ML pipeline logic, Conductor handles sequencing, retries, durability, and observability.

## Fine-Tuning Is a Pipeline, Not a Single Step

Fine-tuning an LLM on your domain data involves five distinct stages, each with different requirements and failure modes. Dataset preparation (formatting to chat/instruction format, train/val split, deduplication) is CPU-bound. Configuration (learning rate, batch size, LoRA rank, epochs) requires domain knowledge. Training is GPU-bound and can take hours. Evaluation compares the fine-tuned model against the base model on held-out data. Deployment makes the model available for inference.

If training crashes after 3 hours (GPU out of memory, training divergence), you need to adjust configuration and restart training .  not re-prepare the dataset. If evaluation shows the fine-tuned model is worse than the base model (catastrophic forgetting), you need to try different hyperparameters. Each stage needs independent retry and tracking.

## The Solution

**You just write the dataset preparation, hyperparameter configuration, training execution, model evaluation, and deployment logic. Conductor handles training checkpointing, evaluation sequencing, and deployment rollback tracking.**

`PrepareDatasetWorker` formats the raw data into the training format (instruction/response pairs, chat turns), performs train/validation split, and validates quality. `ConfigureWorker` sets hyperparameters (learning rate, batch size, epochs, LoRA rank) based on the task type and base model. `TrainWorker` executes the fine-tuning job with the prepared data and configuration. `EvaluateWorker` compares the fine-tuned model against the base model on held-out validation data, computing task-specific metrics. `DeployWorker` deploys the fine-tuned model to a serving endpoint if evaluation passes quality gates. Conductor tracks each training run with its hyperparameters and evaluation results for experiment management.

### What You Write: Workers

The fine-tuning pipeline uses separate workers for dataset prep, hyperparameter config, training, evaluation, and deployment. Swap any step without affecting the rest.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureWorker** | `aft_configure` | Configures and returns config, learning rate, epochs |
| **DeployWorker** | `aft_deploy` | Deploys the fine-tuned model to a production endpoint with 3 replicas for serving |
| **EvaluateWorker** | `aft_evaluate` | Evaluates the training checkpoint on the validation set .  accuracy: 0.952, determines if quality threshold is met |
| **PrepareDatasetWorker** | `aft_prepare_dataset` | Prepares the training dataset .  formats 10K samples with 80/20 train/validation split |
| **TrainWorker** | `aft_train` | Trains the input and returns model id, checkpoint id, final loss, training time |

Workers simulate AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode .  the generation workflow stays the same.

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
java -jar target/ai-fine-tuning-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live AI evaluation (optional .  falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-fine-tuning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow aft_fine_tuning \
  --version 1 \
  --input '{"baseModel": "test-value", "datasetId": "TEST-001", "taskType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w aft_fine_tuning -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real ML platform. OpenAI's fine-tuning API or HuggingFace Transformers for training, SageMaker or Vertex AI for managed jobs, vLLM or Ollama for deployment, and the workflow runs identically in production.

- **TrainWorker** (`aft_train`): submit fine-tuning jobs to OpenAI's fine-tuning API, AWS SageMaker Training Jobs, or run locally with Hugging Face Transformers and PEFT (LoRA)
- **EvaluateWorker** (`aft_evaluate`): implement automated evaluation: MMLU for general knowledge, domain-specific benchmarks for specialized tasks, and A/B testing against the base model
- **DeployWorker** (`aft_deploy`): deploy to vLLM for efficient inference, SageMaker endpoints for managed serving, or Ollama for local deployment of fine-tuned models

Point the training worker at a new framework or cloud GPU provider without touching dataset prep or deployment.

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
ai-fine-tuning-ai-fine-tuning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aifinetuning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiFineTuningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfigureWorker.java
│       ├── DeployWorker.java
│       ├── EvaluateWorker.java
│       ├── PrepareDatasetWorker.java
│       └── TrainWorker.java
└── src/test/java/aifinetuning/workers/
    ├── DeployWorkerTest.java        # 1 tests
    └── PrepareDatasetWorkerTest.java        # 1 tests
```
