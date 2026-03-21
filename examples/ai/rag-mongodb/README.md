# RAG with MongoDB Atlas Vector Search in Java Using Conductor :  Embed, Search, Generate

A Java Conductor workflow that implements RAG using MongoDB Atlas Vector Search. embedding the question, running a vector search query against a MongoDB collection with a vector search index, and generating an answer from the retrieved documents. If you already store your data in MongoDB, Atlas Vector Search lets you add RAG without a separate vector database. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, MongoDB vector search, and generation as independent workers,  you write the MongoDB integration, Conductor handles sequencing, retries, durability, and observability.

## RAG on Your Existing MongoDB Data

If your documents already live in MongoDB, Atlas Vector Search lets you run vector similarity queries against the same collection using `$vectorSearch` aggregation stages. no data migration to a separate vector store needed. The RAG pipeline embeds the question, queries MongoDB with the vector, and generates from the results, all against your existing data and indexes.

Each step can fail independently: the embedding API might time out, the MongoDB cluster might be performing an election, or the LLM might be rate-limited.

## The Solution

**You write the embedding and MongoDB Atlas Vector Search query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, MongoDB Atlas vector search (specifying database and collection), and answer generation. Conductor sequences them, retries the MongoDB query during replica set elections, and tracks every search with the question, database, collection, retrieved documents, and generated answer.

### What You Write: Workers

Three workers integrate MongoDB Atlas into the RAG pipeline. embedding the query, performing $vectorSearch against a MongoDB Atlas collection, and generating an answer from the matched documents.

| Worker | Task | What It Does |
|---|---|---|
| **MongoEmbedWorker** | `mongo_embed` | Worker that converts a question into a fixed embedding vector. In production this would call an embedding model (e.g.... |
| **MongoGenerateWorker** | `mongo_generate` | Worker that generates an answer from the question and retrieved documents. In production this would call an LLM (e.g.... |
| **MongoVectorSearchWorker** | `mongo_vector_search` | Worker that simulates MongoDB Atlas $vectorSearch aggregation pipeline stage. In production this would run: db.collec... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
mongo_embed
    │
    ▼
mongo_vector_search
    │
    ▼
mongo_generate

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
java -jar target/rag-mongodb-1.0.0.jar

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
java -jar target/rag-mongodb-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_mongodb_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "database": {"key": "value"}, "collection": "sample-collection"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_mongodb_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one RAG stage. swap in a real embedding API, run `$vectorSearch` aggregation queries against your MongoDB Atlas collection, and the embed-search-generate pipeline runs unchanged.

- **MongoEmbedWorker** (`mongo_embed`): call an embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) to vectorize the question for MongoDB Atlas Vector Search
- **MongoGenerateWorker** (`mongo_generate`): send MongoDB search results as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer
- **MongoVectorSearchWorker** (`mongo_vector_search`): run a real MongoDB Atlas `$vectorSearch` aggregation pipeline with configurable numCandidates, limit, and metadata filters on your collection

The embed/search/generate contract is fixed. adjust the Atlas Search index, change the similarity metric, or swap the LLM provider without altering the workflow.

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
rag-mongodb/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragmongodb/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagMongodbExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MongoEmbedWorker.java
│       ├── MongoGenerateWorker.java
│       └── MongoVectorSearchWorker.java
└── src/test/java/ragmongodb/workers/
    ├── MongoEmbedWorkerTest.java        # 4 tests
    ├── MongoGenerateWorkerTest.java        # 5 tests
    └── MongoVectorSearchWorkerTest.java        # 5 tests

```
