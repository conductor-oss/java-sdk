# RAG with Elasticsearch in Java Using Conductor :  Dense Vector kNN Search and Generation

A Java Conductor workflow that implements RAG using Elasticsearch's native dense vector search (kNN) .  embedding the question, running a kNN query against an Elasticsearch index with dense_vector fields, and generating an answer from the retrieved documents. Elasticsearch's kNN search uses HNSW graphs for approximate nearest neighbor lookup, combining the speed of vector search with Elasticsearch's existing filtering and scoring capabilities. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, kNN search, and generation as independent workers ,  you write the Elasticsearch integration, Conductor handles sequencing, retries, durability, and observability.

## RAG on Your Existing Elasticsearch Cluster

If you already run Elasticsearch for full-text search, you don't need a separate vector database for RAG. Elasticsearch 8.x supports dense_vector fields and kNN search natively, so you can add vector search to existing indices alongside your traditional keyword queries. The RAG pipeline embeds the question, runs a kNN search against the index, and generates from the results.

Each step can fail independently: the embedding API might time out, the Elasticsearch cluster might be rebalancing, or the LLM might be rate-limited.

## The Solution

**You write the embedding and Elasticsearch kNN query logic. Conductor handles the search pipeline, retries, and observability.**

Each stage is an independent worker .  question embedding, Elasticsearch kNN search, and answer generation. Conductor sequences them, retries the Elasticsearch query during cluster rebalancing, and tracks every search with the question, kNN results, and generated answer.

### What You Write: Workers

Three workers integrate Elasticsearch into the RAG pipeline .  embedding the query, performing kNN dense vector search against an Elasticsearch index, and generating an answer from the matched documents.

| Worker | Task | What It Does |
|---|---|---|
| **EsEmbedWorker** | `es_embed` | Worker that encodes a question into a fixed embedding vector for Elasticsearch knn search. |
| **EsGenerateWorker** | `es_generate` | Worker that generates an answer from a question and Elasticsearch search hits. |
| **EsKnnSearchWorker** | `es_knn_search` | Worker that simulates an Elasticsearch knn vector search. In production, this would issue a POST to /{index}/_search ... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
es_embed
    │
    ▼
es_knn_search
    │
    ▼
es_generate
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
java -jar target/rag-elasticsearch-1.0.0.jar
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
java -jar target/rag-elasticsearch-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_elasticsearch_workflow \
  --version 1 \
  --input '{"question": "test-value", "index": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_elasticsearch_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one RAG stage .  swap in a real embedding API for vectorization, execute kNN queries against your Elasticsearch `dense_vector` index, and the embed-search-generate pipeline runs unchanged.

- **EsEmbedWorker** (`es_embed`): call an embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) to vectorize the question for Elasticsearch knn search
- **EsGenerateWorker** (`es_generate`): send Elasticsearch search hits as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer with source attribution
- **EsKnnSearchWorker** (`es_knn_search`): execute a real Elasticsearch knn query against a `dense_vector` field using the Elasticsearch Java client, with configurable k, num_candidates, and metadata filters

The embed/search/generate contract stays fixed .  switch embedding models, tune kNN parameters, or change the LLM provider without altering the workflow definition.

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
rag-elasticsearch/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragelasticsearch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagElasticsearchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EsEmbedWorker.java
│       ├── EsGenerateWorker.java
│       └── EsKnnSearchWorker.java
└── src/test/java/ragelasticsearch/workers/
    ├── EsEmbedWorkerTest.java        # 4 tests
    ├── EsGenerateWorkerTest.java        # 5 tests
    └── EsKnnSearchWorkerTest.java        # 3 tests
```
