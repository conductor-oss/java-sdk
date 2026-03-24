# Cross-Encoder Reranking Between Retrieval and Generation

Bi-encoder retrieval is fast but imprecise. Cross-encoder reranking is slow but accurate. This pipeline retrieves a broad set of 6 candidates with bi-encoder scores, then reranks the top N with a cross-encoder model that produces different (often re-ordered) scores, and generates from only the top reranked results.

## Workflow

```
question, retrieveK, rerankTopN
       │
       ▼
┌──────────────────┐
│ rerank_embed     │  Embed query
└────────┬─────────┘
         ▼
┌──────────────────┐
│ rerank_retrieve  │  Retrieve 6 candidates (bi-encoder)
└────────┬─────────┘
         ▼
┌──────────────────────┐
│ rerank_crossencoder  │  Rerank with cross-encoder
└────────┬─────────────┘
         ▼
┌──────────────────┐
│ rerank_generate  │  Generate from top reranked docs
└──────────────────┘
```

## Workers

**EmbedWorker** (`rerank_embed`) -- Returns a fixed 8-dimensional embedding `[0.1, -0.3, 0.5, 0.2, -0.8, 0.4, -0.1, 0.7]`. When API key is set, calls OpenAI Embeddings.

**RetrieveWorker** (`rerank_retrieve`) -- Returns 6 candidates with bi-encoder similarity scores.

**CrossEncoderWorker** (`rerank_crossencoder`) -- Applies fixed cross-encoder scores by candidate index position. Cross-encoder scores differ from bi-encoder scores, often reordering the results.

**GenerateWorker** (`rerank_generate`) -- In fallback mode, returns: `"Re-ranking improves RAG precision: cross-encoder models score..."`. When API key is set, calls the LLM.

## Tests

23 tests cover embedding, retrieval, cross-encoder reranking, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
