# RAG Reranking in Java Using Conductor :  Cross-Encoder Reranking Between Retrieval and Generation

A Java Conductor workflow that adds a cross-encoder reranking step between retrieval and generation. retrieving a broad set of candidate documents (e.g., top-K=20), scoring each document-question pair with a cross-encoder model that sees both the question and document together, selecting the top-N most relevant, and generating from the reranked subset. Cross-encoders are more accurate than bi-encoder embeddings but too slow for initial retrieval. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, broad retrieval, cross-encoder reranking, and generation as independent workers,  you write the reranking logic, Conductor handles sequencing, retries, durability, and observability.

## Why Retrieve More, Then Rerank

Bi-encoder embeddings are fast (compare pre-computed vectors) but approximate. the top-5 results from a vector search aren't always the 5 most relevant documents. Cross-encoders are more accurate (they see question and document together) but expensive (they can't pre-compute). The solution: retrieve broadly (top 20 by vector similarity), then rerank precisely (cross-encoder scores each pair), and keep only the top 5 for generation.

This two-stage approach gives you cross-encoder accuracy at vector search speed.

## The Solution

**You write the broad retrieval and cross-encoder reranking logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each stage is an independent worker. question embedding, broad retrieval (high K), cross-encoder reranking (scoring and filtering to top N), and generation from the reranked results. Conductor sequences them, retries the cross-encoder if the model service is overloaded, and tracks the full retrieval-to-generation path.

### What You Write: Workers

Four workers implement the reranking pattern. embedding the query, retrieving a broad initial set of candidates, reranking with a cross-encoder model for precision, and generating an answer from the top reranked results.

| Worker | Task | What It Does |
|---|---|---|
| **CrossEncoderWorker** | `rerank_crossencoder` | Worker that re-ranks candidates using a cross-encoder model. Simulates cross-encoder scoring where order changes sign... |
| **EmbedWorker** | `rerank_embed` | Worker that embeds a question into a vector representation using OpenAI text-embedding-3-small. |
| **GenerateWorker** | `rerank_generate` | Worker that generates an answer using re-ranked context documents. |
| **RetrieveWorker** | `rerank_retrieve` | Worker that retrieves a broad set of candidates from a vector store. Returns 6 candidates with bi-encoder similarity ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
rerank_embed
    │
    ▼
rerank_retrieve
    │
    ▼
rerank_crossencoder
    │
    ▼
rerank_generate

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
java -jar target/rag-reranking-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, EmbedWorker calls text-embedding-3-small and GenerateWorker calls gpt-4o-mini instead of using demo output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-reranking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_reranking_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "retrieveK": "sample-retrieveK", "rerankTopN": "sample-rerankTopN"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_reranking_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one retrieval stage. swap in a real vector store for broad top-K retrieval, integrate Cohere Rerank or a cross-encoder model for precise reranking, and the retrieve-rerank-generate pipeline runs unchanged.

- **EmbedWorker** (`rerank_embed`): call an embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) to vectorize the question for initial bi-encoder retrieval
- **RetrieveWorker** (`rerank_retrieve`): query a vector database (Pinecone, Weaviate, pgvector) to retrieve a broad set of candidate documents with bi-encoder scores
- **CrossEncoderWorker** (`rerank_crossencoder`): call the Cohere Rerank API or run a local cross-encoder model (ms-marco-MiniLM-L-6-v2) to re-score candidates with query-document pair attention
- **GenerateWorker** (`rerank_generate`): send the top re-ranked documents as context to an LLM (OpenAI GPT-4, Anthropic Claude) for higher-quality answer generation

The embed/retrieve/rerank/generate contract is fixed. swap the cross-encoder model, adjust the candidate pool size, or change the LLM provider without modifying the workflow.

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
rag-reranking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragreranking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagRerankingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CrossEncoderWorker.java
│       ├── EmbedWorker.java
│       ├── GenerateWorker.java
│       └── RetrieveWorker.java
└── src/test/java/ragreranking/workers/
    ├── CrossEncoderWorkerTest.java        # 8 tests
    ├── EmbedWorkerTest.java        # 4 tests
    ├── GenerateWorkerTest.java        # 5 tests
    └── RetrieveWorkerTest.java        # 6 tests

```
