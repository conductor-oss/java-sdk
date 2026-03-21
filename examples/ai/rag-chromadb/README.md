# RAG with ChromaDB in Java Using Conductor : Embed, Query, Generate

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
