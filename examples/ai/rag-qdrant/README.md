# RAG with Qdrant in Java Using Conductor :  Vector Search with Payload Filtering

A Java Conductor workflow that implements RAG using Qdrant .  embedding the question, searching a Qdrant collection with vector similarity and payload filtering (filtering by metadata fields like category, date, or access level alongside vector distance), and generating an answer from the results. Qdrant is an open-source vector database with rich payload filtering and gRPC/REST APIs. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, Qdrant search, and generation as independent workers ,  you write the Qdrant integration, Conductor handles sequencing, retries, durability, and observability.

## RAG with Payload-Filtered Vector Search

Qdrant distinguishes itself with flexible payload filtering .  you can combine vector similarity with structured filters on metadata fields. A query can find the most similar vectors that are also in category "engineering" and were created after "2024-01-01." This is essential for access-controlled or scoped RAG where not all documents should be searchable by all users.

The pipeline embeds the question, searches Qdrant with both the vector and payload filters, and generates from the filtered results.

## The Solution

**You write the embedding and Qdrant payload-filtered search logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker .  question embedding, Qdrant collection search (with payload filtering), and answer generation. Conductor sequences them, retries the Qdrant query if the server is temporarily unavailable, and tracks every search with the question, collection, filters, and results.

### What You Write: Workers

Three workers integrate Qdrant into the RAG pipeline .  embedding the query, searching a Qdrant collection with payload filtering and configurable score thresholds, and generating an answer from the matched points.

| Worker | Task | What It Does |
|---|---|---|
| **QdrantEmbedWorker** | `qdrant_embed` | Worker that produces a fixed embedding vector for a given question. In production this would call an embedding API (e... |
| **QdrantGenerateWorker** | `qdrant_generate` | Worker that generates an answer from a question and retrieved Qdrant points. Builds a deterministic answer by combini... |
| **QdrantSearchWorker** | `qdrant_search` | Worker that searches a Qdrant collection with an embedding vector. Returns fixed points with payload data for determi... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
qdrant_embed
    │
    ▼
qdrant_search
    │
    ▼
qdrant_generate
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
java -jar target/rag-qdrant-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for embeddings and generation. When absent, workers use simulated responses. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-qdrant-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_qdrant_workflow \
  --version 1 \
  --input '{"question": "test-value", "collection": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_qdrant_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one RAG stage .  swap in a real embedding model, search a Qdrant collection with payload filters for scoped access, and the embed-search-generate pipeline runs unchanged.

- **QdrantEmbedWorker** (`qdrant_embed`): call an embedding API (OpenAI text-embedding-ada-002, Cohere embed-english-v3) to vectorize the question for Qdrant search
- **QdrantGenerateWorker** (`qdrant_generate`): send Qdrant search results as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer
- **QdrantSearchWorker** (`qdrant_search`): POST to the real Qdrant `/collections/{collection}/points/search` endpoint with vector, limit, score_threshold, and payload filters

The embed/search/generate contract stays fixed .  adjust payload filters, switch HNSW parameters, or swap embedding models without modifying the workflow.

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
rag-qdrant/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragqdrant/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagQdrantExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── QdrantEmbedWorker.java
│       ├── QdrantGenerateWorker.java
│       └── QdrantSearchWorker.java
└── src/test/java/ragqdrant/workers/
    ├── QdrantEmbedWorkerTest.java        # 5 tests
    ├── QdrantGenerateWorkerTest.java        # 6 tests
    └── QdrantSearchWorkerTest.java        # 6 tests
```
