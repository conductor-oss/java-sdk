# RAG with pgvector: Vector Search in PostgreSQL

No separate vector database needed -- pgvector adds vector similarity search directly to PostgreSQL. This pipeline embeds the query, runs a pgvector similarity query against the specified table, and generates an answer.

## Workflow

```
question, table
       │
       ▼
┌──────────────┐
│ pgvec_embed  │  Embed query
└──────┬───────┘
       ▼
┌──────────────┐
│ pgvec_query  │  pgvector similarity search
└──────┬───────┘
       ▼
┌──────────────┐
│ pgvec_generate│  Generate answer
└──────────────┘
```

## Workers

**PgvecEmbedWorker** (`pgvec_embed`) -- Uses `Arrays.asList()` for the fixed embedding. Calls OpenAI when API key is set.

**PgvecQueryWorker** (`pgvec_query`) -- Simulates a pgvector query (`SELECT ... ORDER BY embedding <=> query_vec LIMIT K`). Returns 3 rows as `HashMap` entries with content and cosine distance scores.

**PgvecGenerateWorker** (`pgvec_generate`) -- Calls `gpt-4o-mini` with the retrieved rows as context.

## Tests

17 tests cover embedding, pgvector query simulation, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
