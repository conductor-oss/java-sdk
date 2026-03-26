# RAG with ChromaDB: Embed, Query Collections, Generate

Your documents live in ChromaDB collections and you need a RAG pipeline that embeds the query, runs a similarity search against a ChromaDB collection, and generates an answer from the retrieved context.

## Workflow

```
question, collection
       │
       ▼
┌──────────────────┐
│ chroma_embed     │  Embed query via OpenAI
└────────┬─────────┘
         ▼
┌──────────────────┐
│ chroma_query     │  Query ChromaDB collection
└────────┬─────────┘
         ▼
┌──────────────────┐
│ chroma_generate  │  Generate answer from results
└──────────────────┘
```

## Workers

**ChromaEmbedWorker** (`chroma_embed`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `https://api.openai.com/v1/embeddings` with `text-embedding-3-small`. Otherwise returns a `FIXED_EMBEDDING` vector.

**ChromaQueryWorker** (`chroma_query`) -- Simulates a ChromaDB query (in production: `ChromaClient("http://localhost:8000")`). Returns document IDs `["id-101", "id-204", "id-089"]` with corresponding documents and distances.

**ChromaGenerateWorker** (`chroma_generate`) -- Calls `gpt-4o-mini` with system prompt `"You are a helpful assistant. Use the provided context to answer questions accurately."` when API key is set. Otherwise returns a deterministic answer.

## Tests

16 tests cover embedding, ChromaDB querying, and answer generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
