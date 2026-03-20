# Incremental RAG in Java Using Conductor :  Sync Only Changed Documents to Your Vector Store

A Java Conductor workflow that keeps a vector store in sync with a source document collection by detecting changes since the last sync, filtering new vs: updated documents, embedding only the changed ones, upserting vectors, and verifying the index is consistent. Instead of re-embedding your entire corpus on every update, this pipeline processes only what changed. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the incremental sync pipeline as independent workers .  you write the change detection, embedding, and upsert logic, Conductor handles sequencing, retries, durability, and observability.

## The Cost of Full Re-Indexing

When documents in your knowledge base change .  new articles added, existing ones updated, obsolete ones removed ,  the vector store needs to reflect those changes. A naive approach re-embeds every document on every sync cycle. For a corpus of 100,000 documents where 50 changed, that's 99,950 wasted embedding API calls and hours of unnecessary processing.

Incremental indexing solves this by comparing document hashes to detect what actually changed, separating new documents (no existing hash) from updated ones (hash mismatch), embedding only those, and upserting the new vectors. After the upsert, a verification step confirms the index is consistent and reports query latency. If the embedding API fails mid-batch, you need to retry from the embedding step without re-running change detection .  the changed document list is still valid.

Without orchestration, this becomes a fragile script where an embedding timeout means re-scanning the entire source collection, there's no record of how many documents were synced per run, and a failed upsert leaves the index in an inconsistent state with no way to resume.

## The Solution

**You write the change detection, embedding, and upsert logic. Conductor handles the incremental sync sequencing, retries, and observability.**

Each stage of the incremental sync is an independent worker .  change detection, filtering, embedding, upserting, verification. Conductor sequences them and passes change sets between stages. If the embedding API times out, Conductor retries it without re-running change detection. Every sync run is tracked end-to-end, showing exactly how many documents were detected, filtered, embedded, upserted, and verified.

### What You Write: Workers

Five workers handle incremental index maintenance .  detecting document changes, filtering to only new or modified content, embedding the new chunks, upserting vectors, and verifying index consistency.

| Worker | Task | What It Does |
|---|---|---|
| **DetectChangesWorker** | `ir_detect_changes` | Worker that detects changed documents in the source collection since the last sync timestamp. Returns changed documen... |
| **EmbedIncrementalWorker** | `ir_embed_incremental` | Worker that generates embeddings for documents that need to be inserted or updated. Uses deterministic vectors rather... |
| **FilterNewDocsWorker** | `ir_filter_new_docs` | Worker that separates changed documents into new (no existing hash) and updated (has existing hash) categories, produ... |
| **UpsertVectorsWorker** | `ir_upsert_vectors` | Worker that upserts embedding vectors into the vector store, counting inserts vs updates based on the action field. |
| **VerifyIndexWorker** | `ir_verify_index` | Worker that verifies the vector index after upserting. Confirms all documents are indexed and reports query latency. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
ir_detect_changes
    │
    ▼
ir_filter_new_docs
    │
    ▼
ir_embed_incremental
    │
    ▼
ir_upsert_vectors
    │
    ▼
ir_verify_index
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
java -jar target/incremental-rag-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, the embed worker calls OpenAI Embeddings API (text-embedding-3-small). When unset, all workers use simulated output. |

### Live vs Simulated Mode

- **Without `CONDUCTOR_OPENAI_API_KEY`**: All workers return deterministic simulated output (default behavior, no API calls).
- **With `CONDUCTOR_OPENAI_API_KEY`**: EmbedIncrementalWorker calls OpenAI Embeddings API (text-embedding-3-small). Vector store workers (UpsertVectorsWorker, VerifyIndexWorker, DetectChangesWorker) remain simulated because they require a real vector database.
- If an OpenAI call fails, the worker automatically falls back to simulated output.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/incremental-rag-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow incremental_rag \
  --version 1 \
  --input '{"input": "test"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w incremental_rag -s COMPLETED -c 5
```

## How to Extend

Each worker handles one sync stage .  swap in MongoDB change streams or S3 events for change detection, OpenAI Embeddings for vectorization, and Pinecone or pgvector for upserts, and the incremental indexing pipeline runs unchanged.

- **DetectChangesWorker** (`ir_detect_changes`): swap in a real query against your document store (MongoDB change streams, PostgreSQL CDC, S3 event notifications) using the last sync timestamp
- **FilterNewDocsWorker** (`ir_filter_new_docs`): integrate with a hash store (Redis, DynamoDB) to compare document content hashes and classify changes as inserts vs, updates
- **EmbedIncrementalWorker** (`ir_embed_incremental`): replace fixed embeddings with real calls to OpenAI Embeddings, Cohere, or a local model
- **UpsertVectorsWorker** (`ir_upsert_vectors`): swap in real upsert calls to Pinecone, Weaviate, pgvector, or Qdrant
- **VerifyIndexWorker** (`ir_verify_index`): add real index health checks (vector count verification, sample query latency measurement)

The change-detection contract stays fixed across workers .  swap in a real change feed, upgrade the embedding model, or switch vector stores without modifying the incremental pipeline.

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
incremental-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/incrementalrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IncrementalRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectChangesWorker.java
│       ├── EmbedIncrementalWorker.java
│       ├── FilterNewDocsWorker.java
│       ├── UpsertVectorsWorker.java
│       └── VerifyIndexWorker.java
└── src/test/java/incrementalrag/workers/
    ├── DetectChangesWorkerTest.java        # 4 tests
    ├── EmbedIncrementalWorkerTest.java        # 4 tests
    ├── FilterNewDocsWorkerTest.java        # 3 tests
    ├── UpsertVectorsWorkerTest.java        # 3 tests
    └── VerifyIndexWorkerTest.java        # 3 tests
```
