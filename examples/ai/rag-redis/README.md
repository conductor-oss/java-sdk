# RAG with Redis Vector Search in Java Using Conductor : FT.SEARCH for Similarity Queries

## Ultra-Low Latency RAG with Redis

If you already run Redis for caching or session management, RediSearch adds vector similarity search without a separate database. Redis vectors are stored in-memory, making search latency sub-millisecond. ideal for real-time applications where RAG latency matters (chatbots, autocomplete, interactive assistants). The pipeline embeds the question, searches the Redis index, and generates from the results.

## The Solution

**You write the embedding and Redis FT.SEARCH query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, Redis FT.SEARCH vector query (specifying the index name), and answer generation. Conductor sequences them, retries the Redis query if the connection is temporarily lost, and tracks every search.

### What You Write: Workers

Three workers integrate Redis into the RAG pipeline. embedding the query, performing sub-millisecond vector search via Redis FT.SEARCH, and generating an answer from the matched documents.

| Worker | Task | What It Does |
|---|---|---|
| **RedisEmbedWorker** | `redis_embed` | Worker that converts a question into a fixed embedding vector. Uses deterministic values instead of a real embedding ... |
| **RedisFtSearchWorker** | `redis_ft_search` | Worker that simulates a Redis FT.SEARCH vector similarity query. Real Redis commands this simulates: Create index: FT... |
| **RedisGenerateWorker** | `redis_generate` | Worker that generates an answer from the question and Redis search results. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
redis_embed
 │
 ▼
redis_ft_search
 │
 ▼
redis_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
