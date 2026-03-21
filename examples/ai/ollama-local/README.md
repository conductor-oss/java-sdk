# Ollama Local in Java Using Conductor :  Code Review via Locally-Hosted LLMs

A Java Conductor workflow that runs code review through a locally-hosted Ollama model. checking that the requested model is available, generating a code review, and post-processing the raw output into structured feedback. No API keys, no cloud calls, no data leaving your machine. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate model availability checks, local LLM generation, and output formatting as independent workers,  you write the Ollama integration, Conductor handles sequencing, retries, durability, and observability.

## Running LLMs Locally for Code Review

Cloud-hosted LLMs require API keys, cost money per token, and send your code to a third party. For code review on proprietary codebases, that's often a non-starter. Ollama lets you run models like CodeLlama, Mistral, and Llama 2 entirely on your local machine; but you need to verify the model is actually downloaded and loaded before calling it, handle generation timeouts (local inference is slower), and post-process the raw output into structured review feedback.

Without orchestration, a model-not-found error means a cryptic failure, a generation timeout means lost work, and there's no record of which model version reviewed which code.

## The Solution

**You write the Ollama model check, local generation call, and review formatting logic. Conductor handles the sequencing, retries, and observability.**

Each step is an independent worker. model availability check (is the requested model loaded in Ollama?), local generation (calling Ollama's `/api/generate` endpoint), and post-processing (structuring raw output into review comments). Conductor sequences them, retries the generation if Ollama's local inference times out, and tracks every review with the model used, the code reviewed, and the structured feedback produced.

### What You Write: Workers

Three workers manage local model inference. checking that the Ollama model is loaded and ready, generating a response locally, and post-processing the output with token counts and latency metrics.

| Worker | Task | What It Does |
|---|---|---|
| **OllamaCheckModelWorker** | `ollama_check_model` | Worker that verifies an Ollama model is available (simulated). Takes model and ollamaHost, returns resolvedModel and ... |
| **OllamaGenerateWorker** | `ollama_generate` | Worker that simulates Ollama text generation. Takes prompt, model, ollamaHost, and options. Returns a fixed response ... |
| **OllamaPostProcessWorker** | `ollama_post_process` | Worker that post-processes the Ollama generation response. Takes response and wraps it in a review field. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ollama_check_model
    │
    ▼
ollama_generate
    │
    ▼
ollama_post_process

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
java -jar target/ollama-local-1.0.0.jar

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
| `OLLAMA_HOST` | `localhost:11434` | Ollama server host. When Ollama is running, `OllamaGenerateWorker` calls the real `/api/generate` endpoint. When Ollama is not reachable, returns simulated responses with `[SIMULATED]` prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ollama-local-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ollama_local_workflow \
  --version 1 \
  --input '{"prompt": "What is workflow orchestration?", "model": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ollama_local_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one step of the local inference pipeline. swap in real Ollama `/api/generate` calls with streaming support, add model auto-download via `/api/pull`, and the check-generate-format workflow runs unchanged.

- **OllamaCheckModelWorker** (`ollama_check_model`): call the Ollama `/api/tags` or `/api/show` endpoint to verify model availability, and `/api/pull` to auto-download missing models
- **OllamaGenerateWorker** (`ollama_generate`): call the real Ollama `/api/generate` endpoint with streaming support, configurable temperature/top_p/repeat_penalty, and token-level callbacks for progressive output
- **OllamaPostProcessWorker** (`ollama_post_process`): add response cleaning (strip special tokens), format conversion (Markdown to HTML), or structured extraction (JSON mode parsing) on the raw Ollama output

The check/generate/post-process contract stays fixed. swap models, adjust generation parameters, or add output filtering without changing the workflow definition.

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
ollama-local/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ollamalocal/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OllamaLocalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── OllamaCheckModelWorker.java
│       ├── OllamaGenerateWorker.java
│       └── OllamaPostProcessWorker.java
└── src/test/java/ollamalocal/workers/
    ├── OllamaCheckModelWorkerTest.java        # 5 tests
    ├── OllamaGenerateWorkerTest.java        # 7 tests
    └── OllamaPostProcessWorkerTest.java        # 5 tests

```
