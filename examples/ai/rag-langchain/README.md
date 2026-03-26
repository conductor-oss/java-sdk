# LangChain-Style RAG Pipeline: Load, Split, Embed, Retrieve, Generate

The full LangChain RAG pipeline as a Conductor workflow: load documents from a URL, split text on sentence boundaries, embed chunks via OpenAI, retrieve the top K by decreasing similarity score (0.95, 0.87, 0.79, ...), and generate an answer.

## Workflow

```
source, question, topK
       │
       ▼
┌───────────────────┐
│ lc_load_documents │  Fetch documents from URL
└────────┬──────────┘
         ▼
┌───────────────────┐
│ lc_split_text     │  Split on ". " into chunks
└────────┬──────────┘
         ▼
┌───────────────────┐
│ lc_embed_chunks   │  OpenAI text-embedding-3-small
└────────┬──────────┘
         ▼
┌───────────────────┐
│ lc_retrieve       │  Top K by similarity score
└────────┬──────────┘
         ▼
┌───────────────────┐
│ lc_generate       │  Generate from retrieved context
└───────────────────┘
```

## Workers

**LoadDocumentsWorker** (`lc_load_documents`) -- Returns documents with `pageContent` and `metadata` (source URL). Sample content about LangChain as a framework for LLM-powered applications.

**SplitTextWorker** (`lc_split_text`) -- Splits `content` on `". "` into sentence-level chunks, each with `metadata: {chunkIndex: N}`.

**EmbedChunksWorker** (`lc_embed_chunks`) -- When API key is set, calls `https://api.openai.com/v1/embeddings`. Returns embeddings mapped to chunk IDs.

**RetrieveWorker** (`lc_retrieve`) -- Assigns decreasing similarity scores: `0.95 - (i * 0.08)`, rounded to 2 decimal places via `Math.round(score * 100.0) / 100.0`. Returns the top K results.

**GenerateWorker** (`lc_generate`) -- Calls `gpt-4o-mini` with system prompt for context-based answering.

## Tests

25 tests cover document loading, sentence splitting, embedding, scored retrieval, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
