# RAG Embedding Selection in Java Using Conductor :  Benchmark OpenAI, Cohere, and Local Models in Parallel

A Java Conductor workflow that benchmarks three embedding providers (OpenAI, Cohere, and a local model) against the same test data in parallel, evaluates each on quality and latency metrics, and selects the best model for your use case. Conductor's `FORK_JOIN` runs all three embeddings simultaneously so the benchmark completes in the time of the slowest provider, not the sum. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate parallel benchmarking, evaluation, and selection as independent workers.  you write the embedding and evaluation logic, Conductor handles parallelism, retries, durability, and observability.

## Choosing the Right Embedding Model

Different embedding models produce different quality results for different domains. OpenAI's `text-embedding-3-large` might excel at general knowledge but underperform on technical documentation. Cohere's embeddings might be faster but less accurate for your specific corpus. A local sentence-transformers model might match cloud quality at zero per-query cost. You won't know until you benchmark them side-by-side on your actual data.

The benchmark prepares test queries and documents, embeds them with all three providers in parallel, evaluates each on retrieval quality metrics (recall, MRR) and performance (latency, throughput), and selects the winner.

## The Solution

**You write the embedding calls and evaluation metrics. Conductor handles the parallel benchmarking, retries, and observability.**

Each embedding provider is an independent worker. OpenAI, Cohere, local. Conductor's `FORK_JOIN` runs all three in parallel. An evaluation worker scores each provider's embeddings, and a selection worker picks the best. Every benchmark run records all metrics for all providers, building a dataset for model selection decisions.

### What You Write: Workers

Six workers benchmark embedding providers. preparing test data, running OpenAI, Cohere, and local embeddings in parallel via FORK_JOIN, evaluating each on accuracy and latency, and selecting the best performer for production use.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedCohereWorker** | `es_embed_cohere` | Worker that simulates embedding evaluation using Cohere embed-english-v3.0. Returns pre-computed metrics for the benc... |
| **EmbedLocalWorker** | `es_embed_local` | Worker that simulates embedding evaluation using a local all-MiniLM-L6-v2 model. Returns pre-computed metrics for the... |
| **EmbedOpenaiWorker** | `es_embed_openai` | Worker that simulates embedding evaluation using OpenAI text-embedding-3-large. Returns pre-computed metrics for the ... |
| **EvaluateEmbeddingsWorker** | `es_evaluate_embeddings` | Worker that evaluates embedding metrics from all providers. Computes a composite score for each model and determines ... |
| **PrepareBenchmarkWorker** | `es_prepare_benchmark` | Worker that prepares a benchmark dataset for embedding evaluation. Returns benchmark queries with expected document I... |
| **SelectBestWorker** | `es_select_best` | Worker that selects the best embedding model based on evaluation rankings. Returns the best model, its score, and a r... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
es_prepare_benchmark
    │
    ▼
FORK_JOIN
    ├── es_embed_openai
    ├── es_embed_cohere
    └── es_embed_local
    │
    ▼
JOIN (wait for all branches)
es_evaluate_embeddings
    │
    ▼
es_select_best

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
java -jar target/rag-embedding-selection-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for embeddings and generation. When absent, workers use demo responses. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-embedding-selection-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_embedding_selection \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_embedding_selection -s COMPLETED -c 5

```

## How to Extend

Each worker benchmarks one embedding provider. swap in real OpenAI, Cohere, and local sentence-transformers calls, add retrieval quality metrics like MRR, and the parallel benchmark-evaluate-select workflow runs unchanged.

- **PrepareBenchmarkWorker** (`es_prepare_benchmark`): load benchmark datasets (MTEB, BEIR, or custom domain-specific query/document pairs) with ground truth relevance judgments
- **EmbedOpenaiWorker** (`es_embed_openai`): call the real OpenAI Embeddings API (text-embedding-3-large) and compute NDCG, recall, and precision against the benchmark
- **EmbedCohereWorker** (`es_embed_cohere`): call the real Cohere Embed API (embed-english-v3.0) with input_type='search_query' and compute retrieval metrics against the benchmark
- **EmbedLocalWorker** (`es_embed_local`): run a local embedding model (all-MiniLM-L6-v2 via ONNX Runtime or Sentence Transformers) and compute retrieval metrics against the benchmark
- **EvaluateEmbeddingsWorker** (`es_evaluate_embeddings`): compute composite scores weighting quality (NDCG, recall), latency, and cost-per-query to recommend the best embedding model for your use case
- **SelectBestWorker** (`es_select_best`): output a deployment recommendation with the winning model, its retrieval quality metrics, latency percentiles, and estimated monthly cost

Each embedding worker returns the same vector/timing shape, so adding new providers to the benchmark requires only a new worker and fork branch. the evaluation and selection logic stays unchanged.

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
rag-embedding-selection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragembeddingselection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagEmbeddingSelectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmbedCohereWorker.java
│       ├── EmbedLocalWorker.java
│       ├── EmbedOpenaiWorker.java
│       ├── EvaluateEmbeddingsWorker.java
│       ├── PrepareBenchmarkWorker.java
│       └── SelectBestWorker.java
└── src/test/java/ragembeddingselection/workers/
    ├── EmbedCohereWorkerTest.java        # 2 tests
    ├── EmbedLocalWorkerTest.java        # 2 tests
    ├── EmbedOpenaiWorkerTest.java        # 2 tests
    ├── EvaluateEmbeddingsWorkerTest.java        # 3 tests
    ├── PrepareBenchmarkWorkerTest.java        # 4 tests
    └── SelectBestWorkerTest.java        # 3 tests

```
