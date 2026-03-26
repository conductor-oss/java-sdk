# Web Scraping RAG: Scrape Pages, Chunk, Embed, Query, Generate

Your knowledge isn't in a database -- it's on live web pages. This pipeline scrapes URLs, splits the content into sentence-level chunks, embeds and stores them, queries for relevant chunks, and generates an answer.

## Workflow

```
urls, question
     │
     ▼
┌──────────────────┐
│ wsrag_scrape     │  Fetch pages from URLs
└────────┬─────────┘
         ▼
┌──────────────────┐
│ wsrag_chunk      │  Split on ". " into sentences
└────────┬─────────┘
         ▼
┌──────────────────────┐
│ wsrag_embed_store    │  Embed + store vectors
└────────┬─────────────┘
         ▼
┌──────────────────┐
│ wsrag_query      │  Retrieve relevant chunks
└────────┬─────────┘
         ▼
┌──────────────────┐
│ wsrag_generate   │  Generate answer
└──────────────────┘
```

## Workers

**ScrapeWorker** (`wsrag_scrape`) -- Returns scraped pages as `List.of(Map.of(...))` with URL, content, and metadata for each page.

**ChunkWorker** (`wsrag_chunk`) -- Splits text on `". "` into sentence-level chunks, each with metadata.

**EmbedStoreWorker** (`wsrag_embed_store`) -- When API key is set, calls `https://api.openai.com/v1/embeddings`. Stores the resulting vectors.

**QueryWorker** (`wsrag_query`) -- Returns relevant context chunks with similarity scores.

**GenerateWorker** (`wsrag_generate`) -- Generates from the retrieved context using system prompt.

## Tests

23 tests cover scraping, sentence chunking, embedding, querying, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
