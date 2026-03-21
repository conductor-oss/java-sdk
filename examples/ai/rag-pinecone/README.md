# RAG with Pinecone in Java Using Conductor :  Embed, Query Vectors with Namespace and Metadata Filtering, Generate

A Java Conductor workflow that implements RAG using Pinecone .  embedding the question, querying a Pinecone index with namespace isolation, topK control, and metadata filtering, and generating an answer from the matched vectors. Pinecone is a fully managed vector database with built-in namespacing for multi-tenancy and metadata filtering for scoped queries. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, Pinecone querying, and generation as independent workers ,  you write the Pinecone integration, Conductor handles sequencing, retries, durability, and observability.

## RAG with Pinecone's Managed Vector Infrastructure

Pinecone provides serverless or pod-based vector search with zero infrastructure management. Its RAG pipeline embeds the question, queries the index (with optional namespace for tenant isolation and metadata filters for access control), and generates from the top-K results. The workflow accepts namespace, topK, and filter parameters for flexible, multi-tenant RAG.

Each step can fail independently: the embedding API might time out, Pinecone might be scaling, or the LLM might be rate-limited.

## The Solution

**You write the embedding and Pinecone query logic with namespace and metadata filters. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker .  question embedding, Pinecone vector query (with namespace and filter support), and answer generation. Conductor sequences them, retries the Pinecone query during scaling events, and tracks every search with the question, namespace, filter, retrieved vectors, and generated answer.

### What You Write: Workers

Three workers integrate Pinecone into the RAG pipeline .  embedding the query, querying a Pinecone index with namespace isolation and metadata filtering, and generating an answer from the top-k results.

| Worker | Task | What It Does |
|---|---|---|
| **PineEmbedWorker** | `pine_embed` | Worker that generates a fixed embedding vector for a question. In production this would call an embedding API (e.g. O... |
| **PineGenerateWorker** | `pine_generate` | Worker that generates an answer from a question and retrieved Pinecone matches. Builds a deterministic answer by comb... |
| **PineQueryWorker** | `pine_query` | Worker that queries a Pinecone index with an embedding vector. Returns fixed matches for deterministic behavior. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
pine_embed
    │
    ▼
pine_query
    │
    ▼
pine_generate

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
java -jar target/rag-pinecone-1.0.0.jar

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
java -jar target/rag-pinecone-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_pinecone_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "namespace": "test", "topK": "sample-topK", "filter": "sample-filter"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_pinecone_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one RAG stage .  swap in a real embedding model, query Pinecone with namespace isolation and metadata filters for multi-tenant retrieval, and the embed-query-generate pipeline runs unchanged.

- **PineEmbedWorker** (`pine_embed`): call an embedding API (OpenAI text-embedding-ada-002, Cohere embed-english-v3) to vectorize the question for Pinecone search
- **PineGenerateWorker** (`pine_generate`): send Pinecone matches as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer with source attribution
- **PineQueryWorker** (`pine_query`): query a real Pinecone index using the Pinecone Java/Python SDK with namespace selection, metadata filters, and configurable top_k

The embed/query/generate contract is fixed .  adjust namespaces, add metadata filters, or swap embedding models without changing the workflow definition.

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
rag-pinecone/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragpinecone/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagPineconeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PineEmbedWorker.java
│       ├── PineGenerateWorker.java
│       └── PineQueryWorker.java
└── src/test/java/ragpinecone/workers/
    ├── PineEmbedWorkerTest.java        # 6 tests
    ├── PineGenerateWorkerTest.java        # 6 tests
    └── PineQueryWorkerTest.java        # 7 tests

```
