# RAG with LangChain in Java Using Conductor :  Load, Split, Embed, Retrieve, Generate

A Java Conductor workflow that implements the full LangChain-style RAG pipeline. loading documents, splitting text into chunks, embedding chunks, retrieving relevant chunks for a question, and generating an answer. Each LangChain stage (document loader, text splitter, embedder, retriever, generator) maps to an independent Conductor worker. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage pipeline as independent workers,  you write the LangChain integration, Conductor handles sequencing, retries, durability, and observability.

## The Full LangChain Pipeline as a Workflow

LangChain defines the canonical RAG stages: load documents from a source, split them into chunks, embed the chunks, retrieve relevant ones for a query, and generate an answer. In a LangChain application, these stages are chained in code. As a Conductor workflow, each stage becomes an independently retryable, observable, and replaceable worker.

If the embedding API rate-limits you during chunk embedding, Conductor retries that stage without re-loading and re-splitting the documents. If you want to swap the text splitter (from character-based to sentence-based), you replace one worker without touching the rest of the pipeline.

## The Solution

**You write each LangChain stage as a worker. Conductor handles the five-step pipeline, retries, and observability.**

Each LangChain stage is an independent worker. document loading, text splitting, chunk embedding, retrieval, and generation. Conductor sequences them, retries any stage that fails, and tracks every execution from raw document through final answer.

### What You Write: Workers

Five workers mirror the LangChain document processing pattern. loading documents from a source, splitting text into chunks, embedding the chunks, retrieving relevant passages, and generating an answer from the retrieved context.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedChunksWorker** | `lc_embed_chunks` | Worker that generates embeddings for text chunks. Produces deterministic 4-dimensional vectors based on chunk content... |
| **GenerateWorker** | `lc_generate` | Worker that generates an answer using the RetrievalQA chain. Combines the question with retrieved documents to produc... |
| **LoadDocumentsWorker** | `lc_load_documents` | Worker that loads documents from a source URL using WebBaseLoader. Returns a list of documents with pageContent and m... |
| **RetrieveWorker** | `lc_retrieve` | Worker that retrieves the most relevant documents for a question using FAISS. Assigns decreasing similarity scores (0... |
| **SplitTextWorker** | `lc_split_text` | Worker that splits documents into chunks using sentence-based splitting. Simulates RecursiveCharacterTextSplitter fro... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
lc_load_documents
    │
    ▼
lc_split_text
    │
    ▼
lc_embed_chunks
    │
    ▼
lc_retrieve
    │
    ▼
lc_generate

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
java -jar target/rag-langchain-1.0.0.jar

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
java -jar target/rag-langchain-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_langchain \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_langchain -s COMPLETED -c 5

```

## How to Extend

Each worker maps to one LangChain stage. swap in LangChain4j document loaders, text splitters, and embedding models, connect a real retriever, and the load-split-embed-retrieve-generate pipeline runs unchanged.

- **LoadDocumentsWorker** (`lc_load_documents`): use LangChain document loaders (WebBaseLoader, PyPDFLoader, UnstructuredFileLoader) to ingest documents from URLs, PDFs, or file systems
- **SplitTextWorker** (`lc_split_text`): use LangChain's RecursiveCharacterTextSplitter or SentenceTransformers-based semantic chunking for context-aware document splitting
- **EmbedChunksWorker** (`lc_embed_chunks`): call OpenAI Embeddings (text-embedding-3-small) or Hugging Face models via LangChain's embedding interface to vectorize document chunks
- **RetrieveWorker** (`lc_retrieve`): query a FAISS, Chroma, or Pinecone vector store through LangChain's retriever interface with configurable similarity search parameters
- **GenerateWorker** (`lc_generate`): use LangChain's RetrievalQA chain with GPT-4, Claude, or a local model to generate answers grounded in retrieved documents

Each worker preserves the LangChain-compatible document/chunk interface, so swapping document loaders, text splitters, or embedding providers requires no workflow changes.

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
rag-langchain/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/raglangchain/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagLangchainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmbedChunksWorker.java
│       ├── GenerateWorker.java
│       ├── LoadDocumentsWorker.java
│       ├── RetrieveWorker.java
│       └── SplitTextWorker.java
└── src/test/java/raglangchain/workers/
    ├── EmbedChunksWorkerTest.java        # 5 tests
    ├── GenerateWorkerTest.java        # 5 tests
    ├── LoadDocumentsWorkerTest.java        # 5 tests
    ├── RetrieveWorkerTest.java        # 5 tests
    └── SplitTextWorkerTest.java        # 5 tests

```
