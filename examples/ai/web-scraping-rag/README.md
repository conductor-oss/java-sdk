# Web Scraping RAG in Java Using Conductor : Scrape Pages, Index Content, Answer Questions

## RAG Over Live Web Content

Sometimes the knowledge you need isn't in a pre-indexed corpus. it's on web pages that change frequently. Product documentation, competitor websites, news articles, and regulatory filings need to be scraped, indexed, and queried on demand. This pipeline scrapes the content, processes it into searchable vectors, and immediately queries it, all in a single workflow execution.

Each step can fail independently: a URL might return a 403, the HTML might have unexpected structure, the embedding API might rate-limit, or the vector store might be temporarily unavailable. Without orchestration, a single scraping error means restarting the entire pipeline.

## The Solution

**You write the scraping, chunking, and indexing logic. Conductor handles the end-to-end ingestion-to-query pipeline, retries, and observability.**

Each stage is an independent worker. web scraping (fetching and parsing HTML from multiple URLs), content chunking, embedding and vector storage, query embedding, and answer generation. Conductor sequences the full pipeline, retries failed scrapes individually, and tracks every execution from URLs through final answer.

### What You Write: Workers

Five workers form the scrape-to-answer pipeline. fetching web pages, chunking the extracted content, embedding and storing vectors, querying the freshly indexed content, and generating an answer from the scraped knowledge.

| Worker | Task | What It Does |
|---|---|---|
| **ChunkWorker** | `wsrag_chunk` | Worker that chunks scraped page content into sentence pairs. Splits each page's text by ". " and groups sentences in ... |
| **EmbedStoreWorker** | `wsrag_embed_store` | Worker that embeds chunks and stores them in a vector database. Returns the stored chunk IDs and count. |
| **GenerateWorker** | `wsrag_generate` | Worker that generates a final answer from the question and retrieved context. |
| **QueryWorker** | `wsrag_query` | Worker that queries the vector store with a question. Returns fixed relevant context chunks with similarity scores. |
| **ScrapeWorker** | `wsrag_scrape` | Worker that scrapes web pages and extracts content. Returns fixed demo pages with title, text, and word count. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
wsrag_scrape
 │
 ▼
wsrag_chunk
 │
 ▼
wsrag_embed_store
 │
 ▼
wsrag_query
 │
 ▼
wsrag_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
