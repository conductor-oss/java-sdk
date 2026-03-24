# RAG with Milvus: Embed, Vector Search, Generate

Milvus handles billion-scale vector workloads. This three-step pipeline embeds the query, searches a Milvus collection, and generates an answer from the retrieved context.

## Workflow

```
question, collection
       │
       ▼
┌──────────────────┐
│ milvus_embed     │  Embed query
└────────┬─────────┘
         ▼
┌──────────────────┐
│ milvus_search    │  Search Milvus collection
└────────┬─────────┘
         ▼
┌──────────────────┐
│ milvus_generate  │  Generate answer
└──────────────────┘
```

## Workers

**MilvusEmbedWorker** (`milvus_embed`) -- When API key is set, calls OpenAI Embeddings with `text-embedding-3-small`. Otherwise returns `FIXED_EMBEDDING`.

**MilvusSearchWorker** (`milvus_search`) -- Simulates a Milvus vector similarity search against the specified collection. Returns documents with scores.

**MilvusGenerateWorker** (`milvus_generate`) -- Calls `gpt-4o-mini` with system prompt for context-based answering.

## Tests

17 tests cover embedding, Milvus search, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
