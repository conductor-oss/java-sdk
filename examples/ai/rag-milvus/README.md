# RAG with Milvus in Java Using Conductor :  Embed, Search Vectors, Generate

A Java Conductor workflow that implements RAG using Milvus as the vector database. embedding the question, searching a Milvus collection for similar vectors, and generating an answer from the retrieved documents. Milvus is purpose-built for vector similarity search at scale, supporting billions of vectors with GPU-accelerated indexing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, Milvus vector search, and generation as independent workers,  you write the Milvus integration, Conductor handles sequencing, retries, durability, and observability.

## RAG at Scale with Milvus

Milvus is designed for large-scale vector search. it handles billions of vectors, supports multiple index types (IVF, HNSW, DiskANN), and provides attribute filtering alongside vector similarity. A RAG pipeline against Milvus embeds the question, searches the collection using Milvus's optimized ANN search, and generates from the results.

Each step can fail independently: the embedding model might time out, the Milvus cluster might be compacting, or the LLM might be rate-limited.

## The Solution

**You write the embedding and Milvus vector search logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, Milvus collection search, and answer generation. Conductor sequences them, retries the Milvus query during cluster compaction, and tracks every search with the question, collection, retrieved vectors, and generated answer.

### What You Write: Workers

Three workers integrate Milvus into the RAG pipeline. embedding the query, performing approximate nearest neighbor search at scale against a Milvus collection, and generating an answer from the retrieved vectors.

| Worker | Task | What It Does |
|---|---|---|
| **MilvusEmbedWorker** | `milvus_embed` | Worker that generates a fixed embedding vector for a question. In production this would call an embedding API (e.g. O... |
| **MilvusGenerateWorker** | `milvus_generate` | Worker that generates an answer from a question and retrieved Milvus search results. Builds a deterministic answer su... |
| **MilvusSearchWorker** | `milvus_search` | Worker that searches a Milvus collection with an embedding vector. Returns fixed results for deterministic behavior. ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
milvus_embed
    │
    ▼
milvus_search
    │
    ▼
milvus_generate

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
java -jar target/rag-milvus-1.0.0.jar

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
java -jar target/rag-milvus-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_milvus_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "collection": "sample-collection"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_milvus_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one RAG stage. swap in a real embedding model, connect to a Milvus cluster with IVF or HNSW indexing for billion-scale vector search, and the embed-search-generate pipeline runs unchanged.

- **MilvusEmbedWorker** (`milvus_embed`): call an embedding API (OpenAI text-embedding-ada-002, Cohere embed-english-v3) to vectorize the question for Milvus search
- **MilvusGenerateWorker** (`milvus_generate`): send Milvus search results as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer
- **MilvusSearchWorker** (`milvus_search`): query a real Milvus collection using the PyMilvus or Java SDK with configurable index type (IVF_FLAT, HNSW), metric (IP, L2, cosine), and metadata filters

The embed/search/generate contract stays fixed. switch index types (IVF, HNSW), adjust search parameters, or swap embedding models without modifying the workflow.

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
rag-milvus/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragmilvus/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagMilvusExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MilvusEmbedWorker.java
│       ├── MilvusGenerateWorker.java
│       └── MilvusSearchWorker.java
└── src/test/java/ragmilvus/workers/
    ├── MilvusEmbedWorkerTest.java        # 5 tests
    ├── MilvusGenerateWorkerTest.java        # 5 tests
    └── MilvusSearchWorkerTest.java        # 7 tests

```
