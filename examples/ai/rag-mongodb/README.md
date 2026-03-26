# RAG with MongoDB Atlas Vector Search

Your data already lives in MongoDB. Instead of syncing to a separate vector store, use Atlas Vector Search directly. This pipeline embeds the query, runs a `$vectorSearch` aggregation (with `$project` for title, content, and `$meta: "vectorSearchScore"`), and generates an answer.

## Workflow

```
question, database, collection
       │
       ▼
┌──────────────────┐
│ mongo_embed      │  Embed query
└────────┬─────────┘
         ▼
┌──────────────────────────┐
│ mongo_vector_search      │  Atlas $vectorSearch pipeline
└────────┬─────────────────┘
         ▼
┌──────────────────┐
│ mongo_generate   │  Generate answer
└──────────────────┘
```

## Workers

**MongoEmbedWorker** (`mongo_embed`) -- Uses `Arrays.asList()` for the fixed embedding. When API key is set, calls OpenAI Embeddings.

**MongoVectorSearchWorker** (`mongo_vector_search`) -- Simulates the Atlas `$vectorSearch` aggregation with `$project: {title: 1, content: 1, score: {$meta: "vectorSearchScore"}}`. Returns documents with scores 0.95 and 0.91.

**MongoGenerateWorker** (`mongo_generate`) -- Calls `gpt-4o-mini` with the retrieved documents as context.

## Tests

14 tests cover embedding, MongoDB vector search, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
