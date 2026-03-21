# RAG with Redis Vector Search in Java Using Conductor :  FT.SEARCH for Similarity Queries

A Java Conductor workflow that implements RAG using Redis's vector similarity search (RediSearch) .  embedding the question, running an `FT.SEARCH` query with KNN vector matching against a Redis index, and generating an answer from the matched documents. Redis provides sub-millisecond vector search alongside your existing caching and data layer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, Redis FT.SEARCH, and generation as independent workers ,  you write the Redis integration, Conductor handles sequencing, retries, durability, and observability.

## Ultra-Low Latency RAG with Redis

If you already run Redis for caching or session management, RediSearch adds vector similarity search without a separate database. Redis vectors are stored in-memory, making search latency sub-millisecond .  ideal for real-time applications where RAG latency matters (chatbots, autocomplete, interactive assistants). The pipeline embeds the question, searches the Redis index, and generates from the results.

## The Solution

**You write the embedding and Redis FT.SEARCH query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker .  question embedding, Redis FT.SEARCH vector query (specifying the index name), and answer generation. Conductor sequences them, retries the Redis query if the connection is temporarily lost, and tracks every search.

### What You Write: Workers

Three workers integrate Redis into the RAG pipeline .  embedding the query, performing sub-millisecond vector search via Redis FT.SEARCH, and generating an answer from the matched documents.

| Worker | Task | What It Does |
|---|---|---|
| **RedisEmbedWorker** | `redis_embed` | Worker that converts a question into a fixed embedding vector. Uses deterministic values instead of a real embedding ... |
| **RedisFtSearchWorker** | `redis_ft_search` | Worker that simulates a Redis FT.SEARCH vector similarity query. Real Redis commands this simulates: Create index: FT... |
| **RedisGenerateWorker** | `redis_generate` | Worker that generates an answer from the question and Redis search results. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
redis_embed
    │
    ▼
redis_ft_search
    │
    ▼
redis_generate

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
java -jar target/rag-redis-1.0.0.jar

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
java -jar target/rag-redis-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_redis_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "indexName": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_redis_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one RAG stage .  swap in a real embedding API, run FT.SEARCH KNN queries against your Redis index for sub-millisecond retrieval, and the embed-search-generate pipeline runs unchanged.

- **RedisEmbedWorker** (`redis_embed`): call an embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) to vectorize the question for Redis vector search
- **RedisFtSearchWorker** (`redis_ft_search`): execute a real Redis `FT.SEARCH` vector similarity query against an HNSW or FLAT index using Jedis or Lettuce with `DIALECT 2`
- **RedisGenerateWorker** (`redis_generate`): send Redis search results as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer

The embed/search/generate contract is fixed .  tune the Redis index schema, switch distance metrics, or swap the LLM provider without changing the workflow.

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
rag-redis/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragredis/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagRedisExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── RedisEmbedWorker.java
│       ├── RedisFtSearchWorker.java
│       └── RedisGenerateWorker.java
└── src/test/java/ragredis/workers/
    ├── RedisEmbedWorkerTest.java        # 4 tests
    ├── RedisFtSearchWorkerTest.java        # 5 tests
    └── RedisGenerateWorkerTest.java        # 4 tests

```
