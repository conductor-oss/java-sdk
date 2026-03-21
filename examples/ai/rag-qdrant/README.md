# RAG with Qdrant in Java Using Conductor : Vector Search with Payload Filtering

## RAG with Payload-Filtered Vector Search

Qdrant distinguishes itself with flexible payload filtering. you can combine vector similarity with structured filters on metadata fields. A query can find the most similar vectors that are also in category "engineering" and were created after "2024-01-01." This is essential for access-controlled or scoped RAG where not all documents should be searchable by all users.

The pipeline embeds the question, searches Qdrant with both the vector and payload filters, and generates from the filtered results.

## The Solution

**You write the embedding and Qdrant payload-filtered search logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, Qdrant collection search (with payload filtering), and answer generation. Conductor sequences them, retries the Qdrant query if the server is temporarily unavailable, and tracks every search with the question, collection, filters, and results.

### What You Write: Workers

Three workers integrate Qdrant into the RAG pipeline. embedding the query, searching a Qdrant collection with payload filtering and configurable score thresholds, and generating an answer from the matched points.

| Worker | Task | What It Does |
|---|---|---|
| **QdrantEmbedWorker** | `qdrant_embed` | Worker that produces a fixed embedding vector for a given question. In production this would call an embedding API (e... |
| **QdrantGenerateWorker** | `qdrant_generate` | Worker that generates an answer from a question and retrieved Qdrant points. Builds a deterministic answer by combini... |
| **QdrantSearchWorker** | `qdrant_search` | Worker that searches a Qdrant collection with an embedding vector. Returns fixed points with payload data for determi... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
qdrant_embed
 │
 ▼
qdrant_search
 │
 ▼
qdrant_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
