# RAG Reranking in Java Using Conductor : Cross-Encoder Reranking Between Retrieval and Generation

## Why Retrieve More, Then Rerank

Bi-encoder embeddings are fast (compare pre-computed vectors) but approximate. the top-5 results from a vector search aren't always the 5 most relevant documents. Cross-encoders are more accurate (they see question and document together) but expensive (they can't pre-compute). The solution: retrieve broadly (top 20 by vector similarity), then rerank precisely (cross-encoder scores each pair), and keep only the top 5 for generation.

This two-stage approach gives you cross-encoder accuracy at vector search speed.

## The Solution

**You write the broad retrieval and cross-encoder reranking logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each stage is an independent worker. question embedding, broad retrieval (high K), cross-encoder reranking (scoring and filtering to top N), and generation from the reranked results. Conductor sequences them, retries the cross-encoder if the model service is overloaded, and tracks the full retrieval-to-generation path.

### What You Write: Workers

Four workers implement the reranking pattern. embedding the query, retrieving a broad initial set of candidates, reranking with a cross-encoder model for precision, and generating an answer from the top reranked results.

| Worker | Task | What It Does |
|---|---|---|
| **CrossEncoderWorker** | `rerank_crossencoder` | Worker that re-ranks candidates using a cross-encoder model. Simulates cross-encoder scoring where order changes sign... |
| **EmbedWorker** | `rerank_embed` | Worker that embeds a question into a vector representation using OpenAI text-embedding-3-small. |
| **GenerateWorker** | `rerank_generate` | Worker that generates an answer using re-ranked context documents. |
| **RetrieveWorker** | `rerank_retrieve` | Worker that retrieves a broad set of candidates from a vector store. Returns 6 candidates with bi-encoder similarity ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
rerank_embed
 │
 ▼
rerank_retrieve
 │
 ▼
rerank_crossencoder
 │
 ▼
rerank_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
