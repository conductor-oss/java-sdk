# Basic RAG in Java Using Conductor: Embed Query, Search Vectors, Generate Answer

A user asks your chatbot "What's our refund policy?" and it confidently invents a policy that doesn't exist. because the LLM has zero access to your actual documents. Without retrieval, every answer is a plausible-sounding fabrication. This example builds a three-stage RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor), embed the question, search a vector store for real document chunks, and generate an answer grounded in what was actually retrieved, so the LLM can only cite facts you control.

## LLMs Hallucinate Without Grounding in Your Data

You ask an LLM about your company's refund policy and it invents a plausible-sounding but completely wrong answer. RAG fixes this by retrieving the actual policy document and providing it as context to the LLM, so the answer is grounded in your real data. But RAG has three distinct stages. Embedding the question, searching the vector store, and generating with context, and each can fail independently: the embedding API might timeout, the vector search might return irrelevant results, or the LLM generation might fail due to rate limiting.

Building RAG as a single function means a retry in the embedding step re-runs the entire pipeline, a vector search failure crashes the generation, and there's no visibility into which stage produced poor results. Was the answer bad because the retrieval missed the right document, or because the LLM ignored the context?

## The Solution

**You write the embedding, retrieval, and generation logic. Conductor handles sequencing, durability, and observability.**

`EmbedQueryWorker` converts the user's question into a vector embedding using an embedding model. `SearchVectorsWorker` queries the vector database with the embedding to retrieve the top-k most relevant document chunks. `GenerateAnswerWorker` sends the original question plus the retrieved context to the LLM to produce a grounded answer. Conductor runs these three stages in sequence, records the embedding, retrieved chunks, and generated answer, and lets you tune retry behavior per task. In this example the task defs use `retryCount=0` so live provider failures are surfaced immediately while you validate the pipeline.

### What You Write: Workers

Three workers cover the full RAG pipeline: embedding the query, searching the vector store, and generating an answer, each independently testable and deployable.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedQueryWorker** | `brag_embed_query` | Converts the user's question into a vector embedding using OpenAI (`OPENAI_EMBED_MODEL`, default `text-embedding-3-small`), returning the embedding array, model name, and dimensions | **Real** when `CONDUCTOR_OPENAI_API_KEY` is set; simulated (fixed 8-dim vector) otherwise |
| **SearchVectorsWorker** | `brag_search_vectors` | Queries a vector database with the embedding to retrieve the top-k most relevant document chunks, each with id, text, and similarity score (0.85-0.94) | **Always simulated**.; no real vector DB. For real vector search, see the `rag-pinecone`, `rag-chromadb`, and `rag-pgvector` examples |
| **GenerateAnswerWorker** | `brag_generate_answer` | Sends the original question plus retrieved context to OpenAI (`OPENAI_CHAT_MODEL`, default `gpt-4o-mini`), producing a grounded answer; returns the answer text and token count | **Real** when `CONDUCTOR_OPENAI_API_KEY` is set; simulated (fixed answer) otherwise |

**Important:** Vector search is always simulated in this example, even in live mode. `CONDUCTOR_OPENAI_API_KEY` only turns on real embedding and generation calls. For real vector search, see the `rag-pinecone`, `rag-chromadb`, and `rag-pgvector` examples. Without the key, all three workers produce deterministic simulated output so the workflow runs end-to-end without any external dependencies.

### The Workflow

```
brag_embed_query
    │
    ▼
brag_search_vectors
    │
    ▼
brag_generate_answer

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

Starts Conductor on port 8080 and runs the example automatically (simulated mode).

With real OpenAI API calls:

```bash
CONDUCTOR_OPENAI_API_KEY=sk-... docker compose up --build

```

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

# Build and run (simulated mode)
mvn package -DskipTests
java -jar target/basic-rag-1.0.0.jar

# Or with real OpenAI API calls for embedding + generation:
export CONDUCTOR_OPENAI_API_KEY=sk-...
java -jar target/basic-rag-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

`run.sh` auto-loads the nearest `.env` file it finds while walking up parent directories, so a repo-root `.env` works without manual exports.

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `CONDUCTOR_OPENAI_API_KEY` | _(unset)_ | OpenAI API key. When set, `EmbedQueryWorker` and `GenerateAnswerWorker` make real API calls. When unset, both workers run in simulated mode |
| `OPENAI_EMBED_MODEL` | `text-embedding-3-small` | OpenAI embedding model used by `EmbedQueryWorker` in live mode. |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | OpenAI chat model used by `GenerateAnswerWorker` in live mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/basic-rag-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow basic_rag_workflow \
  --version 1 \
  --input '{"question": "What is Conductor and how does RAG work?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w basic_rag_workflow -s COMPLETED -c 5

```

## The RAG Pipeline

This example implements the foundational Retrieval-Augmented Generation pattern in three stages:

1. **Embed** (`brag_embed_query`): The user's question is converted into a dense vector representation using an embedding model. This vector captures the semantic meaning of the question, enabling similarity-based search rather than keyword matching.

2. **Search** (`brag_search_vectors`): The query embedding is compared against pre-indexed document embeddings in a vector database. The top-k most similar document chunks are returned, ranked by cosine similarity score. Each chunk includes the source text and a relevance score.

3. **Generate** (`brag_generate_answer`): The original question and the retrieved document chunks are sent to an LLM. The model generates an answer grounded in the provided context, reducing hallucination by anchoring responses in actual source material.

Conductor orchestrates these three stages as independent workers. If the embedding API times out, Conductor retries just that step. If the vector search returns but the LLM call fails, the retrieved documents are preserved and only generation is retried. Every execution records the embedding, retrieved chunks, and generated answer, so you can diagnose whether a bad answer came from poor retrieval or poor generation.

## How to Extend

Embedding and generation already use real OpenAI APIs when `CONDUCTOR_OPENAI_API_KEY` is set. The main extension point is the vector search worker, which is always simulated in this example.

- **EmbedQueryWorker** (`brag_embed_query`): already calls OpenAI in live mode and respects `OPENAI_EMBED_MODEL`. To swap providers, replace the API call with Cohere `embed()` or a local sentence-transformers model
- **SearchVectorsWorker** (`brag_search_vectors`): always simulated. Replace with a real vector database: Pinecone `query()`, Weaviate GraphQL, Qdrant `search`, Milvus, or pgvector in PostgreSQL. See the `rag-pinecone`, `rag-chromadb`, and `rag-pgvector` examples
- **GenerateAnswerWorker** (`brag_generate_answer`): already calls OpenAI in live mode and respects `OPENAI_CHAT_MODEL`. To swap providers, replace with Claude Messages API or a local Ollama model
- **Add a reranker**: insert a reranking step between search and generation (Cohere Rerank, cross-encoder models) to improve retrieval precision before feeding context to the LLM

Each worker preserves its input/output contract, so swapping embedding providers or vector stores requires zero workflow changes.

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
basic-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/basicrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BasicRagExample.java         # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmbedQueryWorker.java    # Query -> 8-dim vector embedding
│       ├── GenerateAnswerWorker.java # Context + question -> grounded answer
│       └── SearchVectorsWorker.java # Embedding -> top-k document chunks
└── src/test/java/basicrag/workers/
    ├── EmbedQueryWorkerTest.java    # 5 tests. Embedding shape, metadata, edge cases
    ├── GenerateAnswerWorkerTest.java # 4 tests. Context size, tokens, null handling
    └── SearchVectorsWorkerTest.java # 6 tests. Document count, scores, topK default

```
