# RAG with Pinecone in Java Using Conductor : Embed, Query Vectors with Namespace and Metadata Filtering, Generate

## RAG with Pinecone's Managed Vector Infrastructure

Pinecone provides serverless or pod-based vector search with zero infrastructure management. Its RAG pipeline embeds the question, queries the index (with optional namespace for tenant isolation and metadata filters for access control), and generates from the top-K results. The workflow accepts namespace, topK, and filter parameters for flexible, multi-tenant RAG.

Each step can fail independently: the embedding API might time out, Pinecone might be scaling, or the LLM might be rate-limited.

## The Solution

**You write the embedding and Pinecone query logic with namespace and metadata filters. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, Pinecone vector query (with namespace and filter support), and answer generation. Conductor sequences them, retries the Pinecone query during scaling events, and tracks every search with the question, namespace, filter, retrieved vectors, and generated answer.

### What You Write: Workers

Three workers integrate Pinecone into the RAG pipeline. embedding the query, querying a Pinecone index with namespace isolation and metadata filtering, and generating an answer from the top-k results.

| Worker | Task | What It Does |
|---|---|---|
| **PineEmbedWorker** | `pine_embed` | Worker that generates a fixed embedding vector for a question. In production this would call an embedding API (e.g. O... |
| **PineGenerateWorker** | `pine_generate` | Worker that generates an answer from a question and retrieved Pinecone matches. Builds a deterministic answer by comb... |
| **PineQueryWorker** | `pine_query` | Worker that queries a Pinecone index with an embedding vector. Returns fixed matches for deterministic behavior. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
pine_embed
 │
 ▼
pine_query
 │
 ▼
pine_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
