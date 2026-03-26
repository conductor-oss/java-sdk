# RAG with Pinecone: Namespace and Metadata-Filtered Vector Search

Pinecone's managed vector infrastructure supports namespace isolation and metadata filtering. This pipeline embeds the query, queries Pinecone with namespace and filter parameters, and generates an answer. Top match scores: 0.96, 0.91, 0.87.

## Workflow

```
question, namespace, topK, filter
       │
       ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ pine_embed   │ --> │ pine_query   │ --> │ pine_generate│
└──────────────┘     └──────────────┘     └──────────────┘
```

## Workers

**PineEmbedWorker** (`pine_embed`) -- Calls OpenAI Embeddings when API key is set. Otherwise returns `FIXED_EMBEDDING`.

**PineQueryWorker** (`pine_query`) -- Simulates Pinecone query with namespace and metadata filter support. Returns 3 matches with scores 0.96, 0.91, 0.87.

**PineGenerateWorker** (`pine_generate`) -- Generates answer from retrieved Pinecone results.

## Tests

19 tests cover embedding, namespace-filtered queries, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
