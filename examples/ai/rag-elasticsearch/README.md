# RAG with Elasticsearch in Java Using Conductor : Dense Vector kNN Search and Generation

## RAG on Your Existing Elasticsearch Cluster

If you already run Elasticsearch for full-text search, you don't need a separate vector database for RAG. Elasticsearch 8.x supports dense_vector fields and kNN search natively, so you can add vector search to existing indices alongside your traditional keyword queries. The RAG pipeline embeds the question, runs a kNN search against the index, and generates from the results.

Each step can fail independently: the embedding API might time out, the Elasticsearch cluster might be rebalancing, or the LLM might be rate-limited.

## The Solution

**You write the embedding and Elasticsearch kNN query logic. Conductor handles the search pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, Elasticsearch kNN search, and answer generation. Conductor sequences them, retries the Elasticsearch query during cluster rebalancing, and tracks every search with the question, kNN results, and generated answer.

### What You Write: Workers

Three workers integrate Elasticsearch into the RAG pipeline. embedding the query, performing kNN dense vector search against an Elasticsearch index, and generating an answer from the matched documents.

| Worker | Task | What It Does |
|---|---|---|
| **EsEmbedWorker** | `es_embed` | Worker that encodes a question into a fixed embedding vector for Elasticsearch knn search. |
| **EsGenerateWorker** | `es_generate` | Worker that generates an answer from a question and Elasticsearch search hits. |
| **EsKnnSearchWorker** | `es_knn_search` | Worker that simulates an Elasticsearch knn vector search. In production, this would issue a POST to /{index}/_search ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
es_embed
 │
 ▼
es_knn_search
 │
 ▼
es_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
