# RAG with Weaviate in Java Using Conductor : GraphQL-Powered Vector Search and Generation

## RAG with Weaviate's Rich Query Interface

Weaviate provides vector search through a GraphQL API that supports nearVector, nearText (with built-in vectorizers), BM25, and hybrid queries. all in a single API. It also supports multi-tenancy, batch operations, and cross-references between objects. The RAG pipeline embeds the query, searches a Weaviate class for relevant objects, and generates from the results.

## The Solution

**You write the embedding and Weaviate GraphQL query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. query embedding, Weaviate GraphQL search, and answer generation. Conductor sequences them, retries the Weaviate query if the server is temporarily unavailable, and tracks every search with the query, retrieved objects, and generated answer.

### What You Write: Workers

Three workers integrate Weaviate into the RAG pipeline. embedding the query, searching via Weaviate's GraphQL nearVector or nearText operators, and generating an answer from the retrieved objects.

| Worker | Task | What It Does |
|---|---|---|
| **WeavEmbedWorker** | `weav_embed` | Converts a text question into a fixed embedding vector (demo). |
| **WeavGenerateWorker** | `weav_generate` | Generates an answer from the user question and retrieved Weaviate objects (demo). |
| **WeavSearchWorker** | `weav_search` | Performs a vector-similarity search against Weaviate (demo). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
weav_embed
 │
 ▼
weav_search
 │
 ▼
weav_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
