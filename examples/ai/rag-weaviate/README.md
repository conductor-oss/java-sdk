# RAG with Weaviate in Java Using Conductor :  GraphQL-Powered Vector Search and Generation

A Java Conductor workflow that implements RAG using Weaviate .  embedding the query, searching a Weaviate class via its GraphQL API with vector similarity (nearVector/nearText), and generating an answer from the retrieved objects. Weaviate offers built-in vectorization modules, multi-tenancy, and a GraphQL query interface. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, Weaviate search, and generation as independent workers ,  you write the Weaviate integration, Conductor handles sequencing, retries, durability, and observability for free.

## RAG with Weaviate's Rich Query Interface

Weaviate provides vector search through a GraphQL API that supports nearVector, nearText (with built-in vectorizers), BM25, and hybrid queries .  all in a single API. It also supports multi-tenancy, batch operations, and cross-references between objects. The RAG pipeline embeds the query, searches a Weaviate class for relevant objects, and generates from the results.

## The Solution

**You write the embedding and Weaviate GraphQL query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker .  query embedding, Weaviate GraphQL search, and answer generation. Conductor sequences them, retries the Weaviate query if the server is temporarily unavailable, and tracks every search with the query, retrieved objects, and generated answer.

### What You Write: Workers

Three workers integrate Weaviate into the RAG pipeline .  embedding the query, searching via Weaviate's GraphQL nearVector or nearText operators, and generating an answer from the retrieved objects.

| Worker | Task | What It Does |
|---|---|---|
| **WeavEmbedWorker** | `weav_embed` | Converts a text question into a fixed embedding vector (simulated). |
| **WeavGenerateWorker** | `weav_generate` | Generates an answer from the user question and retrieved Weaviate objects (simulated). |
| **WeavSearchWorker** | `weav_search` | Performs a vector-similarity search against Weaviate (simulated). |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
weav_embed
    │
    ▼
weav_search
    │
    ▼
weav_generate
```

## Example Output

```
=== RAG with Weaviate Vector Database Workflow ===

Step 1: Registering task definitions...
  Registered: weav_embed, weav_search, weav_generate

Step 2: Registering workflow 'rag_weaviate_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [embed] Generated embedding via OpenAI API (LIVE):
  [generate] Response from OpenAI API (LIVE)
  [weav_search worker] Searching class:

  Status: COMPLETED
  Output: {embedding=..., answer=..., title=..., content=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/rag-weaviate-1.0.0.jar
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
java -jar target/rag-weaviate-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_weaviate_workflow \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_weaviate_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one RAG stage .  swap in a real embedding model, query a Weaviate class via its GraphQL nearVector or hybrid search API, and the embed-search-generate pipeline runs unchanged.

- **WeavEmbedWorker** (`weav_embed`): call an embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) or use Weaviate's built-in vectorizer modules (text2vec-openai, text2vec-cohere)
- **WeavGenerateWorker** (`weav_generate`): send Weaviate search results as context to an LLM (OpenAI GPT-4, Anthropic Claude) or use Weaviate's generative module (generative-openai) for in-database RAG
- **WeavSearchWorker** (`weav_search`): query a real Weaviate class using the GraphQL API or Java client with `nearVector`, `nearText`, or hybrid search and metadata filters

The embed/search/generate contract stays fixed .  switch between nearVector and nearText, adjust Weaviate class schemas, or swap LLM providers without modifying the workflow.

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
rag-weaviate/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragweaviate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagWeaviateExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WeavEmbedWorker.java
│       ├── WeavGenerateWorker.java
│       └── WeavSearchWorker.java
└── src/test/java/ragweaviate/workers/
    ├── WeavEmbedWorkerTest.java        # 4 tests
    ├── WeavGenerateWorkerTest.java        # 5 tests
    └── WeavSearchWorkerTest.java        # 7 tests
```
