# Hugging Face Inference in Java Using Conductor :  Task-Based Model Selection and API Orchestration

A Java Conductor workflow that routes NLP tasks (summarization, text generation, sentiment analysis) to the appropriate Hugging Face model, calls the Inference API, and formats the task-specific output. You specify a task type and input text; the workflow selects the right model (e.g., `facebook/bart-large-cnn` for summarization, `gpt2` for text generation), calls it, and returns a formatted result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate model selection, inference, and formatting as independent workers.  you write the task-routing and API logic, Conductor handles retries, durability, and observability.

## Multi-Task NLP with a Single Workflow

Different NLP tasks require different models with different input formats and output structures. Summarization uses BART and returns a summary string. Text generation uses GPT-2 and returns a continuation. Sentiment analysis uses a classifier and returns labels with scores. Each model has its own API parameters (`min_length` for summarization, `max_new_tokens` for generation) and its own output format.

When you hardcode a single model call, adding a new task type means duplicating the entire pipeline. When you handle model selection and inference in the same method, a Hugging Face API timeout means re-running the model selection logic. And without tracking which model was used for which request, you can't debug why a particular summarization was poor.

## The Solution

**You write the model selection, inference call, and result formatting logic. Conductor handles the task routing, retries, and observability.**

Each concern is an independent worker. model selection (mapping task types to Hugging Face model IDs and parameters), inference (calling the Hugging Face Inference API), and result formatting (extracting the relevant field from the model's output based on task type). Conductor chains them so the selected model feeds into the inference call, and the raw output feeds into the task-specific formatter. If the Inference API is rate-limited or a model is loading, Conductor retries automatically. Every execution records which model handled which task.

### What You Write: Workers

Three workers cover task-based model routing. selecting the right Hugging Face model for the task (summarization, generation, or sentiment), running inference, and formatting the task-specific result.

| Worker | Task | What It Does |
|---|---|---|
| **HfFormatResultWorker** | `hf_format_result` | Formats the raw inference output based on the task type. Extracts the relevant field from the model output (e.g, summ... |
| **HfInferenceWorker** | `hf_inference` | Simulates a call to the Hugging Face Inference API. In production, this would POST to https://api-inference.huggingfa... |
| **HfSelectModelWorker** | `hf_select_model` | Selects the appropriate Hugging Face model based on the requested task type. Maps task types (summarization, text-gen... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
hf_select_model
    │
    ▼
hf_inference
    │
    ▼
hf_format_result

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
java -jar target/huggingface-1.0.0.jar

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
| `HUGGINGFACE_TOKEN` | _(none)_ | HuggingFace API token. When set, `HfInferenceWorker` calls the real HuggingFace Inference API. When unset, returns simulated responses with `[SIMULATED]` prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/huggingface-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow huggingface_inference_workflow \
  --version 1 \
  --input '{"text": "Process this order for customer C-100", "task": "sample-task"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w huggingface_inference_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one NLP pipeline concern. swap in real Hugging Face Inference API calls for summarization, generation, or classification, extend the task-to-model mapping, and the select-infer-format workflow runs unchanged.

- **HfSelectModelWorker** (`hf_select_model`): extend the task-to-model mapping with additional task types (translation, NER, question-answering) or use the Hugging Face Hub API to find the best model dynamically
- **HfInferenceWorker** (`hf_inference`): swap in a real HTTP call to `https://api-inference.huggingface.co/models/{modelId}` with your API token
- **HfFormatResultWorker** (`hf_format_result`): add task-specific post-processing like confidence thresholds for classification or length filtering for generation

Each worker preserves its task/result contract, so swapping models, adding new task types, or switching from the Inference API to a self-hosted endpoint requires no workflow changes.

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
huggingface/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/huggingface/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HuggingfaceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HfFormatResultWorker.java
│       ├── HfInferenceWorker.java
│       └── HfSelectModelWorker.java
└── src/test/java/huggingface/workers/
    ├── HfFormatResultWorkerTest.java        # 6 tests
    ├── HfInferenceWorkerTest.java        # 3 tests
    └── HfSelectModelWorkerTest.java        # 8 tests

```
