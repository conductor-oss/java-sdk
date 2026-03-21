# Document Ingestion Pipeline in Java Using Conductor: PDF to Vector Store in Four Steps

Someone dumps 10,000 PDFs into a shared drive and expects the RAG system to answer questions about them by Monday. Your chatbot can't find anything because nobody extracted the text, chunked it for embedding models, generated vectors, or loaded them into the vector store. If the embedding API rate-limits you halfway through, a naive script loses track of what was already processed and starts over from page one. This example builds a four-stage document ingestion pipeline using [Conductor](https://github.com/conductor-oss/conductor): extract, chunk, embed, store, that turns raw PDFs into searchable vectors with per-stage retries and full visibility into which document failed at which step.

## Turning Documents into Searchable Knowledge

Before a RAG system can answer questions, documents must be ingested: raw PDFs need to be parsed into plain text, that text needs to be split into chunks small enough for embedding models (with overlap to preserve context at boundaries), each chunk needs to be embedded into a vector, and those vectors need to be upserted into a collection in your vector store.

Each step depends on the previous one's output. You can't chunk text you haven't extracted, and you can't embed chunks you haven't created. If the embedding API rate-limits you mid-batch or the vector store connection drops during upsert, you need to retry that specific step without re-extracting a 200-page PDF. And when you're ingesting thousands of documents, you need to see exactly which document failed at which stage.

Without orchestration, this becomes a fragile script where a single embedding API timeout means restarting from scratch, with no record of what was already processed.

## The Solution

**You write the extraction, chunking, embedding, and storage logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each stage of the ingestion pipeline is an independent worker. PDF extraction, text chunking, embedding generation, vector storage. Conductor sequences them, passes the output of each stage to the next, retries on transient failures (embedding API timeouts, vector store connection drops), and tracks every document's journey from PDF to stored vectors. You get a example-grade ingestion pipeline without writing retry loops or progress tracking.

### What You Write: Workers

Four workers form the ingestion pipeline. PDF text extraction, word-based chunking with configurable overlap, embedding generation, and vector database upsert, each stage feeding its output to the next.

| Worker | Task | What It Does |
|---|---|---|
| **IngestChunkTextWorker** | `ingest_chunk_text` | Worker 2: Splits extracted text into overlapping chunks. Uses word-based chunking with configurable size and overlap. |
| **IngestEmbedChunksWorker** | `ingest_embed_chunks` | Worker 3: Generates embeddings for each text chunk. Uses fixed (deterministic) embeddings for reproducible tests. |
| **IngestExtractPdfWorker** | `ingest_extract_pdf` | Worker 1: Extracts text from a PDF document. Simulates PDF parsing by returning fixed text about vector databases. |
| **IngestStoreVectorsWorker** | `ingest_store_vectors` | Worker 4: Stores embedding vectors in a vector database collection. Simulates upserting vectors and returns the count |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode, the workflow and worker interfaces stay the same.

### The Workflow

```
ingest_extract_pdf
    │
    ▼
ingest_chunk_text
    │
    ▼
ingest_embed_chunks
    │
    ▼
ingest_store_vectors

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
java -jar target/document-ingestion-1.0.0.jar

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
java -jar target/document-ingestion-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow document_ingestion_workflow \
  --version 1 \
  --input '{"documentUrl": "https://example.com/vector-databases-guide.pdf", "collection": "knowledge_base", "chunkSize": "30", "chunkOverlap": "5"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w document_ingestion_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one ingestion stage. Swap in Apache PDFBox for extraction, OpenAI Embeddings for vectorization, upsert to Pinecone or Weaviate for storage, and the four-step pipeline runs unchanged.

- **IngestExtractPdfWorker** (`ingest_extract_pdf`): swap in Apache PDFBox, Tika, or a cloud document AI service for real PDF text extraction
- **IngestChunkTextWorker** (`ingest_chunk_text`): integrate LangChain4j's text splitters or implement custom sentence-boundary chunking
- **IngestEmbedChunksWorker** (`ingest_embed_chunks`): replace fixed embeddings with calls to OpenAI Embeddings, Cohere, or a local sentence-transformers model
- **IngestStoreVectorsWorker** (`ingest_store_vectors`): swap in real upsert calls to Pinecone, Weaviate, pgvector, Qdrant, or Milvus

Each worker preserves the same chunk/embedding contract, so replacing PDFBox with Tika or swapping Pinecone for Weaviate requires no workflow changes.

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
document-ingestion/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/documentingestion/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DocumentIngestionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── IngestChunkTextWorker.java
│       ├── IngestEmbedChunksWorker.java
│       ├── IngestExtractPdfWorker.java
│       └── IngestStoreVectorsWorker.java
└── src/test/java/documentingestion/workers/
    ├── IngestChunkTextWorkerTest.java        # 6 tests
    ├── IngestEmbedChunksWorkerTest.java        # 7 tests
    ├── IngestExtractPdfWorkerTest.java        # 5 tests
    └── IngestStoreVectorsWorkerTest.java        # 5 tests

```
