# RAG with Redis Vector Search: FT.SEARCH for Similarity Queries

Redis delivers sub-millisecond vector search via `FT.SEARCH` with KNN queries. This pipeline embeds the query, runs `FT.SEARCH idx:docs "*=>[KNN 3 @embedding $query_vec AS vector_score]" SORTBY vector_score RETURN 3 content source vector_score`, and generates an answer.

## Workflow

```
question, indexName
       │
       ▼
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ redis_embed  │ --> │ redis_ft_search  │ --> │ redis_generate   │
└──────────────┘     └──────────────────┘     └──────────────────┘
```

## Workers

**RedisEmbedWorker** (`redis_embed`) -- Uses `Arrays.asList()` for fixed embedding. Calls OpenAI when API key is set.

**RedisFtSearchWorker** (`redis_ft_search`) -- Simulates Redis `FT.SEARCH` with KNN vector similarity, sorted by `vector_score`, returning content, source, and score fields.

**RedisGenerateWorker** (`redis_generate`) -- Generates answer from search results.

## Tests

13 tests cover embedding, FT.SEARCH simulation, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
