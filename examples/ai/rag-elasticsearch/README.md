# RAG with Elasticsearch kNN Vector Search

You already have an Elasticsearch cluster. Instead of adding another vector database, this pipeline runs dense vector kNN search directly in Elasticsearch: embed the query, execute a kNN search returning documents with `_score` values (top hit at 0.97), and generate an answer.

## Workflow

```
question, index
       │
       ▼
┌──────────────┐
│ es_embed     │  Embed query via OpenAI
└──────┬───────┘
       ▼
┌──────────────┐
│ es_knn_search│  kNN search in Elasticsearch index
└──────┬───────┘
       ▼
┌──────────────┐
│ es_generate  │  Generate answer from hits
└──────────────┘
```

## Workers

**EsEmbedWorker** (`es_embed`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls OpenAI Embeddings with `text-embedding-3-small`. Otherwise returns a `FIXED_EMBEDDING` vector.

**EsKnnSearchWorker** (`es_knn_search`) -- Returns kNN search hits with `_score: 0.97` for the top result. In production, this would execute an Elasticsearch kNN query against the specified index.

**EsGenerateWorker** (`es_generate`) -- Calls `gpt-4o-mini` with system prompt for context-based answering. Returns the generated answer.

## Tests

12 tests cover embedding, kNN search results, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
