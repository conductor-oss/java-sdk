# RAG with Milvus in Java Using Conductor : Embed, Search Vectors, Generate

## RAG at Scale with Milvus

Milvus is designed for large-scale vector search. it handles billions of vectors, supports multiple index types (IVF, HNSW, DiskANN), and provides attribute filtering alongside vector similarity. A RAG pipeline against Milvus embeds the question, searches the collection using Milvus's optimized ANN search, and generates from the results.

Each step can fail independently: the embedding model might time out, the Milvus cluster might be compacting, or the LLM might be rate-limited.

## The Solution

**You write the embedding and Milvus vector search logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, Milvus collection search, and answer generation. Conductor sequences them, retries the Milvus query during cluster compaction, and tracks every search with the question, collection, retrieved vectors, and generated answer.

### What You Write: Workers

Three workers integrate Milvus into the RAG pipeline. embedding the query, performing approximate nearest neighbor search at scale against a Milvus collection, and generating an answer from the retrieved vectors.

| Worker | Task | What It Does |
|---|---|---|
| **MilvusEmbedWorker** | `milvus_embed` | Worker that generates a fixed embedding vector for a question. In production this would call an embedding API (e.g. O... |
| **MilvusGenerateWorker** | `milvus_generate` | Worker that generates an answer from a question and retrieved Milvus search results. Builds a deterministic answer su... |
| **MilvusSearchWorker** | `milvus_search` | Worker that searches a Milvus collection with an embedding vector. Returns fixed results for deterministic behavior. ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
milvus_embed
 │
 ▼
milvus_search
 │
 ▼
milvus_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
