# RAG with ChromaDB in Java Using Conductor :  Embed, Query, Generate

A Java Conductor workflow that implements a RAG pipeline using ChromaDB as the vector store. embedding the user question, querying a ChromaDB collection for relevant documents, and generating an answer grounded in the retrieved context. ChromaDB provides a lightweight, open-source vector database that runs locally or in Docker. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, ChromaDB querying, and LLM generation as independent workers,  you write the embedding and ChromaDB integration, Conductor handles sequencing, retries, durability, and observability.

## RAG with ChromaDB

ChromaDB is a popular choice for RAG prototyping and production. it's open-source, runs locally without cloud dependencies, supports metadata filtering, and has a simple Python/REST API. A RAG pipeline against ChromaDB follows three steps: embed the question into a vector, query the ChromaDB collection for nearest neighbors, and pass the retrieved documents as context to an LLM.

Each step can fail independently: the embedding model might time out, the ChromaDB connection might drop, or the LLM might be rate-limited. Without orchestration, a ChromaDB connection error means restarting from embedding, and there's no record of which documents were retrieved for which questions.

## The Solution

**You write the embedding and ChromaDB query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, ChromaDB collection query, and answer generation. Conductor sequences them, retries the ChromaDB query if the connection drops, and tracks every execution with the question, retrieved documents, and generated answer.

### What You Write: Workers

Three workers integrate ChromaDB into the RAG pipeline. embedding the query, querying the ChromaDB collection with metadata filtering, and generating an answer from the retrieved documents.

| Worker | Task | What It Does |
|---|---|---|
| **ChromaEmbedWorker** | `chroma_embed` | Worker that produces a fixed embedding vector for a given question. In production this would call ChromaDB's default ... |
| **ChromaGenerateWorker** | `chroma_generate` | Worker that generates an answer from ChromaDB query results. In production this would send the retrieved documents as... |
| **ChromaQueryWorker** | `chroma_query` | Worker that simulates querying a ChromaDB collection. In production this would use: ChromaClient client = new ChromaC... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
chroma_embed
    │
    ▼
chroma_query
    │
    ▼
chroma_generate

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
java -jar target/rag-chromadb-1.0.0.jar

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
java -jar target/rag-chromadb-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_chromadb_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "collection": "sample-collection"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_chromadb_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one RAG stage. swap in ChromaDB's built-in embedding function or an external embedding API, query a real ChromaDB collection with metadata filters, and the embed-query-generate pipeline runs unchanged.

- **ChromaEmbedWorker** (`chroma_embed`): call ChromaDB's built-in embedding function or an external embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) to vectorize the question
- **ChromaGenerateWorker** (`chroma_generate`): send the retrieved ChromaDB documents as context to an LLM (OpenAI GPT-4, Anthropic Claude) to produce a grounded answer
- **ChromaQueryWorker** (`chroma_query`): query a real ChromaDB collection using the Python client via HTTP (`ChromaClient.get_collection().query()`) with metadata filters and configurable n_results

The embed/query/generate contract is fixed. upgrade embedding models, adjust ChromaDB collection settings, or swap the LLM provider without modifying the workflow.

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
rag-chromadb/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragchromadb/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagChromadbExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChromaEmbedWorker.java
│       ├── ChromaGenerateWorker.java
│       └── ChromaQueryWorker.java
└── src/test/java/ragchromadb/workers/
    ├── ChromaEmbedWorkerTest.java        # 6 tests
    ├── ChromaGenerateWorkerTest.java        # 5 tests
    └── ChromaQueryWorkerTest.java        # 5 tests

```
