# Document QA in Java with Conductor :  Ingest, Chunk, Index, and Answer Questions About Documents

A Java Conductor workflow that answers questions about documents .  ingesting a document from a URL, chunking it into searchable segments, indexing the chunks for retrieval, querying the index with a natural language question, and generating an answer from the most relevant chunks. Given a `documentUrl` and `question`, the pipeline produces a chunk count, an answer, and a confidence score. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-step document QA pipeline.

## Asking Questions Instead of Reading Entire Documents

Users should not have to read a 50-page document to find one answer. Document QA systems let users ask natural language questions and get answers with confidence scores. But building this requires a pipeline: fetch the document, split it into chunks small enough for semantic search, index those chunks, find the most relevant ones for the question, and synthesize an answer from the retrieved context.

This workflow handles the full pipeline. The ingester fetches the document from the provided URL. The chunker splits the document into manageable segments. The indexer stores the chunks in a searchable index. The query worker searches the index using the user's question and retrieves the most relevant chunks. The answer worker synthesizes a natural language answer from those chunks along with a confidence score.

## The Solution

**You just write the document-ingestion, chunking, indexing, querying, and answer-generation workers. Conductor handles the five-step RAG pipeline.**

Five workers form the QA pipeline .  document ingestion, chunking, indexing, querying, and answering. The ingester downloads and parses the document. The chunker splits it into segments suitable for semantic search. The indexer creates a searchable index. The query worker finds the chunks most relevant to the question. The answer worker generates a response from those chunks with a confidence score. Conductor sequences all five steps and passes the document content, chunks, index ID, and relevant chunks between them.

### What You Write: Workers

IngestWorker fetches the document, ChunkWorker splits it for search, IndexWorker creates the vector index, QueryWorker retrieves relevant chunks, and AnswerWorker synthesizes a response with a confidence score.

| Worker | Task | What It Does |
|---|---|---|
| **AnswerWorker** | `dqa_answer` | Synthesizes a natural-language answer from the retrieved chunks, with a confidence score. |
| **ChunkWorker** | `dqa_chunk` | Splits the ingested document into semantically meaningful chunks for search indexing. |
| **IndexWorker** | `dqa_index` | Creates a vector index from the document chunks using an embedding model. |
| **IngestWorker** | `dqa_ingest` | Fetches and parses the document from the provided URL (PDF, HTML, etc.). |
| **QueryWorker** | `dqa_query` | Searches the index with the user's question and retrieves the top relevant chunks. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
dqa_ingest
    │
    ▼
dqa_chunk
    │
    ▼
dqa_index
    │
    ▼
dqa_query
    │
    ▼
dqa_answer
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
java -jar target/document-qa-1.0.0.jar
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

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/document-qa-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dqa_document_qa \
  --version 1 \
  --input '{"documentUrl": "https://example.com", "question": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dqa_document_qa -s COMPLETED -c 5
```

## How to Extend

Each worker handles one RAG stage .  connect your vector store (Pinecone, Weaviate, pgvector) for indexing and your LLM (Claude, GPT-4) for answer generation, and the document QA workflow stays the same.

- **AnswerWorker** (`dqa_answer`): swap in an LLM (GPT-4, Claude) for real answer generation from retrieved context
- **ChunkWorker** (`dqa_chunk`): use LangChain or LlamaIndex chunking strategies (recursive, semantic) for better retrieval quality
- **IndexWorker** (`dqa_index`): connect to a vector database (Pinecone, Weaviate, pgvector) for example-grade semantic search

Swap in a real vector database and LLM and the five-step RAG pipeline continues to produce answers without modification.

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
document-qa/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/documentqa/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DocumentQaExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnswerWorker.java
│       ├── ChunkWorker.java
│       ├── IndexWorker.java
│       ├── IngestWorker.java
│       └── QueryWorker.java
└── src/test/java/documentqa/workers/
    ├── AnswerWorkerTest.java        # 2 tests
    ├── ChunkWorkerTest.java        # 2 tests
    ├── IndexWorkerTest.java        # 2 tests
    ├── IngestWorkerTest.java        # 2 tests
    └── QueryWorkerTest.java        # 2 tests
```
