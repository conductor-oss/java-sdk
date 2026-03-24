# RAG with Weaviate: GraphQL-Powered Vector Search

Weaviate's GraphQL interface enables rich queries with filters, limits, and hybrid search modes. This pipeline embeds the query, runs a Weaviate nearVector search returning objects with properties, and generates an answer.

## Workflow

```
question, className
       │
       ▼
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ weav_embed   │ --> │ weav_search      │ --> │ weav_generate    │
└──────────────┘     └──────────────────┘     └──────────────────┘
```

## Workers

**WeavEmbedWorker** (`weav_embed`) -- Returns a fixed 5-dimensional embedding `[0.0123, -0.0456, 0.0789, -0.0321, 0.0654]`. Calls OpenAI when API key is set.

**WeavSearchWorker** (`weav_search`) -- Simulates a Weaviate nearVector query. Returns 3 objects with properties and certainty scores.

**WeavGenerateWorker** (`weav_generate`) -- Extracts objects from response (defaulting to empty list). Generates answer from context.

## Tests

16 tests cover embedding, Weaviate search, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
