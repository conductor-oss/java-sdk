# RAG with Qdrant: Vector Search with Payload Filtering and Score Threshold

Qdrant supports payload-based filtering and score thresholds alongside vector similarity. This pipeline embeds the query, searches Qdrant with configurable `scoreThreshold` (default 0.7) and payload filters, and generates an answer.

## Workflow

```
question, collection
       │
       ▼
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ qdrant_embed     │ --> │ qdrant_search    │ --> │ qdrant_generate  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
```

## Workers

**QdrantEmbedWorker** (`qdrant_embed`) -- Calls OpenAI when API key is set. Returns `FIXED_EMBEDDING` otherwise.

**QdrantSearchWorker** (`qdrant_search`) -- Simulates a Qdrant search with vector, limit, `score_threshold` (default 0.7), filter, and `with_payload` parameters.

**QdrantGenerateWorker** (`qdrant_generate`) -- Generates answer from search results.

## Tests

17 tests cover embedding, filtered Qdrant search, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
